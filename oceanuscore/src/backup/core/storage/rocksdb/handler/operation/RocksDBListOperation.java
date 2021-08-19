package core.storage.rocksdb.handler.operation;

import core.common.InternalTools;
import core.log.LoggerHelper;
import core.storage.adapters.assist.impl.queue.CommonListOperation;
import core.storage.rocksdb.data.structure.common.KeyExpireData;
import core.storage.rocksdb.data.structure.list.KeyMetaData;
import core.storage.rocksdb.data.structure.list.KeySeqMetaData;
import core.utils.ValidateUtils;
import core.utils.scheduled.ScheduleTask;
import org.rocksdb.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/26.
 * Description：
 */
public class RocksDBListOperation extends CommonListOperation {
    private RocksDB rocksDB;
    private final long TTL_DEFAULT = -1L;
    private final long SEQ_DEFAULT = -1L;
    public RocksDBListOperation(RocksDB rocksDB, InternalTools internalTools){
        this.rocksDB = rocksDB;
        internalTools.getTimer().schedule(new ScheduleTask("RocksDBListOperation_clear_" + this.rocksDB.getName()) {
            @Override
            public void execute() {
                clear();
            }
        }, "0 30 0 1/10 * ? *");
    }
    private final byte[] KEY_PREFIX = new byte[]{'R','C','A'};
    private final byte[] KEY_SEQ_PREFIX = new byte[]{'R','C','a'};
    private final byte[] EXPIRE = new byte[]{'Y','Y','Y'};

