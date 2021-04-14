package core.storage.adapters.assist.impl.queue;

import chat.utils.SingleThreadQueueEx;
import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.assist.impl.queue.data.StorageFuture;
import core.storage.adapters.structure.HashOperation;
import core.utils.thread.ThreadPoolFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/11/16.
 * Description：
 */
public class CommonHashOperation extends CommonOperation implements HashOperation {
    protected final String structure = "HASH";

    @Override
    public byte[] get(String key, String field) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return getPrivate(key, field);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<String, byte[]> getAll(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return getAllPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void set(String key, String field, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new HashStorageFuture(key, field, bytes, HashStorageFuture.OPTION_SET, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " get key " + key + " field " + field + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public byte[] setIfAbsent(String key, String field, byte[] bytes) throws IOException {
        StorageFuture storageFuture = new HashStorageFuture(key, field, bytes, HashStorageFuture.OPTION_SETIFABSENT, null);
        addTask(key, storageFuture);
        try {
            return (byte[]) storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " setIfAbsent key " + key + " field " + field + " bytes " + Arrays.toString(bytes), e);
        }
    }

    @Override
    public Long len(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return lenPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void del(String key) throws IOException {
        StorageFuture storageFuture = new HashStorageFuture(key, null, null, HashStorageFuture.OPTION_DEL, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + "del key " + key, e);
        }
    }

    @Override
    public void del(String key, String field) throws IOException {
        StorageFuture storageFuture = new HashStorageFuture(key, field, null, HashStorageFuture.OPTION_DELKEYFIELD, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " del key " + key + " field " + field, e);
        }
    }

    @Override
    public void del(String key, String... fields) throws IOException {
        StorageFuture storageFuture = new HashStorageFuture(key, fields, HashStorageFuture.OPTION_DELKEYFIELDS, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " del key " + key + " fields " + Arrays.toString(fields), e);
        }
    }

    @Override
    public void mSet(String key, Map<String, byte[]> tMap) throws IOException {
        StorageFuture storageFuture = new HashStorageFuture(key, null, tMap, HashStorageFuture.OPTION_MSET, null);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " mSet key " + key + " tMap " +tMap, e);
        }
    }

    @Override
    public List<byte[]> mGet(String key, String... fields) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return mGetPrivate(key, fields);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void expire(String key, Long expire) throws IOException {
        StorageFuture storageFuture = new HashStorageFuture(key, null, null, HashStorageFuture.OPTION_EXPIRE, expire);
        addTask(key, storageFuture);
        try {
            storageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(structure + " expire key " + key + " expire " + expire, e);
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
                        HashStorageFuture theStorageFuture = null;
                        try {
                            theStorageFuture = (HashStorageFuture) o;
                            ThreadPoolFactory.getInstance().longAdder.increment();
                            if(ThreadPoolFactory.getInstance().longAdder.longValue() == 1000000){
                                System.out.println(System.currentTimeMillis() - ThreadPoolFactory.getInstance().time);
                            }
                            ReadWriteLock lock = getLock(theStorageFuture.key);
                            try {
                                lock.writeLock().lock();
                                byte[] bytes = null;
                                switch (theStorageFuture.option){
                                    case HashStorageFuture.OPTION_SET:
                                        setPrivate(theStorageFuture.key, theStorageFuture.field, (byte[]) theStorageFuture.value);
                                        break;
                                    case HashStorageFuture.OPTION_SETIFABSENT:
                                        bytes = setIfAbsentPrivate(theStorageFuture.key, theStorageFuture.field, (byte[]) theStorageFuture.value);
                                        break;
                                    case HashStorageFuture.OPTION_MSET:
                                        mSetPrivate(theStorageFuture.key, (Map<String, byte[]>) theStorageFuture.value);
                                        break;
                                    case HashStorageFuture.OPTION_DELKEYFIELD:
                                        delPrivate(theStorageFuture.key, theStorageFuture.field);
                                        break;
                                    case HashStorageFuture.OPTION_DELKEYFIELDS:
                                        delPrivate(theStorageFuture.key, theStorageFuture.fields);
                                        break;
                                    case HashStorageFuture.OPTION_DEL:
                                        delPrivate(theStorageFuture.key);
                                        break;
                                    case HashStorageFuture.OPTION_EXPIRE:
                                        expirePrivate(theStorageFuture.key, theStorageFuture.expire);
                                        break;
                                }
                                theStorageFuture.complete(bytes);
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
                    ((HashStorageFuture) o).completeExceptionally(e);
                }
            });
            SingleThreadQueueEx<StorageFuture> oldSingleThreadQueue = concurrentLinkedQueueMap.putIfAbsent(byteBuffer, singleThreadQueue);
            if(oldSingleThreadQueue != null){
                singleThreadQueue = oldSingleThreadQueue;
            }
        }
        singleThreadQueue.offerAndStart(storageFuture);
    }

    private static class HashStorageFuture<T> extends StorageFuture{
        HashStorageFuture(String key, String field, T value, int option, Long expire){
            super(key, value, option, expire);
            this.field = field;
        }
        HashStorageFuture(String key, String[] fields, int option, Long expire){
            super(key, null, option, expire);
            this.fields = fields;
        }
        private String field;

        private String[] fields;

        private static final int OPTION_SET = 0;
        private static final int OPTION_MSET = 1;
        private static final int OPTION_SETIFABSENT = 2;
        private static final int OPTION_DELKEYFIELD = 3;
        private static final int OPTION_DELKEYFIELDS = 4;
        private static final int OPTION_EXPIRE = 5;
        private static final int OPTION_DEL = 6;
    }
}
