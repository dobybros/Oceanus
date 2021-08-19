package core.storage.adapters.assist.impl.lock;

import core.common.InternalTools;
import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.structure.HashOperation;
import org.checkerframework.checker.units.qual.K;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/15.
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            setPrivate(key, field, bytes);
        }finally {
            lock.writeLock().unlock();
        }

    }

//    @Override
//    public void setEx(String key, String field, byte[] bytes, Long expire) throws IOException {
//        ReadWriteLock lock = getLock(key);
//        try {
//            lock.writeLock().lock();
//            setExPrivate(key, field, bytes, expire);
//        }finally {
//            lock.writeLock().unlock();
//        }
//    }

    @Override
    public byte[] setIfAbsent(String key, String field, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return setIfAbsentPrivate(key, field, bytes);
        }finally {
            lock.writeLock().unlock();
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            delPrivate(key);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void del(String key, String field) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            delPrivate(key, field);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void del(String key, String... fields) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            delPrivate(key, fields);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void mSet(String key, Map<String, byte[]> tMap) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            mSetPrivate(key, tMap);
        }finally {
            lock.writeLock().unlock();
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            expirePrivate(key, expire);
        }finally {
            lock.writeLock().unlock();
        }
    }
//
//    @Override
//    public void expire(String key, Long expire, String... fields) throws IOException {
//
//    }
}
