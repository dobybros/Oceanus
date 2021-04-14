package core.storage.rocksdb.handler.operation;

import core.common.InternalTools;
import core.log.LoggerHelper;
import core.storage.adapters.assist.impl.queue.CommonZSetOperation;
import core.storage.adapters.data.zset.Tuple;
import core.storage.rocksdb.data.structure.common.KeyExpireData;
import core.storage.rocksdb.data.structure.zset.KeyFieldLevelData;
import core.storage.rocksdb.data.structure.zset.KeyMetaData;
import core.utils.ValidateUtils;
import core.utils.scheduled.ScheduleTask;
import org.rocksdb.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/11/3.
 * Description：
 */
public class RocksDBZSetOperation extends CommonZSetOperation {
    private RocksDB rocksDB;
    public RocksDBZSetOperation(RocksDB rocksDB, InternalTools internalTools){
        this.rocksDB = rocksDB;
        internalTools.getTimer().schedule(new ScheduleTask("RocksDBZSetOperation_clear_" + this.rocksDB.getName()) {
            @Override
            public void execute() {
                clear();
            }
        }, "0 50 0 1/10 * ? *");
    }
    private final byte[] KEY_PREFIX = new byte[]{'R','E','A'};
    private final byte[] KEY_FIELD_LEVEL = new byte[]{'R','E','a'};
    private final byte[] KEY_FIELD_LEVEL_FIRST = new byte[]{'R','E','B'};
    private final byte[] EXPIRE = new byte[]{'X','X','X'};

