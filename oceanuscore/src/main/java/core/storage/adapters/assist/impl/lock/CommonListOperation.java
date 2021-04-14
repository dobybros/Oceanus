package core.storage.adapters.assist.impl.lock;

import core.common.InternalTools;
import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.structure.ListOperation;

import java.io.IOException;
import java.util.List;
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return lpopPrivate(key);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public byte[] rpop(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return rpopPrivate(key);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Long lpush(String key, byte[]... bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return lpushPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Long lpushIfExists(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return lpushIfExistsPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Long rpush(String key, byte[]... bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return rpushPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Long rpushIfExists(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return rpushIfExistsPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            lExpirePrivate(key, expire);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void del(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            lDelPrivate(key);
        }finally {
            lock.writeLock().unlock();
        }
    }
}
