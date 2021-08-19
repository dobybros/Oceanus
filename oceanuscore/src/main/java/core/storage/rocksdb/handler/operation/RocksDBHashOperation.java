package core.storage.rocksdb.handler.operation;

import chat.logs.LoggerEx;
import core.common.InternalTools;

import core.storage.adapters.assist.impl.queue.CommonHashOperation;
import core.storage.rocksdb.data.structure.common.KeyExpireData;
import core.storage.rocksdb.data.structure.hash.KeyFieldData;
import core.storage.rocksdb.data.structure.hash.KeyMetaData;
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
public class RocksDBHashOperation extends CommonHashOperation {
    private static final String TAG = RocksDBHashOperation.class.getSimpleName();
    private RocksDB rocksDB;
    public RocksDBHashOperation(RocksDB rocksDB, InternalTools internalTools){
        this.rocksDB = rocksDB;
        internalTools.getTimer().schedule(new ScheduleTask("RocksDBHashOperation_clear_" + this.rocksDB.getName()) {
            @Override
            public void execute() {
                clear();
            }
        }, "0 20 0 1/10 * ? *");
    }
    private final byte[] KEY_PREFIX = new byte[]{'R','B','A'};
    private final byte[] KEY_FIELD_PREFIX = new byte[]{'R','B', 'a'};
    private final byte[] EXPIRE = new byte[]{'z','z','z'};


