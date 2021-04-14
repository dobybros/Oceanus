package core.storage.adapters.assist.impl.lock;

import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.structure.SetOperation;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/27.
 * Description：
 */
public class CommonSetOperation extends CommonOperation implements SetOperation {
    protected final String structure = "SET";

    @Override
    public Long add(String key, byte[]... bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return addPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            sdelPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void del(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            sdelPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void expire(String key, Long expire) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            sexpirePrivate(key, expire);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void del(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            sdelPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }
}