    @Override
    protected Long llenPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                return keyMataData.length;
            }
        }catch (RocksDBException t){
            throw new IOException(getRocksDBExceptionString("len key " + key, t));
        }
        return 0L;
    }

    @Override
    protected byte[] lpopPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                KeySeqMetaData keySeqMetaData = getKeySeqMetaData(key, keyMataData.firstSeq, keyMataData.version);
                if(keySeqMetaData == null){
                    throw new IOException("get key: " + key + " seq " + keyMataData.firstSeq + " failed, cant lpop");
                }
                rocksDB.delete(genKeySeqBytes(key, keyMataData.firstSeq, keyMataData.version));
                long length = keyMataData.length - 1;
                try {
                    KeySeqMetaData nextKeySeqMetaData = null;
                    if(keySeqMetaData.nextSeq != SEQ_DEFAULT){
                        nextKeySeqMetaData = getKeySeqMetaData(key, keySeqMetaData.nextSeq, keyMataData.version);
                        if(nextKeySeqMetaData == null){
                            throw new IOException(structure + " get key: " + key + " seq " + keySeqMetaData.nextSeq + " failed, cant lpop");
                        }
                        rocksDB.put(genKeySeqBytes(key, keySeqMetaData.nextSeq, keyMataData.version), genKeySeqValueBytes(nextKeySeqMetaData.value, keySeqMetaData.preSeq, nextKeySeqMetaData.nextSeq));
                    }
                    try {
                        if(length > 0){
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keySeqMetaData.nextSeq, keyMataData.lastSeq, length));
                        }else {
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, SEQ_DEFAULT, SEQ_DEFAULT, 0L));
                        }
                    }catch (Throwable t){
                        if(nextKeySeqMetaData != null){
                            rocksDB.put(genKeySeqBytes(key, keySeqMetaData.nextSeq, keyMataData.version), genKeySeqValueBytes(nextKeySeqMetaData.value, nextKeySeqMetaData.preSeq, nextKeySeqMetaData.nextSeq));
                        }
                        throw t;
                    }
                }catch (Throwable t){
                    rocksDB.put(genKeySeqBytes(key, keyMataData.firstSeq, keyMataData.version), genKeySeqValueBytes(keySeqMetaData.value, keySeqMetaData.preSeq, keySeqMetaData.nextSeq));
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " lpop key " + key, t);
                }
                return keySeqMetaData.value;
            }
            return null;
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("lpop key " + key, e));
        }

    }

    @Override
    protected byte[] rpopPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                KeySeqMetaData keySeqMetaData = getKeySeqMetaData(key, keyMataData.lastSeq, keyMataData.version);
                if(keySeqMetaData == null){
                    throw new IOException(structure + " get key: " + key + " seq " + keyMataData.lastSeq + " failed, cant rpop");
                }
                rocksDB.delete(genKeySeqBytes(key, keyMataData.lastSeq, keyMataData.version));
                long length = keyMataData.length - 1;
                try {
                    KeySeqMetaData preKeySeqMetaData = null;
                    if(keySeqMetaData.preSeq != SEQ_DEFAULT){
                        preKeySeqMetaData = getKeySeqMetaData(key, keySeqMetaData.preSeq, keyMataData.version);
                        if(preKeySeqMetaData == null){
                            throw new IOException("get key: " + key + " seq " + keySeqMetaData.preSeq + " failed, cant rpop");
                        }
                        rocksDB.put(genKeySeqBytes(key, keySeqMetaData.preSeq, keyMataData.version), genKeySeqValueBytes(preKeySeqMetaData.value, preKeySeqMetaData.preSeq, keySeqMetaData.nextSeq));
                    }
                    try {
                        if(length > 0){
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstSeq, keySeqMetaData.preSeq, length));
                        }else {
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, SEQ_DEFAULT, SEQ_DEFAULT, 0L));
                        }
                    }catch (Throwable t){
                        if(preKeySeqMetaData != null){
                            rocksDB.put(genKeySeqBytes(key, keySeqMetaData.preSeq, keyMataData.version), genKeySeqValueBytes(preKeySeqMetaData.value, preKeySeqMetaData.preSeq, preKeySeqMetaData.nextSeq));
                        }
                        throw t;
                    }
                }catch (Throwable t){
                    rocksDB.put(genKeySeqBytes(key, keyMataData.lastSeq, keyMataData.version), genKeySeqValueBytes(keySeqMetaData.value, keySeqMetaData.preSeq, keySeqMetaData.nextSeq));
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " rpop key " + key, t);
                }
                return keySeqMetaData.value;
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("rpop key " + key, e));
        }
        return null;
    }

    @Override
    protected Long lpushPrivate(String key, byte[]... values) throws IOException {
        ValidateUtils.checkAllNotNull(key, values);
        long length = 0L;
        try {
            for (byte[] bytes : values){
                long seq;long version = 1L;long firstSeq = SEQ_DEFAULT;long lastSeq = SEQ_DEFAULT;long keyTtl = TTL_DEFAULT;long nextSeq;
                KeyMetaData keyMataData = null;
                try {
                    keyMataData = getMetaData(key);
                    if(keyMataData != null){
                        lastSeq = keyMataData.lastSeq;
                        version = keyMataData.version;
                        length = keyMataData.length;
                        firstSeq = keyMataData.firstSeq;
                        keyTtl = keyMataData.ttl;
                    }
                    length += 1;
                    nextSeq = firstSeq;
                    seq = (firstSeq > lastSeq ? (firstSeq + 1) : (lastSeq + 1));
                    firstSeq = seq;
                    if(lastSeq == SEQ_DEFAULT){
                        lastSeq = seq;
                    }
                    rocksDB.put(genKeySeqBytes(key, seq, version), genKeySeqValueBytes(bytes, SEQ_DEFAULT, nextSeq));
                    try {
                        KeySeqMetaData nextKeySeqMetaData = getKeySeqMetaData(key, nextSeq, version);
                        if(nextKeySeqMetaData != null){
                            rocksDB.put(genKeySeqBytes(key, nextSeq, version), genKeySeqValueBytes(nextKeySeqMetaData.value, seq, nextKeySeqMetaData.nextSeq));
                        }
                        try {
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(version, keyTtl, firstSeq, lastSeq, length));
                        }catch (Throwable t){
                            if(nextKeySeqMetaData != null){
                                rocksDB.put(genKeySeqBytes(key, nextSeq, version), genKeySeqValueBytes(nextKeySeqMetaData.value, nextKeySeqMetaData.preSeq, nextKeySeqMetaData.nextSeq));
                            }
                            throw t;
                        }
                    }catch (Throwable t){
                        rocksDB.delete(genKeySeqBytes(key, seq, version));
                        throw t;
                    }
                }catch (Throwable t){
                    if(keyMataData == null){
                        lDelPrivate(key);
                    }else {
                        KeyMetaData keyMataDataInit = keyMataData;
                        keyMataData = getMetaData(key);
                        if(keyMataData != null){
                            long newFirstSeq = keyMataData.firstSeq;
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataDataInit.version, keyMataDataInit.ttl, keyMataDataInit.firstSeq, keyMataDataInit.lastSeq, keyMataDataInit.length));
                            long theFirstSeq = keyMataDataInit.firstSeq;
                            KeySeqMetaData firstKeySeqMetaData = getKeySeqMetaData(key, theFirstSeq, version);
                            if(firstKeySeqMetaData != null){
                                while (newFirstSeq != theFirstSeq){
                                    KeySeqMetaData nextKeySeqMetaData = getKeySeqMetaData(key, newFirstSeq, keyMataDataInit.version);
                                    if(nextKeySeqMetaData != null){
                                        rocksDB.delete(genKeySeqBytes(key, newFirstSeq, keyMataDataInit.version));
                                        newFirstSeq = nextKeySeqMetaData.nextSeq;
                                    }
                                }
                                rocksDB.put(genKeySeqBytes(key, theFirstSeq, keyMataData.version), genKeySeqValueBytes(firstKeySeqMetaData.value, SEQ_DEFAULT, firstKeySeqMetaData.nextSeq));
                            }
                        }
                    }
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " lpush key " + key + " bytes " + Arrays.toString(bytes) + " failed", t);
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("lpush key " + key + " bytes " + Arrays.deepToString(values), e));
        }
        return length;
    }

    @Override
    protected Long lpushIfExistsPrivate(String key, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData == null){
                return 0L;
            }
            return lpushPrivate(key, bytes);
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("lpushIfExists key " + key + " bytes " + Arrays.toString(bytes), e));
        }
    }

    @Override
    protected Long rpushPrivate(String key, byte[]... values) throws IOException {
        ValidateUtils.checkAllNotNull(key, values);
        long length = 0L;
        try {
            for (byte[] bytes : values){
                long seq;long version = 1L;long preSeq;long firstSeq = SEQ_DEFAULT;long lastSeq = SEQ_DEFAULT;long keyTtl = TTL_DEFAULT;
                KeyMetaData keyMataData = null;
                try {
                    keyMataData = getMetaData(key);
                    if(keyMataData != null){
                        lastSeq = keyMataData.lastSeq;
                        version = keyMataData.version;
                        length = keyMataData.length;
                        firstSeq = keyMataData.firstSeq;
                        keyTtl = keyMataData.ttl;
                    }
                    length += 1;
                    preSeq = lastSeq;
                    seq = (firstSeq > lastSeq ? (firstSeq + 1) : (lastSeq + 1));
                    lastSeq = seq;
                    if(firstSeq == SEQ_DEFAULT){
                        firstSeq = seq;
                    }
                    rocksDB.put(genKeySeqBytes(key, seq, version), genKeySeqValueBytes(bytes, preSeq, SEQ_DEFAULT));
                    try {
                        KeySeqMetaData preKeySeqMetaData = getKeySeqMetaData(key, preSeq, version);
                        if(preKeySeqMetaData != null){
                            rocksDB.put(genKeySeqBytes(key, preSeq, version), genKeySeqValueBytes(preKeySeqMetaData.value, preKeySeqMetaData.preSeq, seq));
                        }
                        try {
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(version, keyTtl, firstSeq, lastSeq, length));
                        }catch (Throwable t){
                            if(preKeySeqMetaData != null){
                                rocksDB.put(genKeySeqBytes(key, preSeq, version), genKeySeqValueBytes(preKeySeqMetaData.value, preKeySeqMetaData.preSeq, preKeySeqMetaData.nextSeq));
                            }
                            throw t;
                        }
                    }catch (Throwable t){
                        rocksDB.delete(genKeySeqBytes(key, seq, version));
                        throw t;
                    }
                }catch (Throwable t){
                    if(keyMataData == null){
                        lDelPrivate(key);
                    }else {
                        KeyMetaData keyMataDataInit = keyMataData;
                        rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataDataInit.version, keyMataDataInit.ttl, keyMataDataInit.firstSeq, keyMataDataInit.lastSeq, keyMataDataInit.length));
                        long theLastSeq = keyMataDataInit.lastSeq;
                        KeySeqMetaData lastKeySeqMetaData = getKeySeqMetaData(key, theLastSeq, version);
                        if(lastKeySeqMetaData != null){
                            keyMataData = getMetaData(key);
                            if(keyMataData != null){
                                long newLastSeq = keyMataData.lastSeq;
                                while (newLastSeq != theLastSeq){
                                    KeySeqMetaData nextKeySeqMetaData = getKeySeqMetaData(key, newLastSeq, keyMataDataInit.version);
                                    if(nextKeySeqMetaData != null){
                                        rocksDB.delete(genKeySeqBytes(key, newLastSeq, keyMataDataInit.version));
                                        newLastSeq = nextKeySeqMetaData.preSeq;
                                    }
                                }
                                rocksDB.put(genKeySeqBytes(key, theLastSeq, keyMataData.version), genKeySeqValueBytes(lastKeySeqMetaData.value, lastKeySeqMetaData.preSeq, SEQ_DEFAULT));
                            }
                        }
                    }
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " rpush key " + key + " bytes " + Arrays.toString(bytes) + " failed", t);
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("rpush key " + key + " bytes " + Arrays.deepToString(values), e));
        }
        return length;
    }

    @Override
    protected Long rpushIfExistsPrivate(String key, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData == null){
                return 0L;
            }
            return rpushPrivate(key, bytes);
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("rpushIfExists key " + key + " bytes " + Arrays.toString(bytes), e));
        }
    }

    @Override
    protected List<byte[]> getPrivate(String key, long start, long stop) throws IOException {
        ValidateUtils.checkNotNull(key);
        start = (long)ValidateUtils.checkWithDefault(start, 0);
        stop = (long)ValidateUtils.checkWithDefault(stop, -1);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                long length = keyMataData.length;
                if(length <= 0){
                    return null;
                }
                if(stop < 0){
                    stop = length + stop;
                }
                if(start > stop){
                    return null;
                }
                if(start < 0){
                    start = 0;
                }
                if(start >= length){
                    return null;
                }

                List<byte[]> bytes = new ArrayList<>();
                long nextSeq = keyMataData.firstSeq;
                int index = 0;
                while (nextSeq != SEQ_DEFAULT){
                    byte[] valueBytes = rocksDB.get(genKeySeqBytes(key, nextSeq, keyMataData.version));
                    if(valueBytes != null){
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
                        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
                        try (byteArrayInputStream; dataInputStream){
                            nextSeq = dataInputStream.readLong();
                            if(index >= start && index <= stop){
                                int valueLength = dataInputStream.readInt();
                                byte[] factValueBytes = new byte[valueLength];
                                dataInputStream.readFully(factValueBytes);
                                bytes.add(factValueBytes);
                                if(index == stop){
                                    break;
                                }
                            }
                            index++;
                        }
                    }
                }
                return bytes;
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("get key " + key + " start " + start + " stop " + stop, e));
        }
        return null;
    }

    @Override
    protected void lExpirePrivate(String key, Long expire) throws IOException {
        ValidateUtils.checkAllNotNull(key, expire);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData == null){
                throw new IOException("Key " + key + " not exist when expire");
            }
            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, System.currentTimeMillis() + expire, keyMataData.firstSeq, keyMataData.lastSeq, keyMataData.length));
            try {
                deleteAsync(key, getMetaData(key));
            }catch (Throwable t){
                rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstSeq, keyMataData.lastSeq, keyMataData.length));
                if(t instanceof RocksDBException){
                    throw t;
                }
                throw new IOException(structure + " expire key " + key + " expire " + expire, t);
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("expire key " + key + " expire " + expire, e));
        }
    }

    @Override
    protected void lDelPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version + 1, TTL_DEFAULT, SEQ_DEFAULT, SEQ_DEFAULT, 0L));
                try {
                    deleteAsync(key, keyMataData);
                }catch (Throwable t){
                    rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstSeq, keyMataData.lastSeq, keyMataData.length));
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " del key " + key, t);
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("del key " + key, e));
        }
    }

    @Override
    protected byte[] generateLockKey(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_PREFIX);
            dataOutputStream.writeBytes(key);
            return byteArrayOutputStream.toByteArray();
        }
    }
    private void deleteAsync(String key, KeyMetaData keyMataData) throws IOException, RocksDBException {
        ValidateUtils.checkAllNotNull(key, keyMataData);
        KeyExpireData keyExpireData = getKeyExpireData(key);
        int length = 0;
        byte[] value;
        byte[] contactValue = null;
        byte[] valueByes;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(keyMataData.version);
            dataOutputStream.writeLong(keyMataData.ttl);
            dataOutputStream.writeLong(keyMataData.firstSeq);
            valueByes = byteArrayOutputStream.toByteArray();
        }
        if(keyExpireData != null){
            length = keyExpireData.length;
            value = keyExpireData.value;
            contactValue = valueByes;
        }else {
            value = valueByes;
        }
        rocksDB.put(genKeysExpireBytes(key), genKeysExpireValueBytes(length + 1, value, contactValue));
    }
    private KeyMetaData getMetaData(String key) throws IOException, RocksDBException{
        byte[] valueBytes = rocksDB.get(genMetaDataKeyBytes(key));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                long version = dataInputStream.readLong();
                long ttl = dataInputStream.readLong();
                if(ttl == TTL_DEFAULT || ttl > System.currentTimeMillis()){
                    long firstSeq = dataInputStream.readLong();
                    long lastSeq = dataInputStream.readLong();
                    long length = dataInputStream.readLong();
                    return new KeyMetaData(version, ttl, firstSeq, lastSeq, length);
                }else {
                    rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(version + 1, TTL_DEFAULT, SEQ_DEFAULT, SEQ_DEFAULT, 0L));
                    return new KeyMetaData(version + 1, TTL_DEFAULT, SEQ_DEFAULT, SEQ_DEFAULT, 0L);
                }
            }
        }
        return null;
    }
    private KeyMetaData getMetaDataAll(String key) throws IOException, RocksDBException{
        byte[] valueBytes = rocksDB.get(genMetaDataKeyBytes(key));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                long version = dataInputStream.readLong();
                long ttl = dataInputStream.readLong();
                long firstSeq = dataInputStream.readLong();
                long lastSeq = dataInputStream.readLong();
                long length = dataInputStream.readLong();
                return new KeyMetaData(version, ttl, firstSeq, lastSeq, length);
            }
        }
        return null;
    }
    private KeySeqMetaData getKeySeqMetaData(String key, Long seq, Long version) throws IOException, RocksDBException{
        byte[] valueBytes = rocksDB.get(genKeySeqBytes(key, seq, version));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                long nextSeq = dataInputStream.readLong();
                int length = dataInputStream.readInt();
                byte[] factValueBytes = new byte[length];
                dataInputStream.readFully(factValueBytes);
                long preSeq = dataInputStream.readLong();
                return new KeySeqMetaData(factValueBytes, preSeq, nextSeq);
            }
        }
        return null;
    }
    private String getRocksDBExceptionString(String head, RocksDBException e) {
        StringBuilder builder = new StringBuilder(structure).append(" ").
                append(head).append(" ");
        if(e != null){
            Status status = e.getStatus();
            if(status != null) {
                builder.append("Status ");
                builder.append(status.getCodeString()).append(" ");
                builder.append(status.getSubCode()).append("; ");
            }
            builder.append("Error ").append(e.getMessage());
        }
        return builder.toString();
    }
    private byte[] genMetaDataKeyBytes(String key) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_PREFIX);
            dataOutputStream.writeBytes(key);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genMetaDataKeyValueBytes(long version, long ttl, long firstSeq, long lastSeq, long length) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(version);
            dataOutputStream.writeLong(ttl);
            dataOutputStream.writeLong(firstSeq);
            dataOutputStream.writeLong(lastSeq);
            dataOutputStream.writeLong(length);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genKeySeqBytes(String key, long seq, long version) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_SEQ_PREFIX);
            dataOutputStream.writeBytes(key);
            dataOutputStream.writeLong(version);
            dataOutputStream.writeLong(seq);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genKeySeqValueBytes(byte[] value, long preSeq, long nextSeq) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(nextSeq);
            dataOutputStream.writeInt(value.length);
            dataOutputStream.write(value);
            dataOutputStream.writeLong(preSeq);
            return byteArrayOutputStream.toByteArray();
        }
    }


    private KeyExpireData getKeyExpireData(String key)throws IOException,RocksDBException{
        byte[] valueBytes = rocksDB.get(genKeysExpireBytes(key));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                int length = dataInputStream.readInt();
                if(length > 0){
                    byte[] bytes = dataInputStream.readAllBytes();
                    return new KeyExpireData(length, bytes);
                }
            }
        }
        return null;
    }

    private byte[] genKeysExpireBytes(String key) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(EXPIRE);
            dataOutputStream.writeBytes(key);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genKeysExpireValueBytes(int length, byte[] value, byte[] contactValue)throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeInt(length);
            dataOutputStream.write(value);
            if(contactValue != null){
                dataOutputStream.write(contactValue);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    private void clear(){
        RocksIterator rocksIterator = rocksDB.newIterator(new ReadOptions());
        for (rocksIterator.seek(EXPIRE); rocksIterator.isValid(); rocksIterator.next()){
            try {
                ByteArrayInputStream byteArrayInputStreamEx = new ByteArrayInputStream(rocksIterator.key());
                DataInputStream dataInputStreamEx = new DataInputStream(byteArrayInputStreamEx);
                byte[] bytes = new byte[3];
                dataInputStreamEx.readFully(bytes);
                if(Arrays.equals(EXPIRE, bytes)){
                    String key = dataInputStreamEx.readUTF();
                    rocksDB.delete(genKeysExpireBytes(key));
                    ReadWriteLock lock = getLock(key);
                    try {
                        lock.writeLock().lock();
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(rocksIterator.value());
                        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
                        try (byteArrayInputStream; dataInputStream){
                            int expireLength = dataInputStream.readInt();
                            if(expireLength > 0){
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                                try (byteArrayOutputStream; dataOutputStream){
                                    List<Map> cantDeleteList = new ArrayList<>();
                                    for (int i = 0; i < expireLength; i++) {
                                        try (byteArrayInputStream; dataInputStream) {
                                            long version = dataInputStream.readLong();
                                            long ttl = dataInputStream.readLong();
                                            long firstSeq = dataInputStream.readLong();
                                            KeyMetaData keyMataData = getMetaDataAll(key);
                                            boolean shouldDelete = true;
                                            boolean reAdd = false;
                                            if(keyMataData != null){
                                                if(version == keyMataData.version){
                                                    if(ttl != TTL_DEFAULT){
                                                        if(keyMataData.ttl != ttl){
                                                            shouldDelete = false;
                                                        }else if(ttl >= System.currentTimeMillis()){
                                                            shouldDelete = false;
                                                            reAdd = true;
                                                        }
                                                    }
                                                }
                                            }

                                            Map<String, Object> map = new HashMap<>();
                                            if(shouldDelete){
                                                LoggerHelper.getLogger().info(structure + " key: " + key + ",meta version: " + (keyMataData == null ? null : keyMataData.version) + ",will delete version: " + version + ", meta ttl: " + (keyMataData == null ? null : keyMataData.ttl) + ", will delete ttl: " + ttl);
                                                long seq = firstSeq;
                                                while (seq != SEQ_DEFAULT){
                                                    byte[] valueBytes = rocksDB.get(genKeySeqBytes(key, seq, version));
                                                    if(valueBytes != null){
                                                        rocksDB.delete(genKeySeqBytes(key, seq, version));
                                                        ByteArrayInputStream valueByteArrayInputStream = new ByteArrayInputStream(valueBytes);
                                                        DataInputStream valueDataInputStream = new DataInputStream(valueByteArrayInputStream);
                                                        try (valueByteArrayInputStream; valueDataInputStream){
                                                            seq = valueDataInputStream.readLong();
                                                        }
                                                    }
                                                }
                                            }
                                            if(reAdd){
                                                map.put("version", version);
                                                map.put("ttl", ttl);
                                                map.put("firstSeq", firstSeq);
                                                cantDeleteList.add(map);
                                            }
                                        }
                                    }
                                    if(!cantDeleteList.isEmpty()){
                                        dataOutputStream.writeInt(cantDeleteList.size());
                                        for (Map map : cantDeleteList){
                                            dataOutputStream.writeLong((long)map.get("version"));
                                            dataOutputStream.writeLong((long)map.get("ttl"));
                                            dataOutputStream.writeLong((long)map.get("firstSeq"));
                                        }
                                        rocksDB.put(genKeysExpireBytes(key), byteArrayOutputStream.toByteArray());
                                    }
                                }
                            }
                        }
                    }finally {
                        lock.writeLock().unlock();
                    }
                }
            }catch (Throwable t){
                t.printStackTrace();
            }
        }
        LoggerHelper.getLogger().info(structure + " finish clear, time: " + System.currentTimeMillis());
    }
}