    @Override
    protected byte[] getPrivate(String key, String field) throws IOException {
        ValidateUtils.checkAllNotNull(key, field);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                KeyFieldData keyFieldData = getKeyFieldData(key, field, keyMataData.version);
                if(keyFieldData != null){
                    return keyFieldData.value;
                }
            }
        }catch(RocksDBException e){
            throw new IOException(getRocksDBExceptionString("get key " + key, e));
        }
        return null;
    }

    @Override
    protected Map<String, byte[]> getAllPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyMetaData keyMetaData = getMetaData(key);
            if(keyMetaData != null){
                String firstField = keyMetaData.firstField;
                Map<String, byte[]> fieldBytesMap = new HashMap<>();
                String field = firstField;
                while (!field.equals(KeyFieldData.FIELD_DEFAULT)){
                    byte[] fieldValueBytes = rocksDB.get(genKeyFieldBytes(key, field, keyMetaData.version));
                    if(fieldValueBytes != null){
                        ByteArrayInputStream fieldByteArrayInputStream = new ByteArrayInputStream(fieldValueBytes);
                        DataInputStream fieldDataInputStream = new DataInputStream(fieldByteArrayInputStream);
                        try (fieldByteArrayInputStream; fieldDataInputStream){
                            String nextField = fieldDataInputStream.readUTF();
                            int length = fieldDataInputStream.readInt();
                            byte[] factValueBytes = new byte[length];
                            fieldDataInputStream.readFully(factValueBytes);
                            fieldBytesMap.put(field, factValueBytes);
                            field = nextField;
                        }
                    }else {
                        break;
                    }
                }
                if(!fieldBytesMap.isEmpty()){
                    return fieldBytesMap;
                }
            }
        }catch(RocksDBException e){
            throw new IOException(getRocksDBExceptionString("getall key " + key, e));
        }
        return null;
    }

    @Override
    protected void setPrivate(String key, String field, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, field, bytes);
        setX(key, field, bytes, false);
    }
    private byte[] setX(String key, String field, byte[] bytes, boolean needNotExist) throws IOException{
        try {
            KeyMetaData keyMataData = getMetaData(key);
            KeyFieldData keyFieldData = null;
            if(keyMataData == null){
                keyMataData = new KeyMetaData();
            }else {
                keyFieldData = getKeyFieldData(key, field, keyMataData.version);
            }
            if(keyMataData.length.equals(KeyMetaData.LENGTH_DEFAULT)){
                keyMataData.firstField = field;
            }
            boolean isNewKeyField = false;
            String preField = keyMataData.lastField;
            if(keyFieldData == null){
                isNewKeyField = true;
                keyMataData.lastField = field;
            }
            if(needNotExist){
                if(!isNewKeyField){
                    return keyFieldData.value;
                }
            }
            if(isNewKeyField){
                rocksDB.put(genKeyFieldBytes(key, field, keyMataData.version), genKeyFieldValueBytes(bytes, preField, KeyFieldData.FIELD_DEFAULT));
                try {
                    KeyFieldData preKeyFieldData = getKeyFieldData(key, preField, keyMataData.version);
                    if(preKeyFieldData != null){
                        rocksDB.put(genKeyFieldBytes(key, preField, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.value, preKeyFieldData.preField, field));
                    }
                    try {
                        rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstField, keyMataData.lastField, keyMataData.length + 1));
                    }catch (Throwable t){
                        if(preKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, preField, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.value, preKeyFieldData.preField, preKeyFieldData.nextField));
                        }
                        throw t;
                    }
                }catch (Throwable t){
                    rocksDB.delete(genKeyFieldBytes(key, field, keyMataData.version));
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " set key " + key + " field " + field + " bytes " + Arrays.toString(bytes) + " expire " + null + " needNotExist " + needNotExist + " failed", t);
                }
            }else {
                if(!Arrays.equals(keyFieldData.value, bytes)){
                    rocksDB.put(genKeyFieldBytes(key, field, keyMataData.version), genKeyFieldValueBytes(bytes, keyFieldData.preField, keyFieldData.nextField));
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("set key " + key + " field " + field + " bytes " + Arrays.toString(bytes) + " expire " + null + " needNotExist " + needNotExist, e));
        }
        return null;
    }

    @Override
    protected byte[] setIfAbsentPrivate(String key, String field, byte[] bytes) throws IOException {
        ValidateUtils.checkAllNotNull(key, field, bytes);
        return setX(key, field, bytes, true);
    }

    @Override
    protected Long lenPrivate(String key) throws IOException {
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
    protected void delPrivate(String key) throws IOException {
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version + 1, KeyMetaData.TTL_DEFAULT, KeyMetaData.FIELD_DEFAULT, KeyMetaData.FIELD_DEFAULT, KeyMetaData.LENGTH_DEFAULT));
                try {
                    deleteAsync(key, keyMataData);
                }catch (Throwable t){
                    rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstField, keyMataData.lastField, keyMataData.length));
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " del key " + key + " failed", t);
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("del key " + key, e));
        }
    }

    @Override
    protected void delPrivate(String key, String... fields) throws IOException {
        ValidateUtils.checkAllNotNull(key, fields);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                Map<String, KeyFieldData> existMap = new HashMap<>();
                for (String field : fields){
                    try {
                        KeyFieldData keyFieldData = getKeyFieldData(key, field, keyMataData.version);
                        if(keyFieldData != null){
                            delPrivate(key, field);
                            existMap.put(field, keyFieldData);
                        }
                    }catch (Throwable t){
                        if(!existMap.isEmpty()){
                            for (String existField : existMap.keySet()){
                                KeyFieldData keyFieldData = existMap.get(existField);
                                setIfAbsentPrivate(key, existField, keyFieldData.value);
                            }
                        }
                        if(t instanceof RocksDBException){
                            throw t;
                        }
                        throw new IOException(structure + " del key " + key + " field " + field + " failed", t);
                    }
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("del key " + key + " fields " + Arrays.toString(fields), e));
        }
    }

    @Override
    protected void delPrivate(String key, String field) throws IOException {
        ValidateUtils.checkAllNotNull(key, field);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                KeyFieldData keyFieldData = getKeyFieldData(key, field, keyMataData.version);
                if(keyFieldData != null){
                    String firstField = keyMataData.firstField;
                    String lastField = keyMataData.lastField;
                    if(field.equals(keyMataData.firstField)){
                        firstField = keyFieldData.nextField;
                    }else if(field.equals(keyMataData.lastField)){
                        lastField = keyFieldData.preField;
                    }
                    long length = keyMataData.length - 1;
                    KeyFieldData preKeyFieldData = null;
                    KeyFieldData nextKeyFieldData = null;
                    if(length > 0){
                        preKeyFieldData = getKeyFieldData(key, keyFieldData.preField, keyMataData.version);
                        if(preKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.preField, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.value, preKeyFieldData.preField, keyFieldData.nextField));
                        }
                        nextKeyFieldData = getKeyFieldData(key, keyFieldData.nextField, keyMataData.version);
                        if(nextKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.nextField, keyMataData.version), genKeyFieldValueBytes(nextKeyFieldData.value, keyFieldData.preField, nextKeyFieldData.nextField));
                        }
                    }else {
                        firstField = KeyFieldData.FIELD_DEFAULT;
                        lastField = KeyFieldData.FIELD_DEFAULT;
                    }
                    try {
                        rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, firstField, lastField, length));
                        try {
                            rocksDB.delete(genKeyFieldBytes(key, field, keyMataData.version));
                        }catch (Throwable t){
                            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstField, keyMataData.lastField, keyMataData.length));
                            throw t;
                        }
                    }catch (Throwable t){
                        if(preKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.preField, keyMataData.version), genKeyFieldValueBytes(preKeyFieldData.value, preKeyFieldData.preField, preKeyFieldData.nextField));
                        }
                        if(nextKeyFieldData != null){
                            rocksDB.put(genKeyFieldBytes(key, keyFieldData.nextField, keyMataData.version), genKeyFieldValueBytes(nextKeyFieldData.value, nextKeyFieldData.preField, nextKeyFieldData.nextField));
                        }
                        if(t instanceof RocksDBException){
                            throw t;
                        }
                        throw new IOException(structure + "del key " + key + " field " + field + " failed", t);
                    }
                }
            }
        }catch (RocksDBException t){
            throw new IOException(getRocksDBExceptionString("del key "+ key + " field "+ field, t));
        }
    }

    @Override
    protected void mSetPrivate(String key, Map<String, byte[]> tMap) throws IOException {
        ValidateUtils.checkAllNotNull(key, tMap);
        if(!tMap.isEmpty()) {
            try {
                KeyMetaData keyMataData = getMetaData(key);
                Map<String, KeyFieldData> keyFieldDatas = new HashMap<>();
                List<String> addSuccessFields = new ArrayList<>();
                for (String field : tMap.keySet()) {
                    try {
                        if (keyMataData != null) {
                            KeyFieldData keyFieldData = getKeyFieldData(key, field, keyMataData.version);
                            if (keyFieldData != null) {
                                keyFieldDatas.put(field, keyFieldData);
                            }
                        }
                        setX(key, field, tMap.get(field), false);
                        addSuccessFields.add(field);
                    } catch (Throwable t) {
                        if(!addSuccessFields.isEmpty()){
                            delPrivate(key, addSuccessFields.toArray(new String[0]));
                            if(!keyFieldDatas.isEmpty()){
                                for (String existField : keyFieldDatas.keySet()){
                                    if(addSuccessFields.contains(existField)){
                                        KeyFieldData keyFieldData = keyFieldDatas.get(existField);
                                        setIfAbsentPrivate(key, existField, keyFieldData.value);
                                    }
                                }
                            }
                        }
                        if(t instanceof RocksDBException){
                            throw t;
                        }
                        throw new IOException(structure + " set key " + key + " field " + field + " failed", t);
                    }
                }

            }catch (RocksDBException e){
                throw new IOException("mSet key " + key, e);
            }
        }
    }

    @Override
    protected List<byte[]> mGetPrivate(String key, String... fields) throws IOException {
        ValidateUtils.checkAllNotNull(key, fields);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData != null){
                List<byte[]> values = new ArrayList<>();
                for (String field : fields){
                    KeyFieldData keyFieldData = getKeyFieldData(key, field, keyMataData.version);
                    if(keyFieldData != null){
                        values.add(keyFieldData.value);
                    }
                }
                if(!values.isEmpty()){
                    return values;
                }
            }
        }catch(RocksDBException e){
            throw new IOException(getRocksDBExceptionString("mget key " + key, e));
        }
        return null;
    }

    @Override
    protected void expirePrivate(String key, Long expire) throws IOException {
        ValidateUtils.checkAllNotNull(key, expire);
        try {
            KeyMetaData keyMataData = getMetaData(key);
            if(keyMataData == null){
                throw new IOException("Key " + key + " not exist when expire");
            }
            rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, System.currentTimeMillis() + expire, keyMataData.firstField, keyMataData.lastField, keyMataData.length));
            try {
                deleteAsync(key, getMetaData(key));
            }catch (Throwable t){
                rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(keyMataData.version, keyMataData.ttl, keyMataData.firstField, keyMataData.lastField, keyMataData.length));
                if(t instanceof RocksDBException){
                    throw t;
                }
                throw new IOException(structure + " expire key " + key + " expire " + " failed", t);
            }
        }catch (RocksDBException e){
            throw new IOException("expire key " + key + " expire " + expire, e);
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
            dataOutputStream.writeUTF(keyMataData.firstField);
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
                     String firstField = dataInputStream.readUTF();
                     String lastField = dataInputStream.readUTF();
                     long length = dataInputStream.readLong();
                     return new KeyMetaData(version, ttl, firstField, lastField, length);
                 }else {
                     rocksDB.put(genMetaDataKeyBytes(key), genMetaDataKeyValueBytes(version + 1, KeyMetaData.TTL_DEFAULT, KeyMetaData.FIELD_DEFAULT, KeyMetaData.FIELD_DEFAULT, KeyMetaData.LENGTH_DEFAULT));
                     return new KeyMetaData(version + 1, KeyMetaData.TTL_DEFAULT, KeyMetaData.FIELD_DEFAULT, KeyMetaData.FIELD_DEFAULT, KeyMetaData.LENGTH_DEFAULT);
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
                String firstField = dataInputStream.readUTF();
                String lastField = dataInputStream.readUTF();
                long length = dataInputStream.readLong();
                return new KeyMetaData(version, ttl, firstField, lastField, length);
            }
        }
        return null;
    }
    private KeyFieldData getKeyFieldData(String key, String field, Long version) throws IOException,RocksDBException{
        byte[] valueBytes = rocksDB.get(genKeyFieldBytes(key, field, version));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                String nextField = dataInputStream.readUTF();
                int length = dataInputStream.readInt();
                byte[] factValueBytes = new byte[length];
                dataInputStream.readFully(factValueBytes);
                String preField = dataInputStream.readUTF();
                return new KeyFieldData(preField, nextField, factValueBytes);
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

    private byte[] genMetaDataKeyValueBytes(long version, long ttl, String firstField, String lastField, long length) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(version);
            dataOutputStream.writeLong(ttl);
            dataOutputStream.writeUTF(firstField);
            dataOutputStream.writeUTF(lastField);
            dataOutputStream.writeLong(length);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genKeyFieldBytes(String key, String field, long version) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_FIELD_PREFIX);
            dataOutputStream.writeBytes(key);
            dataOutputStream.writeLong(version);
            dataOutputStream.writeBytes(field);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] genKeyFieldValueBytes(byte[] valueBytes, String preField, String nextField) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeUTF(nextField);
            dataOutputStream.writeInt(valueBytes.length);
            dataOutputStream.write(valueBytes);
            dataOutputStream.writeUTF(preField);
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
            dataOutputStream.writeUTF(key);
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
                                            String firstField = dataInputStream.readUTF();
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
                                                LoggerEx.info(TAG, structure + "key: " + key + ",meta version: " + (keyMataData == null ? null : keyMataData.version) + ",will delete version: " + version + ", meta ttl: " + (keyMataData == null ? null : keyMataData.ttl) + ", will delete ttl: " + ttl);
                                                String field = firstField;
                                                while (!field.equals(KeyFieldData.FIELD_DEFAULT)){
                                                    byte[] fieldValueBytes = rocksDB.get(genKeyFieldBytes(key, field, version));
                                                    if(fieldValueBytes != null){
                                                        rocksDB.delete(genKeyFieldBytes(key, field, version));
                                                        ByteArrayInputStream fieldByteArrayInputStream = new ByteArrayInputStream(fieldValueBytes);
                                                        DataInputStream fieldDataInputStream = new DataInputStream(fieldByteArrayInputStream);
                                                        try (fieldByteArrayInputStream; fieldDataInputStream){
                                                            field = fieldDataInputStream.readUTF();
                                                        }
                                                    }
                                                }

                                            }
                                            if(reAdd){
                                                map.put("version", version);
                                                map.put("ttl", ttl);
                                                map.put("firstField", firstField);
                                                cantDeleteList.add(map);
                                            }
                                        }
                                    }
                                    if(!cantDeleteList.isEmpty()){
                                        dataOutputStream.writeInt(cantDeleteList.size());
                                        for (Map map : cantDeleteList){
                                            dataOutputStream.writeLong((long)map.get("version"));
                                            dataOutputStream.writeLong((long)map.get("ttl"));
                                            dataOutputStream.writeUTF((String) map.get("firstField"));
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
