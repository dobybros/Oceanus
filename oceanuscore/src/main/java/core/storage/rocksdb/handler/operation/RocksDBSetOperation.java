package core.storage.rocksdb.handler.operation;

import chat.logs.LoggerEx;
import core.common.InternalTools;

import core.storage.adapters.assist.impl.queue.CommonSetOperation;
import core.storage.rocksdb.data.structure.common.KeyExpireData;
import core.storage.rocksdb.data.structure.set.KeyFieldData;
import core.storage.rocksdb.data.structure.set.KeyMetaData;
import core.utils.ValidateUtils;
import core.utils.scheduled.ScheduleTask;
import org.rocksdb.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/15.
 * Description：
 */
public class RocksDBSetOperation extends CommonSetOperation {
    private static final String TAG = RocksDBSetOperation.class.getSimpleName();
    private RocksDB rocksDB;
    public RocksDBSetOperation(RocksDB rocksDB, InternalTools internalTools){
        this.rocksDB = rocksDB;
        internalTools.getTimer().schedule(new ScheduleTask("RocksDBHashOperation_clear_" + this.rocksDB.getName()) {
            @Override
            public void execute() {
                clear();
            }
        }, "0 40 0 1/10 * ? *");
    }
    private final byte[] KEY_PREFIX = new byte[]{'R','D','A'};
    private final byte[] KEY_FIELD_PREFIX = new byte[]{'R','D','a'};
    private final byte[] EXPIRE = new byte[]{'y','y','y'};

