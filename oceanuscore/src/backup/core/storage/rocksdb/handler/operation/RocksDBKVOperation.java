package core.storage.rocksdb.handler.operation;

import core.common.InternalTools;
import core.storage.adapters.assist.impl.CommonKVOperation;
import core.storage.rocksdb.data.structure.kv.KeyData;
import core.utils.ByteUtils;
import core.utils.ValidateUtils;
import core.utils.scheduled.ScheduleTask;
import org.rocksdb.*;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/30.
 * Description：
 */
public class RocksDBKVOperation extends CommonKVOperation {
    private RocksDB rocksDB;
    private final long TTL_DEFAULT = -1L;
    public RocksDBKVOperation(RocksDB rocksDB, InternalTools internalTools){
        this.rocksDB = rocksDB;
        internalTools.getTimer().schedule(new ScheduleTask("RocksDBKVOperation_clear_" + this.rocksDB.getName()) {
            @Override
            public void execute() {
                clear();
            }
        }, "0 10 0 1/10 * ? *");
    }
    private final byte[] KEY_PREFIX = new byte[]{'R','A', 'A'};
    private final byte[] EXPIRE = new byte[]{'Z','Z', 'Z'};

    @Override
    protected void kvsetPrivate(String key, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        try {
            rocksDB.put(genKey(key), genValue(bytes, TTL_DEFAULT));
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString(" set key " + key + " bytes " + Arrays.toString(bytes), e));
        }
    }

    @Override
    protected void kvsetExPrivate(String key, byte[] bytes, Long expire) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes, expire);
        try {
            rocksDB.put(genKey(key), genValue(bytes, System.currentTimeMillis() + expire));
            try {
                expire(key);
            }catch (Throwable t){
                 rocksDB.delete(genKey(key));
                if(t instanceof RocksDBException){
                    throw t;
                }
                throw new IOException(structure + " setEx key " + key + " bytes " + Arrays.toString(bytes) + " expire " + expire, t);
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString(" setEx key " + key + " bytes " + Arrays.toString(bytes) + " expire " + expire, e));
        }