    @Override
    protected Long zaddPrivate(String key, double score, byte[] bytes) throws IOException {
        if(score < 0){
            throw new IllegalArgumentException("Score more than 0, score: " + score);
        }
        KeyMetaData keyMetaData = null;
        try {
            keyMetaData = getKeyMetaData(key);
            if(keyMetaData == null){
                keyMetaData = new KeyMetaData();
            }
            KeyFieldLevelData thisKeyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
            if(thisKeyFieldLevelData == null){
                if(!keyMetaData.maxLevel.equals(KeyMetaData.LEVEL_DEFAULT)){
                    Map<ByteBuffer, KeyFieldLevelData> cacheMap = new HashMap<>();
                    byte[] headBytes = genKeyLevelDataHead(key, keyMetaData.version, keyMetaData.maxLevel);
                    KeyFieldLevelData keyFieldLevelData = getCacheKeyFieldLevelData(key, headBytes, keyMetaData.version, keyMetaData.maxLevel, cacheMap);
                    assert keyFieldLevelData != null;
                    byte[] next = headBytes;
                    long currentLevel = keyMetaData.maxLevel;
                    InternalData internalData = new InternalData(getLevel(keyMetaData.length));
                    long i =0;
                    while (true){
                        while (keyFieldLevelData.span != 0){
                            if(!Arrays.equals(keyFieldLevelData.nextValues, internalData.maxBytes)){
                                KeyFieldLevelData theKeyFieldLevelData = getCacheKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, currentLevel, cacheMap);
                                assert theKeyFieldLevelData != null;
                                if(theKeyFieldLevelData.score < score){
                                    internalData.addSpan(i, keyFieldLevelData.span);
                                    next = keyFieldLevelData.nextValues;
                                    internalData.maxBytes = next;
                                    keyFieldLevelData = theKeyFieldLevelData;
                                }else {
                                    break;
                                }
                            }else {
                                break;
                            }
                        }
                        if(currentLevel > internalData.level){
                            keyFieldLevelData = getCacheKeyFieldLevelData(key, next, keyMetaData.version, currentLevel, cacheMap);
                            if(keyFieldLevelData.span != 0){
                                keyFieldLevelData.span += 1;
                                putCacheKeyFieldLevelData(key, next, keyMetaData.version, currentLevel, cacheMap, keyFieldLevelData);
                            }
                        }else {
                            internalData.addLevelData(i++, next);
                        }
                        currentLevel --;
                        if(!Arrays.equals(keyFieldLevelData.downValues, KeyMetaData.BYTES_DEFAULT)){
                            next = keyFieldLevelData.downValues;
                            byte[] down = keyFieldLevelData.downValues;
                            keyFieldLevelData = getCacheKeyFieldLevelData(key, keyFieldLevelData.downValues, keyMetaData.version, currentLevel, cacheMap);
                            if(keyFieldLevelData == null){
                                System.out.println("down: " + Arrays.toString(down) + ",currentLevel: " + currentLevel);
                            }
                            assert keyFieldLevelData != null;
                        }else {
                            break;
                        }
                    }
                    currentLevel = currentLevel < 0 ? 0 : currentLevel;
                    next = internalData.getBytes(--i);
                    KeyFieldLevelData newKeyFieldLevelData = new KeyFieldLevelData(keyFieldLevelData.nextValues, KeyMetaData.BYTES_DEFAULT, keyFieldLevelData.span == 0 ? 0 : 1, score);
                    putCacheKeyFieldLevelData(key, bytes, keyMetaData.version, currentLevel, cacheMap, newKeyFieldLevelData);
                    System.out.println("bytes: " + new String(bytes) + ",pre: " + ((next.length) > 10 ? ("level: " + currentLevel) : new String(next)) + ",next: " + new String(keyFieldLevelData.nextValues));
                    keyFieldLevelData.nextValues = bytes;
                    keyFieldLevelData.span = 1;
                    putCacheKeyFieldLevelData(key, next, keyMetaData.version, currentLevel, cacheMap, keyFieldLevelData);
                    keyMetaData.length += 1;
                    compareBytesScore(key, bytes, score, internalData, i, keyMetaData, cacheMap);
                }else {
                    rocksDB.put(genKeyFieldLevelBytes(key, bytes, keyMetaData.version, 0), genKeyFieldLevelValueBytes(KeyMetaData.BYTES_DEFAULT, KeyMetaData.BYTES_DEFAULT, 0, score));
                    rocksDB.put(genKeyFieldLevelBytes(key, genKeyLevelDataHead(key, keyMetaData.version, 0), keyMetaData.version, 0), genKeyFieldLevelValueBytes(bytes, KeyMetaData.BYTES_DEFAULT, 1, 0));
                    rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(keyMetaData.ttl, keyMetaData.version, keyMetaData.length + 1, 0));
                }
            }else {
                if(thisKeyFieldLevelData.score != score){
                    deleteField(key, keyMetaData, bytes);
                    zaddPrivate(key, score, bytes);
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("add key " + key + " score " + score + " bytes " + Arrays.toString(bytes), e));
        }
        return keyMetaData.length;
    }
    private void compareBytesScore(String key, byte[] bytes, double score, InternalData internalData, long index, KeyMetaData keyMetaData, Map<ByteBuffer, KeyFieldLevelData> cacheMap) throws IOException, RocksDBException {
        long level = internalData.level;
        int i = 1;
        while (i <= level){
            if(index > 0){
                byte[] pre = internalData.getBytes(--index);
                KeyFieldLevelData preKeyFieldLevelData = getCacheKeyFieldLevelData(key, pre, keyMetaData.version, i, cacheMap);
                assert preKeyFieldLevelData != null;
                int span = internalData.getSPan(index);
                KeyFieldLevelData newKeyFieldLevelData = new KeyFieldLevelData(preKeyFieldLevelData.nextValues, bytes,preKeyFieldLevelData.span == 0 ? 0 : preKeyFieldLevelData.span - span, score);
                preKeyFieldLevelData.nextValues = bytes;
                preKeyFieldLevelData.span = span + 1;
                putCacheKeyFieldLevelData(key, pre, keyMetaData.version, i, cacheMap, preKeyFieldLevelData);
                putCacheKeyFieldLevelData(key, bytes, keyMetaData.version, i, cacheMap, newKeyFieldLevelData);
                System.out.println("level: " + i + ",bytes: " + new String(bytes) + ",span: " + newKeyFieldLevelData.span + ",pre: " + ((pre.length) > 10 ? ("level: " + i) : new String(pre)) + ",span: " + preKeyFieldLevelData.span + ",next: " + new String(newKeyFieldLevelData.nextValues)) ;
            } else {
                KeyFieldLevelData newKeyFieldLevelData = new KeyFieldLevelData(KeyMetaData.BYTES_DEFAULT, bytes, 0, score);
                putCacheKeyFieldLevelData(key, bytes, keyMetaData.version, i, cacheMap, newKeyFieldLevelData);
                rocksDB.put(genKeyFieldLevelBytes(key, genKeyLevelDataHead(key, keyMetaData.version, i), keyMetaData.version, i), genKeyFieldLevelValueBytes(bytes, genKeyLevelDataHead(key, keyMetaData.version, i - 1), internalData.span + 1, 0));
                System.out.println("level: " + i + ",bytes: " + new String(bytes) + ",span0: " + newKeyFieldLevelData.span + ",pre: " +  ("level: " + i) + ",span: " + (internalData.span + 1));
            }
            i++;
        }
        if(level > keyMetaData.maxLevel){
            keyMetaData.maxLevel = level;
        }
        rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(keyMetaData.ttl, keyMetaData.version, keyMetaData.length, keyMetaData.maxLevel));
    }
    private KeyFieldLevelData getCacheKeyFieldLevelData(String key, byte[] bytes, long version, long level, Map<ByteBuffer, KeyFieldLevelData> cacheMap) throws IOException, RocksDBException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(genKeyFieldLevelBytes(key, bytes, version, level));
        KeyFieldLevelData keyFieldLevelData = cacheMap.get(byteBuffer);
        if(keyFieldLevelData == null){
            keyFieldLevelData = getKeyFieldLevelData(key, bytes, version, level);
            cacheMap.put(byteBuffer, keyFieldLevelData);
        }
        return keyFieldLevelData;
    }
    private void putCacheKeyFieldLevelData(String key, byte[] bytes, long version, long level, Map<ByteBuffer, KeyFieldLevelData> cacheMap, KeyFieldLevelData keyFieldLevelData) throws IOException, RocksDBException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(genKeyFieldLevelBytes(key, bytes, version, level));
        cacheMap.put(byteBuffer, keyFieldLevelData);
        rocksDB.put(genKeyFieldLevelBytes(key, bytes, version, level), genKeyFieldLevelValueBytes(keyFieldLevelData.nextValues, keyFieldLevelData.downValues, keyFieldLevelData.span, keyFieldLevelData.score));
    }
    @Override
    protected Long zaddPrivate(String key, Map<byte[], Double> bytesScores) throws IOException {
        ValidateUtils.checkAllNotNull(key, bytesScores);
        long length = 0L;
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            Map<byte[], KeyFieldLevelData> keyFieldDatas = new HashMap<>();
            List<byte[]> addSuccessBytes = new ArrayList<>();
            for (byte[] bytes : bytesScores.keySet()){
                try {
                    if(keyMetaData != null){
                        KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
                        if(keyFieldLevelData != null){
                            keyFieldDatas.put(bytes, keyFieldLevelData);
                        }
                    }
                    double score = bytesScores.get(bytes);
                    length = zaddPrivate(key, score, bytes);
                    addSuccessBytes.add(bytes);
                }catch (Throwable t){
                    if(!addSuccessBytes.isEmpty()){
                        zdelPrivate(key, addSuccessBytes.toArray(new byte[0][0]));
                        if(!keyFieldDatas.isEmpty()){
                            for (byte[] value : keyFieldDatas.keySet()){
                                if(addSuccessBytes.contains(value)){
                                    zaddPrivate(key, keyFieldDatas.get(value).score, value);
                                }
                            }
                        }
                    }
                    if(t instanceof RocksDBException){
                        throw t;
                    }
                    throw new IOException(structure + " del key " + key + " bytes " + Arrays.toString(bytes) + " failed", t);
                }
            }

        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("addMultiple key: " + key + ",bytesScores: " + bytesScores, e));
        }
        return length;
    }

    @Override
    protected Long zlenPrivate(String key) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                return keyMetaData.length;
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("len key" + key, e));
        }
        return 0L;
    }

    @Override
    protected Long zlenPrivate(String key, double minScore, double maxScore) throws IOException {
        Set<Tuple> tuples = zgetByScorePrivate(key, minScore, maxScore);
        if(tuples != null){
            return (long) tuples.size();
        }
        return super.zlenPrivate(key, minScore, maxScore);
    }

    @Override
    protected Double zincrbyPrivate(String key, double incrScore, byte[] bytes) throws IOException {
        try {
            double newScore = incrScore;
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
                if(keyFieldLevelData != null){
                    newScore = keyFieldLevelData.score + incrScore;
                }
            }
            zaddPrivate(key, newScore, bytes);
        }catch (RocksDBException t){
            throw new IOException(getRocksDBExceptionString("incrby key " + key + " incrScore " + incrScore + " bytes " + Arrays.toString(bytes), t));
        }
        return super.zincrbyPrivate(key, incrScore, bytes);
    }
    private KeyFieldLevelData findByScore(String key, double minScore, KeyMetaData keyMetaData) throws IOException, RocksDBException {
        KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, genKeyLevelDataHead(key, keyMetaData.version, keyMetaData.maxLevel), keyMetaData.version, keyMetaData.maxLevel);
        assert keyFieldLevelData != null;
        long currentLevel = keyMetaData.maxLevel;
        while (true){
            while (keyFieldLevelData.span != 0){
                KeyFieldLevelData theKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, currentLevel);
                assert theKeyFieldLevelData != null;
                if(minScore > theKeyFieldLevelData.score){
                    keyFieldLevelData = theKeyFieldLevelData;
                }else {
                    break;
                }
            }
            currentLevel --;
            if(!Arrays.equals(keyFieldLevelData.downValues, KeyMetaData.BYTES_DEFAULT)){
                keyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.downValues, keyMetaData.version, currentLevel);
                assert keyFieldLevelData != null;
            }else {
                break;
            }
        }
        return keyFieldLevelData;
    }
    private Long findIndexByScore(String key, double score, byte[] bytes, KeyMetaData keyMetaData) throws IOException, RocksDBException {
        KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, genKeyLevelDataHead(key, keyMetaData.version, keyMetaData.maxLevel), keyMetaData.version, keyMetaData.maxLevel);
        assert keyFieldLevelData != null;
        long currentLevel = keyMetaData.maxLevel;
        long span = -1;
        byte[] sameBytes = null;
        while (true){
            while (keyFieldLevelData.span != 0){
                KeyFieldLevelData theKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, currentLevel);
                assert theKeyFieldLevelData != null;
                if(score == theKeyFieldLevelData.score){
                    if(Arrays.equals(bytes, keyFieldLevelData.nextValues)){
                        return span + keyFieldLevelData.span;
                    }else {
                        long theSpan = span + keyFieldLevelData.span;
                        if(sameBytes != null && Arrays.equals(sameBytes, keyFieldLevelData.nextValues)){
                            break;
                        }
                        sameBytes = keyFieldLevelData.nextValues;
                        if(currentLevel != 0){
                            theKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, 0);
                            assert theKeyFieldLevelData != null;
                        }
                        while (theKeyFieldLevelData.span != 0){
                            KeyFieldLevelData thisKeyFieldLevelData = getKeyFieldLevelData(key, theKeyFieldLevelData.nextValues, keyMetaData.version, 0);
                            assert thisKeyFieldLevelData != null;
                            if(theKeyFieldLevelData.score == score){
                                if(Arrays.equals(theKeyFieldLevelData.nextValues, bytes)){
                                    return ++theSpan;
                                }else {
                                    theSpan++;
                                    theKeyFieldLevelData = thisKeyFieldLevelData;
                                }
                            }else {
                                break;
                            }
                        }
                        break;
                    }
                }else if(score > theKeyFieldLevelData.score){
                    span += keyFieldLevelData.span;
                    keyFieldLevelData = theKeyFieldLevelData;
                }else {
                    break;
                }
            }
            currentLevel --;
            if(!Arrays.equals(keyFieldLevelData.downValues, KeyMetaData.BYTES_DEFAULT)){
                keyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.downValues, keyMetaData.version, currentLevel);
                assert keyFieldLevelData != null;
            }else {
                break;
            }
        }
        return null;
    }
    private byte[] findBySpan(String key, long start, KeyMetaData keyMetaData) throws IOException, RocksDBException{
        if(start == 0){
            KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, genKeyLevelDataHead(key, keyMetaData.version, 0), keyMetaData.version, 0);
            assert keyFieldLevelData != null;
            return keyFieldLevelData.nextValues;
        }
        KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, genKeyLevelDataHead(key, keyMetaData.version, keyMetaData.maxLevel), keyMetaData.version, keyMetaData.maxLevel);
        assert keyFieldLevelData != null;
        long currentLevel = keyMetaData.maxLevel;
        long span = -1;
        while (true){
            while (keyFieldLevelData.span != 0){
                long newSpan = span + keyFieldLevelData.span;
                if(start == newSpan ){
                    return keyFieldLevelData.nextValues;
                }else if(start > newSpan){
                    span = newSpan;
                    keyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, currentLevel);
                    assert keyFieldLevelData != null;
                }else {
                    break;
                }
            }
            currentLevel --;
            if(!Arrays.equals(keyFieldLevelData.downValues, KeyMetaData.BYTES_DEFAULT)){
                keyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.downValues, keyMetaData.version, currentLevel);
                assert keyFieldLevelData != null;
            }else {
                break;
            }
        }
        return null;
    }
    @Override
    protected Set<Tuple> zgetPrivate(String key, long start, long stop) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                long length = keyMetaData.length;
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
                byte[] bytes = findBySpan(key, start, keyMetaData);
                if(bytes != null){
                    Set<Tuple> tuples = new HashSet<>();
                    KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
                    if(keyFieldLevelData != null){
                        start++;
                        tuples.add(new Tuple(bytes, keyFieldLevelData.score));
                        while (start <= stop){
                            KeyFieldLevelData theKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, 0);
                            if(theKeyFieldLevelData == null){
                                break;
                            }
                            tuples.add(new Tuple(keyFieldLevelData.nextValues, theKeyFieldLevelData.score));
                            keyFieldLevelData = theKeyFieldLevelData;
                            start ++;
                        }
                    }
                    return tuples;
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("zget key " + key + " start " + start + " stop " + stop, e));
        }
        return super.zgetPrivate(key, start, stop);
    }

    @Override
    protected Set<Tuple> zgetByScorePrivate(String key, double minScore, double maxScore) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                KeyFieldLevelData keyFieldLevelData = findByScore(key, minScore, keyMetaData);
                KeyFieldLevelData nextKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, 0);
                if(nextKeyFieldLevelData != null){
                    Set<Tuple> tuples = new HashSet<>();
                    while (nextKeyFieldLevelData.score <= maxScore){
                        tuples.add(new Tuple(keyFieldLevelData.nextValues, nextKeyFieldLevelData.score));
                        keyFieldLevelData = nextKeyFieldLevelData;
                        if(keyFieldLevelData.span != 0 ){
                            nextKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, 0);
                            assert nextKeyFieldLevelData != null;
                        }else {
                            break;
                        }
                    }
                    return tuples;
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("getByScore key " + key + " minScore " + minScore + " maxScore ", e));
        }
        return null;
    }

    @Override
    protected Long zrankPrivate(String key, byte[] bytes) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
                if(keyFieldLevelData != null){
                    return findIndexByScore(key, keyFieldLevelData.score, bytes, keyMetaData);
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("rank key " + key, e));
        }
        return super.zrankPrivate(key, bytes);
    }

    @Override
    protected void zdelPrivate(String key, byte[]... bytes) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                Map<byte[], KeyFieldLevelData> existMap = new HashMap<>();
                for (byte[] value : bytes){
                    try {
                        KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, value, keyMetaData.version, 0);
                        deleteField(key, keyMetaData, value);
                        if(keyFieldLevelData != null){
                            existMap.put(value, keyFieldLevelData);
                        }
                    }catch (Throwable t){
                        if(!existMap.isEmpty()){
                            for (byte[] existValue : existMap.keySet()){
                                zaddPrivate(key, existMap.get(existValue).score, existValue);
                            }
                        }
                        if(t instanceof RocksDBException){
                            throw t;
                        }
                        throw new IOException(structure + " del key " + key + " bytes " + Arrays.deepToString(bytes) + " failed", t);
                    }
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("del key " + key + " bytes " + Arrays.deepToString(bytes), e));
        }
    }

    @Override
    protected void zdelPrivate(String key, byte[] bytes) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                deleteField(key, keyMetaData, bytes);
            }
        }catch (RocksDBException t){
            throw new IOException(getRocksDBExceptionString("del key " + " bytes " + Arrays.toString(bytes), t));
        }
    }

    @Override
    protected void zdelRangeByRankPrivate(String key, long start, long stop) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                long length = keyMetaData.length;
                if(length <= 0){
                    return;
                }
                if(stop < 0){
                    stop = length + stop;
                }
                if(start > stop){
                    return;
                }
                if(start < 0){
                    start = 0;
                }
                if(start >= length){
                    return;
                }
                byte[] bytes = findBySpan(key, start, keyMetaData);
                if(bytes != null){
                    KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
                    if(keyFieldLevelData != null){
                        start++;
                        deleteField(key, keyMetaData, bytes);
                        while (start <= stop){
                            KeyFieldLevelData theKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, 0);
                            if(theKeyFieldLevelData == null){
                                break;
                            }
                            deleteField(key, Objects.requireNonNull(getKeyMetaData(key)), keyFieldLevelData.nextValues);
                            keyFieldLevelData = theKeyFieldLevelData;
                            start ++;
                        }
                    }
                }

            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("delRange key " + key + " start " + start + " stop " + stop, e));
        }
    }

    @Override
    protected void zdelRangeByScorePrivate(String key, double minScore, double maxScore) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                KeyFieldLevelData keyFieldLevelData = findByScore(key, minScore, keyMetaData);
                KeyFieldLevelData nextKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, 0);
                if(nextKeyFieldLevelData != null){
                    while (nextKeyFieldLevelData.score <= maxScore){
                        deleteField(key, Objects.requireNonNull(getKeyMetaData(key)), keyFieldLevelData.nextValues);
                        keyFieldLevelData = nextKeyFieldLevelData;
                        if(keyFieldLevelData.span != 0 ){
                            nextKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, 0);
                            assert nextKeyFieldLevelData != null;
                        }else {
                            break;
                        }
                    }
                }
            }
        }catch (RocksDBException e){
            throw new IOException(getRocksDBExceptionString("delByScore key " + key + " minScore " + minScore + " maxScore ", e));
        }
    }

    @Override
    protected Double zscorePrivate(String key, byte[] bytes) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
                if(keyFieldLevelData != null){
                    return keyFieldLevelData.score;
                }
            }
        }catch (RocksDBException t){
            throw new IOException(getRocksDBExceptionString("score key " + key + " bytes " + Arrays.toString(bytes), t));
        }
        return super.zscorePrivate(key, bytes);
    }

    @Override
    protected void zexpirePrivate(String key, Long expire) throws IOException {
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData == null){
                throw new IOException(structure + " key " + key + " not exist when expire");
            }
            rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(System.currentTimeMillis() + expire, keyMetaData.version, keyMetaData.length, keyMetaData.maxLevel));
            try {
                deleteAsync(key, keyMetaData);
            }catch (Throwable t){
                rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(keyMetaData.ttl, keyMetaData.version, keyMetaData.length, keyMetaData.maxLevel));
                throw t;
            }
        }catch (RocksDBException t){
            throw new IOException(getRocksDBExceptionString("expire key " + key + " expire " + expire, t));
        }
    }

    @Override
    protected void zdelPrivate(String key) throws IOException {
        ValidateUtils.checkNotNull(key);
        try {
            KeyMetaData keyMetaData = getKeyMetaData(key);
            if(keyMetaData != null){
                rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(KeyMetaData.TTL_DEFAULT, keyMetaData.version + 1, 0, KeyMetaData.LEVEL_DEFAULT));
                try {
                    deleteAsync(key, keyMetaData);
                }catch (Throwable t){
                    rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(keyMetaData.ttl, keyMetaData.version, keyMetaData.length, keyMetaData.maxLevel));
                    if(t instanceof  RocksDBException){
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
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_PREFIX);
            dataOutputStream.writeBytes(key);
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
    private KeyMetaData getKeyMetaData(String key) throws IOException, RocksDBException {
        byte[] valueBytes = rocksDB.get(genKeyMetaDataBytes(key));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            long ttl = dataInputStream.readLong();
            long version = dataInputStream.readLong();
            try (byteArrayInputStream; dataInputStream) {
                if (ttl == KeyMetaData.TTL_DEFAULT || ttl > System.currentTimeMillis()) {
                    long length = dataInputStream.readLong();
                    long maxLevel = dataInputStream.readLong();
                    return new KeyMetaData(ttl, version, length, maxLevel);
                } else {
                    rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(KeyMetaData.TTL_DEFAULT,version + 1, KeyMetaData.LENGTH_DEFAULT, KeyMetaData.LEVEL_DEFAULT));
                    return new KeyMetaData(KeyMetaData.TTL_DEFAULT,version + 1, KeyMetaData.LENGTH_DEFAULT, KeyMetaData.LEVEL_DEFAULT);
                }
            }
        }
        return null;
    }
    private KeyMetaData getKeyMetaDataAll(String key) throws IOException, RocksDBException {
        byte[] valueBytes = rocksDB.get(genKeyMetaDataBytes(key));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream) {
                long ttl = dataInputStream.readLong();
                long version = dataInputStream.readLong();
                long length = dataInputStream.readLong();
                long maxLevel = dataInputStream.readLong();
                return new KeyMetaData(ttl, version, length, maxLevel);
            }
        }
        return null;
    }
    private byte[] genKeyMetaDataBytes(String key) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_PREFIX);
            dataOutputStream.writeBytes(key);
            return byteArrayOutputStream.toByteArray();
        }
    }
    private byte[] genKeyMetaDataValueBytes(long ttl, long version, long length, long maxLevel) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(ttl);
            dataOutputStream.writeLong(version);
            dataOutputStream.writeLong(length);
            dataOutputStream.writeLong(maxLevel);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private KeyFieldLevelData getKeyFieldLevelData(String key, byte[] field, long version, long level) throws IOException, RocksDBException {
        byte[] valueBytes = rocksDB.get(genKeyFieldLevelBytes(key, field, version, level));
        if(valueBytes != null){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(valueBytes);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            try (byteArrayInputStream; dataInputStream){
                double score = dataInputStream.readDouble();
                int nextValuesLength = dataInputStream.readInt();
                byte[] nextValues = new byte[nextValuesLength];
                dataInputStream.readFully(nextValues);
                int downValuesLength = dataInputStream.readInt();
                byte[] downValues = new byte[downValuesLength];
                dataInputStream.readFully(downValues);
                int span = dataInputStream.readInt();
                return new KeyFieldLevelData(nextValues, downValues, span, score);
            }
        }
        return null;
    }
    private byte[] genKeyFieldLevelBytes(String key, byte[] field, long version, long level) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_FIELD_LEVEL);
            dataOutputStream.writeBytes(key);
            dataOutputStream.write(field);
            dataOutputStream.writeLong(version);
            dataOutputStream.writeLong(level);
            return byteArrayOutputStream.toByteArray();
        }
    }
    private byte[] genKeyFieldLevelValueBytes(byte[] nextValues, byte[] downValues, Integer span, double score) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeDouble(score);
            dataOutputStream.writeInt(nextValues.length);
            dataOutputStream.write(nextValues);
            dataOutputStream.writeInt(downValues.length);
            dataOutputStream.write(downValues);
            dataOutputStream.writeInt(span);
            return byteArrayOutputStream.toByteArray();
        }
    }
    private Long getLevel(long length){
        long maxLevel = length >> 4;
        long level = 0;
        double SKIP_P = 0.25;
        while (Math.random() < SKIP_P && level < maxLevel){
            level += 1;
        }
        return level;
    }

    private void deleteAsync(String key, KeyMetaData keyMetaData) throws IOException, RocksDBException {
        ValidateUtils.checkAllNotNull(key, keyMetaData);
        KeyExpireData keyExpireData = getKeyExpireData(key);
        int length = 0;
        byte[] value = null;
        byte[] contactValue = null;
        byte[] valueByes = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.writeLong(keyMetaData.version);
            dataOutputStream.writeLong(keyMetaData.ttl);
            dataOutputStream.writeLong(keyMetaData.maxLevel);
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
    private byte[] genKeyLevelDataHead(String key, long version, long level) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try (byteArrayOutputStream; dataOutputStream){
            dataOutputStream.write(KEY_FIELD_LEVEL_FIRST);
            dataOutputStream.writeBytes(key);
            dataOutputStream.writeLong(version);
            dataOutputStream.writeLong(level);
            return byteArrayOutputStream.toByteArray();
        }
    }
    private byte[] genKeysExpireBytes(String key) throws IOException {
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

    private void deleteField(String key, KeyMetaData keyMetaData, byte[] bytes) throws IOException, RocksDBException {
        KeyFieldLevelData bytesKeyFieldLevelData = getKeyFieldLevelData(key, bytes, keyMetaData.version, 0);
        if(bytesKeyFieldLevelData != null){
            double score = bytesKeyFieldLevelData.score;
            byte[] preBytes = genKeyLevelDataHead(key, keyMetaData.version, keyMetaData.maxLevel);
            KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, preBytes, keyMetaData.version, keyMetaData.maxLevel);
            KeyFieldLevelData preKeyFieldLevelData = keyFieldLevelData;
            assert keyFieldLevelData != null;
            long currentLevel = keyMetaData.maxLevel;
            byte[] sameBytes = null;
            long maxLevel = keyMetaData.maxLevel;
            while (true){
                while (keyFieldLevelData.span != 0){
                    KeyFieldLevelData theKeyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, keyMetaData.version, currentLevel);
                    assert theKeyFieldLevelData != null;
                    if(score == theKeyFieldLevelData.score){
                        if(Arrays.equals(bytes, keyFieldLevelData.nextValues)){
                            int newSpan = 0;
                            if(theKeyFieldLevelData.span != 0){
                                newSpan = preKeyFieldLevelData.span + theKeyFieldLevelData.span - 1;
                                rocksDB.put(genKeyFieldLevelBytes(key, preBytes, keyMetaData.version, currentLevel), genKeyFieldLevelValueBytes(theKeyFieldLevelData.nextValues, preKeyFieldLevelData.downValues, newSpan, preKeyFieldLevelData.score));
                            }else {
                                if((maxLevel == currentLevel) && Arrays.equals(preBytes, genKeyLevelDataHead(key, keyMetaData.version, currentLevel))){
                                    maxLevel--;
                                    rocksDB.delete(genKeyFieldLevelBytes(key, preBytes, keyMetaData.version, currentLevel));
                                }else {
                                    rocksDB.put(genKeyFieldLevelBytes(key, preBytes, keyMetaData.version, currentLevel), genKeyFieldLevelValueBytes(theKeyFieldLevelData.nextValues, preKeyFieldLevelData.downValues, newSpan, preKeyFieldLevelData.score));
                                }
                            }
                            break;
                        }else {
                            if(sameBytes != null && Arrays.equals(sameBytes, keyFieldLevelData.nextValues)){
                                rocksDB.put(genKeyFieldLevelBytes(key, preBytes, keyMetaData.version, currentLevel), genKeyFieldLevelValueBytes(preKeyFieldLevelData.nextValues, preKeyFieldLevelData.downValues, preKeyFieldLevelData.span - 1, preKeyFieldLevelData.score));
                                break;
                            }
                            sameBytes = keyFieldLevelData.nextValues;
                            byte[] thePreBytes = keyFieldLevelData.nextValues;
                            KeyFieldLevelData thePreKeyFieldLevelData = theKeyFieldLevelData;
                            boolean find = false;
                            while (theKeyFieldLevelData.span != 0){
                                KeyFieldLevelData thisKeyFieldLevelData = getKeyFieldLevelData(key, theKeyFieldLevelData.nextValues, keyMetaData.version, currentLevel);
                                assert thisKeyFieldLevelData != null;
                                if(thisKeyFieldLevelData.score == score){
                                    if(Arrays.equals(bytes, theKeyFieldLevelData.nextValues)){
                                        int newSpan = 0;
                                        if(thisKeyFieldLevelData.span != 0){
                                            newSpan = thePreKeyFieldLevelData.span + thisKeyFieldLevelData.span - 1;
                                        }
                                        keyFieldLevelData = theKeyFieldLevelData;
                                        preBytes = thePreBytes;
                                        preKeyFieldLevelData = thePreKeyFieldLevelData;
                                        rocksDB.put(genKeyFieldLevelBytes(key, thePreBytes, keyMetaData.version, currentLevel), genKeyFieldLevelValueBytes(thisKeyFieldLevelData.nextValues, thePreKeyFieldLevelData.downValues, newSpan, thePreKeyFieldLevelData.score));
                                        find = true;
                                        break;
                                    }else {
                                        thePreBytes = theKeyFieldLevelData.nextValues;
                                        thePreKeyFieldLevelData = thisKeyFieldLevelData;
                                        theKeyFieldLevelData = thisKeyFieldLevelData;
                                    }
                                }else {
                                    break;
                                }
                            }
                            if(!find){
                                rocksDB.put(genKeyFieldLevelBytes(key, preBytes, keyMetaData.version, currentLevel), genKeyFieldLevelValueBytes(preKeyFieldLevelData.nextValues, preKeyFieldLevelData.downValues, preKeyFieldLevelData.span - 1, preKeyFieldLevelData.score));
                            }
                            break;
                        }
                    }else if(score > theKeyFieldLevelData.score){
                        preBytes = keyFieldLevelData.nextValues;
                        preKeyFieldLevelData = theKeyFieldLevelData;
                        keyFieldLevelData = theKeyFieldLevelData;
                    }else {
                        rocksDB.put(genKeyFieldLevelBytes(key, preBytes, keyMetaData.version, currentLevel), genKeyFieldLevelValueBytes(preKeyFieldLevelData.nextValues, preKeyFieldLevelData.downValues, preKeyFieldLevelData.span - 1, preKeyFieldLevelData.score));
                        break;
                    }
                }
                currentLevel --;
                if(!Arrays.equals(keyFieldLevelData.downValues, KeyMetaData.BYTES_DEFAULT)){
                    preBytes = keyFieldLevelData.downValues;
                    keyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.downValues, keyMetaData.version, currentLevel);
                    assert keyFieldLevelData != null;
                    preKeyFieldLevelData = keyFieldLevelData;
                }else {
                    break;
                }
            }
            rocksDB.put(genKeyMetaDataBytes(key), genKeyMetaDataValueBytes(keyMetaData.ttl, keyMetaData.version, keyMetaData.length - 1, maxLevel));
        }
    }

    private static class InternalData{
        InternalData(long level){
            this.level = level;
        }
        private long level;
        private int span = 0;
        private byte[] maxBytes = new byte[0];
        private Map<Long, LevelData> levelBytes = new HashMap<>();

        void addLevelData(long index, byte[] bytes){
            LevelData levelData = getLevelData(index);
            if(levelData == null){
                levelData = new LevelData(bytes);
            }
            levelBytes.put(index, levelData);
        }
        LevelData getLevelData(long index){
            return levelBytes.get(index);
        }
        byte[] getBytes(long index){
            LevelData levelData = getLevelData(index);
            if(levelData != null){
                return levelData.bytes;
            }
            return null;
        }
        int getSPan(long index){
            LevelData levelData = getLevelData(index);
            if(levelData != null){
                return levelData.span;
            }
            return 0;
        }
        void addSpan(long index, int span){
            this.span += span;
            for (long i = index - 1; i >= 0 ; i--) {
                LevelData levelData = getLevelData(i);
                if(levelData != null){
                    levelData.span += span;
                }
            }
        }
    }
    private static class LevelData{
        private int span = 0;
        private byte[] bytes;
        LevelData(byte[] bytes){
            this.bytes = bytes;
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
                                            long maxLevel = dataInputStream.readLong();
                                            KeyMetaData keyMataData = getKeyMetaDataAll(key);
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
                                                LoggerHelper.getLogger().info(structure + " key: " + key + ",meta version: " + (keyMataData == null ? null : keyMataData.version) + ",will delete version: " + version + ", meta ttl: " + (keyMataData == null ? null : keyMataData.ttl) + ", will delete ttl: " + ttl);
                                                for (long j = maxLevel; j >= 0 ; j--) {
                                                    try {
                                                        byte[] preBytes = genKeyLevelDataHead(key, version, j);
                                                        KeyFieldLevelData keyFieldLevelData = getKeyFieldLevelData(key, preBytes, version, j);
                                                        while (keyFieldLevelData != null){
                                                            rocksDB.delete(genKeyFieldLevelBytes(key, preBytes, version, j));
                                                            preBytes = keyFieldLevelData.nextValues;
                                                            keyFieldLevelData = getKeyFieldLevelData(key, keyFieldLevelData.nextValues, version, j);
                                                        }
                                                    }catch (Throwable t){
                                                        LoggerHelper.getLogger().error(structure + " clear keyFieldLevelData failed, key: " + key + ",version: " + version + ",level: " + j);
                                                    }
                                                }

                                            }
                                            if(reAdd){
                                                map.put("version", version);
                                                map.put("ttl", ttl);
                                                map.put("maxLevel", maxLevel);
                                                cantDeleteList.add(map);
                                            }
                                        }
                                    }
                                    if(!cantDeleteList.isEmpty()){
                                        dataOutputStream.writeInt(cantDeleteList.size());
                                        for (Map map : cantDeleteList){
                                            dataOutputStream.writeLong((long)map.get("version"));
                                            dataOutputStream.writeLong((long)map.get("ttl"));
                                            dataOutputStream.writeLong((long)map.get("maxLevel"));
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
