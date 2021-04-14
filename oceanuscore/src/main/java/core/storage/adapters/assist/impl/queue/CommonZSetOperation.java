package core.storage.adapters.assist.impl.queue;

import chat.utils.SingleThreadQueueEx;
import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.assist.impl.queue.data.StorageFuture;
import core.storage.adapters.data.zset.Tuple;
import core.storage.adapters.structure.ZSetOperation;
import core.utils.thread.ThreadPoolFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/28.
 * Description：
 */
public class CommonZSetOperation extends CommonOperation implements ZSetOperation {
    protected final String structure = "ZSET";

    @Override
    public Long add(String key, double score, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, bytes, score, ZSetStorageFuture.OPTION_ADD);
        addTask(key, storageFuture);
        try {
            return (Long) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " add key " + key + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public Long add(String key, Map<byte[], Double> bytesScores) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, bytesScores, ZSetStorageFuture.OPTION_ADDMULTIPLE, null);
        addTask(key, storageFuture);
        try {
            return (Long) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " add key " + key + " bytes " + bytesScores, e);
        }
    }

    @Override
    public Long len(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return zlenPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Long len(String key, double minScore, double maxScore) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return zlenPrivate(key, minScore, maxScore);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Double incrby(String key, double incrScore, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, bytes, incrScore, ZSetStorageFuture.OPTION_INCRBY);
        addTask(key, storageFuture);
        try {
            return (Double) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " incrby key " + key + " bytes " + Arrays.toString(bytes) + " incrScore " + incrScore, e);
        }
    }

    @Override
    public Set<Tuple> get(String key, long start, long stop) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return zgetPrivate(key, start, stop);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<Tuple> getByScore(String key, double minScore, double maxScore) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return zgetByScorePrivate(key, minScore, maxScore);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Long rank(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return zrankPrivate(key, bytes);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void del(String key, byte[]... bytes) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, bytes, ZSetStorageFuture.OPTION_DELVALUES, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " del key " + key + " bytes " + Arrays.deepToString(bytes), e);
        }
    }

    @Override
    public void del(String key, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, bytes, ZSetStorageFuture.OPTION_DELVALUE, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " del key " + key + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public void delRangeByRank(String key, long start, long stop) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, start, stop, ZSetStorageFuture.OPTION_DELRANGEBYRANK);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " delRangeByRank key " + key + " start " + start + " stop " + stop, e);
        }
    }

    @Override
    public void delRangeByScore(String key, double minScore, double maxScore) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, minScore, maxScore, ZSetStorageFuture.OPTION_DELRANGEBYSCORE);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " delRangeByScore key " + key + " minScore " + minScore + " maxScore " + maxScore, e);
        }
    }

    @Override
    public Double score(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return zscorePrivate(key, bytes);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void expire(String key, Long expire) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, null, ZSetStorageFuture.OPTION_EXPIRE, expire);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " expire key " + key + " expire " + expire, e);
        }
    }

    @Override
    public void del(String key) throws IOException {
        StorageFuture storageFuture = new ZSetStorageFuture(key, null, ZSetStorageFuture.OPTION_DEL, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " del key " + key, e);
        }
    }
    @Override
    protected void addTask(String key, StorageFuture storageFuture) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(generateLockKey(key));
        SingleThreadQueueEx<StorageFuture> singleThreadQueue = concurrentLinkedQueueMap.get(byteBuffer);
        if(singleThreadQueue == null){
            singleThreadQueue = new SingleThreadQueueEx<>(ThreadPoolFactory.getInstance().getStorageThreadPool(), new SingleThreadQueueEx.Handler() {
                @Override
                public void execute(Object o) {
                    ZSetStorageFuture theStorageFuture = null;
                    try {
                        theStorageFuture = (ZSetStorageFuture) o;
                        ThreadPoolFactory.getInstance().longAdder.increment();
                        if(ThreadPoolFactory.getInstance().longAdder.longValue() == 1000000){
                            System.out.println(System.currentTimeMillis() - ThreadPoolFactory.getInstance().time);
                        }
                        ReadWriteLock lock = getLock(theStorageFuture.key);
                        try {
                            lock.writeLock().lock();
                            Object result = null;
                            switch (theStorageFuture.option){
                                case ZSetStorageFuture.OPTION_ADD:
                                    result = zaddPrivate(theStorageFuture.key, theStorageFuture.score, (byte[]) theStorageFuture.value);
                                    break;
                                case ZSetStorageFuture.OPTION_ADDMULTIPLE:
                                    result = zaddPrivate(theStorageFuture.key, (Map<byte[], Double>) theStorageFuture.value);
                                    break;
                                case ZSetStorageFuture.OPTION_INCRBY:
                                    result = zincrbyPrivate(key, theStorageFuture.score, (byte[]) theStorageFuture.value);
                                    break;
                                case ZSetStorageFuture.OPTION_DELVALUE:
                                    zdelPrivate(theStorageFuture.key, (byte[]) theStorageFuture.value);
                                    break;
                                case ZSetStorageFuture.OPTION_DELVALUES:
                                    zdelPrivate(theStorageFuture.key, (byte[][]) theStorageFuture.value);
                                    break;
                                case ZSetStorageFuture.OPTION_DELRANGEBYRANK:
                                    zdelRangeByRankPrivate(theStorageFuture.key, theStorageFuture.start, theStorageFuture.end);
                                    break;
                                case ZSetStorageFuture.OPTION_DELRANGEBYSCORE:
                                    zdelRangeByScorePrivate(theStorageFuture.key, theStorageFuture.minScore, theStorageFuture.maxScore);
                                    break;
                                case ZSetStorageFuture.OPTION_DEL:
                                    zdelPrivate(theStorageFuture.key);
                                    break;
                                case ZSetStorageFuture.OPTION_EXPIRE:
                                    zexpirePrivate(theStorageFuture.key, theStorageFuture.expire);
                                    break;
                            }
                            theStorageFuture.complete(result);
                        }finally {
                            lock.writeLock().unlock();
                        }
                    } catch (IOException e) {
                        theStorageFuture.completeExceptionally(e);
                    }
                }

                @Override
                public void error(Object o, Throwable e) {
                    ((ZSetStorageFuture) o).completeExceptionally(e);
                }
            });
            SingleThreadQueueEx<StorageFuture> oldSingleThreadQueue = concurrentLinkedQueueMap.putIfAbsent(byteBuffer, singleThreadQueue);
            if(oldSingleThreadQueue != null){
                singleThreadQueue = oldSingleThreadQueue;
            }
        }
        singleThreadQueue.offerAndStart(storageFuture);
    }
    private static class ZSetStorageFuture<T> extends StorageFuture{
        ZSetStorageFuture(String key, T value, Integer option, Long expire) {
            super(key, value, option, expire);
        }
        ZSetStorageFuture(String key, T value, Double score, Integer option) {
            super(key, value, option, null);
            this.score = score;
        }
        ZSetStorageFuture(String key, Long start, Long end, Integer option) {
            super(key, null, option, null);
            this.start = start;
            this.end = end;
        }
        ZSetStorageFuture(String key, Double minScore, Double maxScore, Integer option) {
            super(key, null, option, null);
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
        private Double score;
        private Long start;
        private Long end;
        private Double minScore;
        private Double maxScore;
        private static final int OPTION_ADD = 0;
        private static final int OPTION_ADDMULTIPLE = 1;
        private static final int OPTION_INCRBY = 2;
        private static final int OPTION_DELVALUE = 3;
        private static final int OPTION_DELVALUES = 4;
        private static final int OPTION_DELRANGEBYRANK = 5;
        private static final int OPTION_DELRANGEBYSCORE = 6;
        private static final int OPTION_EXPIRE = 7;
        private static final int OPTION_DEL = 8;
    }
}