//        clear();
    }

    @Override
    protected byte[] kvsetIfAbsentPrivate(String key, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        try {
            KeyData keyData = getKeyData(key);
            if(keyData != null){
                return keyData.bytes;
            }else {
                rocksDB.put(genKey(key), genValue(bytes, TTL_DEFAULT));
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("setIfAbsent key " + key + " bytes " + Arrays.toString(bytes), e));
        }
        return null;
    }

    @Override
    protected Integer kvsetIfAbsentPrivate(String key, byte[] bytes, Long expire) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes, expire);
        try {
            KeyData keyData = getKeyData(key);
            if(keyData != null){
               return 0;
            }else {
                rocksDB.put(genKey(key), genValue(bytes, System.currentTimeMillis() + expire));
                try {
                    expire(key);
                }catch (Throwable t){
                    rocksDB.put(genKey(key), genValue(bytes, TTL_DEFAULT));
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " setIfAbsent key " + key + " bytes " + Arrays.toString(bytes) + " failed", t);
                }
                return 1;
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("setIfAbsent key " + key + " bytes " + Arrays.toString(bytes), e));
        }
    }

    @Override
    protected byte[] kvappendPrivate(String key, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytes);
        try {
            KeyData keyData = getKeyData(key);
            if(keyData != null){
                byte[] oldBytes = keyData.bytes;
                byte[] newBytes = new byte[oldBytes.length + bytes.length];
                ByteUtils.copyBytes(0, newBytes, oldBytes);
                ByteUtils.copyBytes(oldBytes.length, newBytes, bytes);
                rocksDB.put(genKey(key), genValue(newBytes, keyData.ttl));
                return newBytes;
            }else {
                rocksDB.put(genKey(key), genValue(bytes, TTL_DEFAULT));
                return bytes;
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("append key " + key + " bytes " + Arrays.toString(bytes), e));
        }
    }

    @Override
    protected byte[] kvgetPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyData keyData = getKeyData(key);
            if(keyData != null){
                return keyData.bytes;
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("get key " + key, e));
        }
        return null;
    }

    @Override
    protected Integer kvlenPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyData keyData = getKeyData(key);
            if(keyData != null){
                return keyData.bytes.length;
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("len key " + key ,e));
        }
        return 0;
    }

    @Override
    protected void kvexpirePrivate(String key, Long expire) throws IOException {
        try {
            KeyData keyData = getKeyData(key);
            if(keyData == null){
                throw new IOException(getRocksDBExceptionString("key " + key + " is not exist", null));
            }
            rocksDB.put(genKey(key), genValue(keyData.bytes, System.currentTimeMillis() + expire));
            try {
                expire(key);
                }catch (Throwable t){
                    rocksDB.put(genKey(key), genValue(keyData.bytes, keyData.ttl));
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                throw new IOException(structure + " expire key " + key + " expire " + expire + " failed", t);
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("expire key " + key + " expire " + expire ,e));
        }
    }

    @Override
    protected void kvdel(String... keys) throws IOException {
        ValidateUtils.checkNotNull(keys);
        try {
            Map<String, KeyData> existsMap = new HashMap<>();
            for (String key : keys){
                try {
                    KeyData keyData = getKeyData(key);
                    if(keyData != null){
                        del(key);
                        existsMap.put(key, keyData);
                    }
                }catch (Throwable t){
                    if(!existsMap.isEmpty()){
                        for (String existKey : existsMap.keySet()){
                            KeyData keyData = existsMap.get(existKey);
                            if(keyData.ttl == TTL_DEFAULT){
                                setIfAbsent(existKey, keyData.bytes);
                            }else {
                                setIfAbsent(existKey, keyData.bytes, keyData.ttl);
                            }
                        }
                    }
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " del key " + key + " failed", t);
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("del keys " + Arrays.toString(keys), e));
        }
    }

    @Override
    protected void kvdelPrivate(String key) throws IOException {
        try {
            KeyData keyData = getKeyData(key);
            if(keyData != null){
                rocksDB.delete(genKey(key));
                rocksDB.delete(genExpireKeyData(key));
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("del key " + key, e));
        }
    }

    private void expire(String key) throws IOException, RocksDBException{
        rocksDB.put(genExpireKeyData(key), new byte[0]);
    }

    @Override
    protected byte[] generateLockKey(String key) throws IOException {
        return genKey(key);
    }

    private KeyData getKeyData(String key) throws IOException, RocksDBException{
        byte[] valueBytes = rocksDB.get(genKey(key));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                long ttl = dataInputStream.readLong();
                if(ttl == TTL_DEFAULT || ttl >= System.currentTimeMillis()){
                    int length = dataInputStream.readInt();
                    if(length > 0){
                        byte[] bytes = new byte[length];
                        dataInputStream.readFully(bytes);
                        return new KeyData(bytes, ttl);
                    }
                }
            }
        }
        return null;
    }
    private KeyData getKeyDataAll(String key) throws IOException, RocksDBException{
        byte[] valueBytes = rocksDB.get(genKey(key));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                long ttl = dataInputStream.readLong();
                int length = dataInputStream.readInt();
                if(length > 0){
                    byte[] bytes = new byte[length];
                    dataInputStream.readFully(bytes);
                    return new KeyData(bytes, ttl);
                }
            }
        }
        return null;
    }
    private byte[] genKey(String key) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_PREFIX);
            dataOutputStream.writeBytes(key);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genExpireKeyData(String key) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(EXPIRE);
            dataOutputStream.writeUTF(key);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genValue(byte[] bytes, long ttl) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(ttl);
            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);
            return byteArrayOutputStream.toByteArray();
        }
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
                    ReadWriteLock lock = getLock(key);
                    try {
                        lock.writeLock().lock();
                        byte[] valueBytes = rocksDB.get(genKey(key));
                        if(valueBytes != null){
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
                            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
                            try (byteArrayInputStream; dataInputStream){
                                long ttl = dataInputStream.readLong();
                                if(ttl != TTL_DEFAULT && ttl < System.currentTimeMillis()){
                                    rocksDB.delete(genKey(key));
                                    rocksDB.delete(genExpireKeyData(key));
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
    }
}
