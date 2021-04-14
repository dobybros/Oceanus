package core.storage.adapters.assist.impl.queue;

import chat.utils.SingleThreadQueueEx;
import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.assist.impl.queue.data.StorageFuture;
import core.storage.adapters.structure.ListOperation;
import core.utils.thread.ThreadPoolFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/26.
 * Description：
 */
public class CommonListOperation extends CommonOperation implements ListOperation {
    protected final String structure = "LIST";

    @Override
    public Long len(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return llenPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public byte[] lpop(String key) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, null, ListStorageFuture.OPTION_LPOP, null);
        addTask(key, storageFuture);
        try {
            return (byte[]) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " lpop key " + key, e);
        }
    }

    @Override
    public byte[] rpop(String key) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, null, ListStorageFuture.OPTION_RPOP, null);
        addTask(key, storageFuture);
        try {
            return (byte[]) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " rpop key " + key, e);
        }
    }

    @Override
    public Long lpush(String key, byte[]... bytes) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, bytes, ListStorageFuture.OPTION_LPUSH, null);
        addTask(key, storageFuture);
        try {
            return (Long) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " lpush key " + key + " bytes " + Arrays.deepToString(bytes), e);
        }
    }

    @Override
    public Long lpushIfExists(String key, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, bytes, ListStorageFuture.OPTION_LPUSHIFEXISTS, null);
        addTask(key, storageFuture);
        try {
            return (Long) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " lpushIfExists key " + key + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public Long rpush(String key, byte[]... bytes) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, bytes, ListStorageFuture.OPTION_RPUSH, null);
        addTask(key, storageFuture);
        try {
            return (Long) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " rpush key " + key + " bytes " + Arrays.deepToString(bytes), e);
        }
    }

    @Override
    public Long rpushIfExists(String key, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, bytes, ListStorageFuture.OPTION_RPUSHIFEXISTS, null);
        addTask(key, storageFuture);
        try {
            return (Long) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " rpushIfExists key " + key + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public List<byte[]> get(String key, long start, long stop) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return getPrivate(key, start, stop);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public byte[] rpopLpush(String sourceKey, String destinationKey) throws IOException {
        byte[] bytes = rpop(sourceKey);
        if(bytes != null){
            lpush(destinationKey, bytes);
        }
        return bytes;
    }

    @Override
    public void expire(String key, Long expire) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, null, ListStorageFuture.OPTION_EXPIRE, expire);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " expire key " + key + " expire " + expire, e);
        }
    }

    @Override
    public void del(String key) throws IOException {
        StorageFuture storageFuture = new ListStorageFuture(key, null, ListStorageFuture.OPTION_DEL, null);
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
                    {
                        ListStorageFuture theStorageFuture = null;
                        try {
                            theStorageFuture = (ListStorageFuture) o;
                            ThreadPoolFactory.getInstance().longAdder.increment();
                            if(ThreadPoolFactory.getInstance().longAdder.longValue() == 1000000){
                                System.out.println(System.currentTimeMillis() - ThreadPoolFactory.getInstance().time);
                            }
                            ReadWriteLock lock = getLock(theStorageFuture.key);
                            try {
                                lock.writeLock().lock();
                                Object result = null;
                                switch (theStorageFuture.option){
                                    case ListStorageFuture.OPTION_LPOP:
                                        result = lpopPrivate(storageFuture.key);
                                        break;
                                    case ListStorageFuture.OPTION_LPUSH:
                                        result = lpushPrivate(theStorageFuture.key, (byte[][]) theStorageFuture.value);
                                        break;
                                    case ListStorageFuture.OPTION_LPUSHIFEXISTS:
                                        result = lpushIfExistsPrivate(theStorageFuture.key, (byte[]) theStorageFuture.value);
                                        break;
                                    case ListStorageFuture.OPTION_RPOP:
                                        result = rpopPrivate(storageFuture.key);
                                        break;
                                    case ListStorageFuture.OPTION_RPUSH:
                                        result = rpushPrivate(theStorageFuture.key, (byte[][]) theStorageFuture.value);
                                        break;
                                    case ListStorageFuture.OPTION_RPUSHIFEXISTS:
                                        result = rpushIfExistsPrivate(theStorageFuture.key, (byte[]) theStorageFuture.value);
                                        break;
                                    case ListStorageFuture.OPTION_DEL:
                                        lDelPrivate(theStorageFuture.key);
                                        break;
                                    case ListStorageFuture.OPTION_EXPIRE:
                                        lExpirePrivate(theStorageFuture.key, theStorageFuture.expire);
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
                }

                @Override
                public void error(Object o, Throwable e) {
                    ((ListStorageFuture) o).completeExceptionally(e);
                }
            });
            SingleThreadQueueEx<StorageFuture> oldSingleThreadQueue = concurrentLinkedQueueMap.putIfAbsent(byteBuffer, singleThreadQueue);
            if(oldSingleThreadQueue != null){
                singleThreadQueue = oldSingleThreadQueue;
            }
        }
        singleThreadQueue.offerAndStart(storageFuture);
    }
    private static class ListStorageFuture<T> extends StorageFuture{
        ListStorageFuture(String key, T value, Integer option, Long expire) {
            super(key, value, option, expire);
        }
        private static final int OPTION_LPOP = 0;
        private static final int OPTION_LPUSH = 1;
        private static final int OPTION_LPUSHIFEXISTS = 2;
        private static final int OPTION_RPOP = 3;
        private static final int OPTION_RPUSH = 4;
        private static final int OPTION_RPUSHIFEXISTS = 5;
        private static final int OPTION_EXPIRE = 6;
        private static final int OPTION_DEL = 7;
    }
}