    @Override
    protected Long addPrivate(String key, byte[]... bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        List<byte[]> addSuccessBytes = new ArrayList<>();
        long length = 0L;
        try {
            for (byte[] value : bytes){
                try {
                    KeyMetaData keyMataData = getMetaData(key);
                    KeyFieldData keyFieldData = null;
                    if(keyMataData == null){
                        keyMataData = new KeyMetaData();
                    }else {
                        keyFieldData = getKeyFieldData(key, value, keyMataData.version);
                    }
                    if(keyMataData.length == 0){
                        keyMataData.firstBytes = value;
                    }
                    byte[] preBytes = keyMataData.lastBytes;
                    if(keyFieldData == null){
                        keyMataData.lastBytes = value;
                        rocksDB.put(genKeyFieldBytes(key, value, keyMataData.version), genKeyFieldValueBytes(preBytes, KeyFieldData.BYTES_DEFAULT));
                        try {
                            KeyFieldData preKeyFieldData = getKeyFieldData(key, preBytes, keyMataData.version);
                            if(preKeyFieldData != null){
                                rocksDB.put(genKeyFieldBytes(key, preBytes, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.preBytes, value));
                            }
                            try {
                                length = keyMataData.length + 1;
                                rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstBytes, keyMataData.lastBytes, length));
                            }catch (Throwable t){
                                if(preKeyFieldData != null){
                                    rocksDB.put(genKeyFieldBytes(key, preBytes, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.preBytes, preKeyFieldData.nextBytes));
                                }
                                throw t;
                            }
                        }catch (Throwable t){
                            rocksDB.delete(genKeyFieldBytes(key, value, keyMataData.version));
                            if(t instanceof RocksDBException){
                                throw t;
                            }
                            throw new IOException(structure + " set key " + key + " value " + Arrays.toString(value) + " bytes " + Arrays.deepToString(bytes) + " failed", t);
                        }
                        addSuccessBytes.add(value);
                    }
                }catch (Throwable t){
                    if(!addSuccessBytes.isEmpty()){
                        sdelPrivate(key, addSuccessBytes.toArray(new byte[0][0]));
                    }
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " add key " + key + " bytes " + Arrays.deepToString(bytes) + " failed", t);            }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("add key " + key + " bytes " + Arrays.deepToString(bytes), e));
        }
        return length;
    }

    @Override
    protected Set<byte[]> sgetPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                Set<byte[]> bytes = new HashSet<>();
                byte[] nextBytes = keyMataData.firstBytes;
                while (!Arrays.equals(nextBytes, KeyFieldData.BYTES_DEFAULT)){
                    byte[] valueBytes = rocksDB.get(genKeyFieldBytes(key, nextBytes, keyMataData.version));
                    if(valueBytes != null){
                        bytes.add(nextBytes);
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
                        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
                        try (byteArrayInputStream; dataInputStream){
                            int nextBytesLength = dataInputStream.readInt();
                            nextBytes = new byte[nextBytesLength];
                            dataInputStream.readFully(nextBytes);
                        }
                    }else {
                        break;
                    }
                }
                if(!bytes.isEmpty()){
                    return bytes;
                }
            }
        }catch(RocksDBException e){
            throw new IOException(getRocksDBExceptionString("get key " + key, e));
        }
        return null;
    }

    @Override
    protected Long slenPrivate(String key) throws IOException {
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
    protected Boolean isMemberPrivate(String key, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                byte[] valueBytes = rocksDB.get(genKeyFieldBytes(key, bytes, keyMataData.version));
                if(valueBytes != null){
                    return true;
                }
            }
        }catch(RocksDBException e){
            throw new IOException(getRocksDBExceptionString("isMember key " + key + " bytes " + Arrays.toString(bytes), e));
        }
        return false;
    }

    @Override
    protected void sdelPrivate(String key, byte[]... bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        List<byte[]> existBytes = new ArrayList<>();
        for (byte[] value : bytes){
            try {
                sdelPrivate(key, value);
                existBytes.add(value);
            }catch (Throwable t){
                if(!existBytes.isEmpty()){
                    addPrivate(key, existBytes.toArray(new byte[0][0]));
                }
                throw new IOException(structure + " del key " + key + " bytes " + Arrays.deepToString(bytes) + " failed", t);
            }
        }
    }

    @Override
    protected void sdelPrivate(String key, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                KeyFieldData keyFieldData = getKeyFieldData(key, bytes, keyMataData.version);
                if(keyFieldData != null){
                    byte[] firstBytes = keyMataData.firstBytes;
                    byte[] lastBytes = keyMataData.lastBytes;
                    if(Arrays.equals(bytes, keyMataData.firstBytes)){
                        firstBytes = keyFieldData.nextBytes;
                    }else if(Arrays.equals(bytes, keyMataData.lastBytes)){
                        lastBytes = keyFieldData.preBytes;
                    }
                    long length = keyMataData.length - 1;
                    KeyFieldData preKeyFieldData = null;
                    KeyFieldData nextKeyFieldData = null;
                    if(length > 0){
                        preKeyFieldData = getKeyFieldData(key, keyFieldData.preBytes, keyMataData.version);
                        if(preKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.preBytes, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.preBytes, keyFieldData.nextBytes));
                        }
                        nextKeyFieldData = getKeyFieldData(key, keyFieldData.nextBytes, keyMataData.version);
                        if(nextKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.nextBytes, keyMataData.version), genKeyFieldValueBytes(keyFieldData.preBytes, nextKeyFieldData.nextBytes));
                        }
                    }else {
                        firstBytes = KeyFieldData.BYTES_DEFAULT;
                        lastBytes = KeyFieldData.BYTES_DEFAULT;
                    }
                    try {
                        rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, firstBytes, lastBytes, length));
                        try {
                            rocksDB.delete(genKeyFieldBytes(key, bytes, keyMataData.version));
                        }catch (Throwable t){
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstBytes, keyMataData.lastBytes, keyMataData.length));
                            throw t;
                        }
                    }catch (Throwable t){
                        if(preKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.preBytes, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.preBytes, preKeyFieldData.nextBytes));
                        }
                        if(nextKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.nextBytes, keyMataData.version), genKeyFieldValueBytes(nextKeyFieldData.preBytes, nextKeyFieldData.nextBytes));
                        }
                        if(t instanceof RocksDBException){
                            throw t;
                        }
                        throw new IOException(structure + "del key " + key + " bytes " + Arrays.toString(bytes) + " failed", t);
                    }
                }
            }
        }catch (RocksDBException t){
            throw new IOException(getRocksDBExceptionString("del key "+ key + " bytes "+ Arrays.toString(bytes), t));
        }
    }

    @Override
    protected void sexpirePrivate(String key, Long expire) throws IOException {
        ValidateUtils.checkAllNotNull(key, expire);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData == null){
                throw new IOException("Key " + key + " not exist when expire");
            }
            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, System.currentTimeMillis() + expire, keyMataData.firstBytes, keyMataData.lastBytes, keyMataData.length));
            try {
                deleteAsync(key, getMetaData(key));
            }catch (Throwable t){
                rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstBytes, keyMataData.lastBytes, keyMataData.length));
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
    protected void sdelPrivate(String key) throws IOException {
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version + 1, KeyMetaData.TTL_DEFAULT, KeyMetaData.BYTES_DEFAULT, KeyMetaData.BYTES_DEFAULT, 0L));
                try {
                    deleteAsync(key, keyMataData);
                }catch (Throwable t){
                    rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstBytes, keyMataData.lastBytes, keyMataData.length));
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
            dataOutputStream.writeInt(keyMataData.firstBytes.length);
            dataOutputStream.write(keyMataData.firstBytes);
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
                 if(ttl == KeyMetaData.TTL_DEFAULT || ttl > System.currentTimeMillis()){
                     int firstBytesLength = dataInputStream.readInt();
                     byte[] firstBytes = new byte[firstBytesLength];
                     dataInputStream.readFully(firstBytes);
                     int lastBytesLength = dataInputStream.readInt();
                     byte[] lastBytes = new byte[lastBytesLength];
                     dataInputStream.readFully(lastBytes);
                     long length = dataInputStream.readLong();
                     return new KeyMetaData(version, ttl, firstBytes, lastBytes, length);
                 }else {
                     rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(version + 1, KeyMetaData.TTL_DEFAULT, KeyMetaData.BYTES_DEFAULT, KeyMetaData.BYTES_DEFAULT, KeyMetaData.LENGTH_DEFAULT));
                     return new KeyMetaData(version + 1, KeyMetaData.TTL_DEFAULT, KeyMetaData.BYTES_DEFAULT, KeyMetaData.BYTES_DEFAULT, KeyMetaData.LENGTH_DEFAULT);
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
                int firstBytesLength = dataInputStream.readInt();
                byte[] firstBytes = new byte[firstBytesLength];
                dataInputStream.readFully(firstBytes);
                int lastBytesLength = dataInputStream.readInt();
                byte[] lastBytes = new byte[lastBytesLength];
                dataInputStream.readFully(lastBytes);
                long length = dataInputStream.readLong();
                return new KeyMetaData(version, ttl, firstBytes, lastBytes, length);
            }
        }
        return null;
    }
    private KeyFieldData getKeyFieldData(String key, byte[] bytes, Long version) throws IOException,RocksDBException{
        byte[] valueBytes = rocksDB.get(genKeyFieldBytes(key, bytes, version));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                int nextBytesLength = dataInputStream.readInt();
                byte[] nextBytes = new byte[nextBytesLength];
                dataInputStream.readFully(nextBytes);
                int preBytesLength = dataInputStream.readInt();
                byte[] preBytes = new byte[preBytesLength];
                dataInputStream.readFully(preBytes);
                return new KeyFieldData(preBytes, nextBytes);
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

    private byte[] genMetaDataKeyValueBytes(long version, long ttl, byte[] firstBytes, byte[] lastBytes, long length) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(version);
            dataOutputStream.writeLong(ttl);
            dataOutputStream.writeInt(firstBytes.length);
            dataOutputStream.write(firstBytes);
            dataOutputStream.writeInt(lastBytes.length);
            dataOutputStream.write(lastBytes);
            dataOutputStream.writeLong(length);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genKeyFieldBytes(String key, byte[] bytes, long version) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_FIELD_PREFIX);
            dataOutputStream.writeBytes(key);
            dataOutputStream.writeLong(version);
            dataOutputStream.write(bytes);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genKeyFieldValueBytes(byte[] preBytes, byte[] nextBytes) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeInt(nextBytes.length);
            dataOutputStream.write(nextBytes);
            dataOutputStream.writeInt(preBytes.length);
            dataOutputStream.write(preBytes);
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

    private byte[] genKeysExpireBytes(String key) throws IOException{
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
                                            int firstBytesLength = dataInputStream.readInt();
                                            byte[] firstBytes = new byte[firstBytesLength];
                                            dataInputStream.readFully(firstBytes);
                                            KeyMetaData keyMataData = getMetaDataAll(key);
                                            boolean shouldDelete = true;
                                            boolean reAdd = false;
                                            if(keyMataData != null){
                                                if(version == keyMataData.version){
                                                    if(ttl != KeyMetaData.TTL_DEFAULT){
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
                                                LoggerEx.info(TAG, structure + " key: " + key + ",meta version: " + (keyMataData == null ? null : keyMataData.version) + ",will delete version: " + version + ", meta ttl: " + (keyMataData == null ? null : keyMataData.ttl) + ", will delete ttl: " + ttl);
                                                byte[] theBytes = firstBytes;
                                                while (!Arrays.equals(theBytes, KeyFieldData.BYTES_DEFAULT)){
                                                    byte[] valueBytes = rocksDB.get(genKeyFieldBytes(key, theBytes, version));
                                                    if(valueBytes != null){
                                                        rocksDB.delete(genKeyFieldBytes(key, theBytes, version));
                                                        ByteArrayInputStream theByteArrayInputStream = new ByteArrayInputStream(valueBytes);
                                                        DataInputStream theDataInputStream = new DataInputStream(theByteArrayInputStream);
                                                        try (theByteArrayInputStream; theDataInputStream){
                                                            int nextBytesLength = theDataInputStream.readInt();
                                                            theBytes = new byte[nextBytesLength];
                                                            theDataInputStream.readFully(theBytes);
                                                        }
                                                    }else {
                                                        break;
                                                    }
                                                }

                                            }
                                            if(reAdd){
                                                map.put("version", version);
                                                map.put("ttl", ttl);
                                                map.put("firstBytes", firstBytes);
                                                cantDeleteList.add(map);
                                            }
                                        }
                                    }
                                    if(!cantDeleteList.isEmpty()){
                                        dataOutputStream.writeInt(cantDeleteList.size());
                                        for (Map map : cantDeleteList){
                                            dataOutputStream.writeLong((long)map.get("version"));
                                            dataOutputStream.writeLong((long)map.get("ttl"));
                                            byte[] firstBytes = (byte[]) map.get("firstBytes");
                                            dataOutputStream.writeInt(firstBytes.length);
                                            dataOutputStream.write(firstBytes);
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
        LoggerEx.info(TAG, structure + " finish clear , time: " + System.currentTimeMillis());
    }
}
