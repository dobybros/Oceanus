package core.storage.adapters.assist.impl.queue;

import chat.utils.SingleThreadQueueEx;
import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.assist.impl.queue.data.StorageFuture;
import core.storage.adapters.structure.SetOperation;
import core.utils.thread.ThreadPoolFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/27.
 * Description：
 */
public class CommonSetOperation extends CommonOperation implements SetOperation {
    protected final String structure = "SET";

    @Override
    public Long add(String key, byte[]... bytes) throws IOException {
        StorageFuture storageFuture = new SetStorageFuture(key, bytes, SetStorageFuture.OPTION_ADD, null);
        addTask(key, storageFuture);
        try {
            return (Long) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " add key " + key + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public Set<byte[]> get(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return sgetPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Long len(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return slenPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Boolean isMember(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return isMemberPrivate(key, bytes);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public byte[] move(String sourceKey, String destinationKey, byte[] bytes) throws IOException {
        del(sourceKey, bytes);
        add(destinationKey, bytes);
        return bytes;
    }

    @Override
    public void del(String key, byte[]... bytes) throws IOException{
        StorageFuture storageFuture = new SetStorageFuture(key, bytes, SetStorageFuture.OPTION_DELKEYVALUES, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " del key " + key + " bytes " + Arrays.deepToString(bytes), e);
        }
    }

    @Override
    public void del(String key, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new SetStorageFuture(key, bytes, SetStorageFuture.OPTION_DELKEYVALUE, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " del key " + key + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public void expire(String key, Long expire) throws IOException {
        StorageFuture storageFuture = new SetStorageFuture(key, null, SetStorageFuture.OPTION_EXPIRE, expire);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " expire key " + key, e);
        }
    }

    @Override
    public void del(String key) throws IOException {
        StorageFuture storageFuture = new SetStorageFuture(key, null, SetStorageFuture.OPTION_DEL, null);
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
                        SetStorageFuture theStorageFuture = null;
                        try {
                            theStorageFuture = (SetStorageFuture) o;
                            ThreadPoolFactory.getInstance().longAdder.increment();
//                            if(ThreadPoolFactory.getInstance().longAdder.longValue() == 1000000){
//                                System.out.println(System.currentTimeMillis() - ThreadPoolFactory.getInstance().time);
//                            }
                            ReadWriteLock lock = getLock(theStorageFuture.key);
                            try {
                                lock.writeLock().lock();
                                Long length = null;
                                switch (theStorageFuture.option){
                                    case SetStorageFuture.OPTION_ADD:
                                        length = addPrivate(theStorageFuture.key, (byte[][]) theStorageFuture.value);
                                        break;
                                    case SetStorageFuture.OPTION_DELKEYVALUE:
                                        sdelPrivate(theStorageFuture.key, (byte[]) theStorageFuture.value);
                                        break;
                                    case SetStorageFuture.OPTION_DELKEYVALUES:
                                        sdelPrivate(theStorageFuture.key, (byte[][]) theStorageFuture.value);
                                        break;
                                    case SetStorageFuture.OPTION_DEL:
                                        sdelPrivate(theStorageFuture.key);
                                        break;
                                    case SetStorageFuture.OPTION_EXPIRE:
                                        sexpirePrivate(theStorageFuture.key, theStorageFuture.expire);
                                        break;
                                }
                                theStorageFuture.complete(length);
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
                    ((SetStorageFuture) o).completeExceptionally(e);
                }
            });
            SingleThreadQueueEx<StorageFuture> oldSingleThreadQueue = concurrentLinkedQueueMap.putIfAbsent(byteBuffer, singleThreadQueue);
            if(oldSingleThreadQueue != null){
                singleThreadQueue = oldSingleThreadQueue;
            }
        }
        singleThreadQueue.offerAndStart(storageFuture);
    }
    private static class SetStorageFuture<T> extends StorageFuture{
        SetStorageFuture(String key, T value, Integer option, Long expire) {
            super(key, value, option, expire);
        }
        private static final int OPTION_ADD = 0;
        private static final int OPTION_DELKEYVALUE = 1;
        private static final int OPTION_DELKEYVALUES = 2;
        private static final int OPTION_EXPIRE = 3;
        private static final int OPTION_DEL = 4;
    }
}
