package core.storage.adapters.assist;

import chat.utils.SingleThreadQueueEx;
import core.storage.adapters.assist.impl.queue.data.StorageFuture;
import core.storage.adapters.data.zset.Tuple;
import core.utils.thread.ThreadFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by lick on 2020/10/15.
 * Description：
 */
public abstract class CommonOperation {
    private Map<ByteBuffer, ReadWriteLock> readWriteLockMap = new ConcurrentHashMap<>();
    //kv
    protected void kvsetPrivate(String key, byte[] bytes) throws IOException{};

    protected void kvsetExPrivate(String key, byte[] bytes, Long expire) throws IOException{};

    protected byte[] kvsetIfAbsentPrivate(String key, byte[] bytes) throws IOException{return null;};

    protected Integer kvsetIfAbsentPrivate(String key, byte[] bytes, Long expire) throws IOException{return 0;};

    protected byte[] kvappendPrivate(String key, byte[] bytes) throws IOException{return null;};

    protected byte[] kvgetPrivate(String key) throws IOException{return null;};

    protected Integer kvlenPrivate(String key) throws IOException{return 0;};

    protected void kvexpirePrivate(String key, Long expire)throws IOException{};

    protected void kvdel(String... keys) throws IOException{};

    protected void kvdelPrivate(String key)throws IOException{};
    //hash
    protected byte[] getPrivate(String key, String field)throws IOException{return null;};

    protected Map<String, byte[]> getAllPrivate(String key)throws IOException{return null;};

    protected  void setPrivate(String key, String field, byte[] bytes)throws IOException{};

//    protected void setExPrivate(String key, String field, byte[] bytes, Long expire)throws IOException{};
    
    protected byte[] setIfAbsentPrivate(String key, String field, byte[] bytes)throws IOException{return null;};

    protected Long lenPrivate(String key)throws IOException{return 0L;};

    protected void delPrivate(String key)throws IOException{};

    protected void delPrivate(String key, String field)throws IOException{};

    protected void delPrivate(String key, String... fields) throws IOException{}

    protected void mSetPrivate(String key, Map<String, byte[]> tMap)throws IOException{};

    protected List<byte[]> mGetPrivate(String key, String... fields)throws IOException{return null;};

    protected void expirePrivate(String key, Long expire)throws IOException{};
    
//    protected void expirePrivate(String key, Long expire, String... fields)throws IOException{};
    //list
    protected Long llenPrivate(String key) throws IOException{return 0L;};

    protected byte[] lpopPrivate(String key) throws IOException{return null;};

    protected byte[] rpopPrivate(String key) throws IOException{return null;};

    protected Long lpushPrivate(String key, byte[]... bytes) throws IOException{return null;};

    protected Long lpushIfExistsPrivate(String key, byte[] bytes) throws IOException{return null;};

    protected Long rpushPrivate(String key, byte[]... bytes) throws IOException{return null;};

    protected Long rpushIfExistsPrivate(String key, byte[] bytes) throws IOException{return null;};

    protected List<byte[]> getPrivate(String key, long start, long stop) throws IOException{return null;};

    protected void lExpirePrivate(String key, Long expire)throws IOException{};

    protected void lDelPrivate(String key)throws IOException{};
    //set
    protected Long addPrivate(String key, byte[]... bytes) throws IOException{return null;};

    protected Set<byte[]> sgetPrivate(String key) throws IOException{return null;};

    protected Long slenPrivate(String key) throws IOException{return 0L;};

    protected Boolean isMemberPrivate(String key, byte[] bytes) throws IOException{return false;};

    protected void sdelPrivate(String key, byte[]... bytes) throws IOException{};

    protected void sdelPrivate(String key, byte[] bytes) throws IOException{};

    protected void sexpirePrivate(String key, Long expire)throws IOException{};

    protected void sdelPrivate(String key)throws IOException{};
    //zset
    protected Long zaddPrivate(String key, double score, byte[] bytes) throws IOException{return null;};

    protected Long zaddPrivate(String key, Map<byte[], Double> bytesScores) throws IOException{return null;};

    protected Long zlenPrivate(String key) throws IOException{return 0L;};

    protected Long zlenPrivate(String key, double minScore, double maxScore) throws IOException{return 0L;};

    protected Double zincrbyPrivate(String key, double incrScore, byte[] bytes) throws IOException{return null;};

    protected Set<Tuple> zgetPrivate(String key, long start, long stop) throws IOException{return null;};

    protected Set<Tuple> zgetByScorePrivate(String key, double minScore, double maxScore) throws IOException{return null;};

    protected Long zrankPrivate(String key, byte[] bytes) throws IOException{return null;};

    protected void zdelPrivate(String key, byte[]... bytes) throws IOException{};

    protected void zdelPrivate(String key, byte[] bytes) throws IOException{};

    protected void zdelRangeByRankPrivate(String key, long start, long stop) throws IOException{};

    protected void zdelRangeByScorePrivate(String key, double minScore, double maxScore) throws IOException{};

    protected Double zscorePrivate(String key, byte[] bytes) throws IOException{return null;};

    protected void zexpirePrivate(String key, Long expire)throws IOException{};

    protected void zdelPrivate(String key)throws IOException{};

    protected byte[] generateLockKey(String key)throws IOException{return null;};
    protected ReadWriteLock getLock(String key)throws IOException{
        byte[] keyBytes = generateLockKey(key);
        ByteBuffer byteBuffer = ByteBuffer.wrap(keyBytes);
        return getLock(byteBuffer);
    }
    protected ReadWriteLock getLock(ByteBuffer byteBuffer)throws IOException{
        ReadWriteLock readWriteLock = readWriteLockMap.get(byteBuffer);
        if(readWriteLock == null){
            readWriteLock = new ReentrantReadWriteLock(true);
            ReadWriteLock old = readWriteLockMap.putIfAbsent(byteBuffer, readWriteLock);
            if(old != null){
                readWriteLock = old;
            }
        }
        return readWriteLock;
    }
    protected Map<ByteBuffer, SingleThreadQueueEx<StorageFuture>> concurrentLinkedQueueMap = new ConcurrentHashMap<>();

    protected void addTask(String key, StorageFuture storageFuture) throws IOException {}

    protected void removeQueue(ByteBuffer byteBuffer) {
        concurrentLinkedQueueMap.remove(byteBuffer);
    }
}
