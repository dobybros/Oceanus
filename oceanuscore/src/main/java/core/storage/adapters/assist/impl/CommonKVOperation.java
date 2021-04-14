package core.storage.adapters.assist.impl;

import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.structure.KVOperation;
import core.utils.ValidateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/30.
 * Description：
 */
public class CommonKVOperation extends CommonOperation implements KVOperation {
    protected final String structure = "KV";

    @Override
    public void set(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            kvsetPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void set(String key, byte[] bytes, Long expire) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            kvsetExPrivate(key, bytes, expire);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public byte[] setIfAbsent(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return kvsetIfAbsentPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Integer setIfAbsent(String key, byte[] bytes, Long expire) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return kvsetIfAbsentPrivate(key, bytes, expire);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void mset(Map<String, byte[]> bytesMap) throws IOException {
        ValidateUtils.checkNotNull(bytesMap);
        List<String> addSuccessList = new ArrayList<>();
        Map<String, byte[]> existsMap = new HashMap<>();
        for (String key : bytesMap.keySet()){
            try {
                byte[] bytes = get(key);
                if(bytes != null){
                    existsMap.put(key, bytes);
                }
                set(key, bytesMap.get(key));
                addSuccessList.add(key);
            }catch (Throwable t){
                if(!addSuccessList.isEmpty()){
                    del(addSuccessList.toArray(new String[0]));
                    if(!existsMap.isEmpty()){
                        for (String existKey : existsMap.keySet()){
                            if(addSuccessList.contains(existKey)){
                                set(existKey, existsMap.get(existKey));
                            }
                        }
                    }
                }
                throw new IOException(structure + " set key " + key + " failed", t);
            }
        }
    }

    @Override
    public byte[] getSet(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            byte[] oldBytes = kvgetPrivate(key);
            if(oldBytes != null){
                kvsetPrivate(key, bytes);
                return oldBytes;
            }
            return null;
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public byte[] append(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return kvappendPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<byte[]> mget(String... keys) throws IOException {
        ValidateUtils.checkNotNull(keys);
        List<byte[]> bytes = new ArrayList<>();
        for (String key : keys){
            byte[] valueBytes = get(key);
            if(valueBytes != null){
                bytes.add(valueBytes);
            }
        }
        if(!bytes.isEmpty()){
            return bytes;
        }
        return null;
    }

    @Override
    public byte[] get(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return kvgetPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Integer len(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.readLock().lock();
            return kvlenPrivate(key);
        }finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void expire(String key, Long expire) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            kvexpirePrivate(key, expire);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void del(String... keys) throws IOException {
        kvdel(keys);
    }

    @Override
    public void del(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            kvdelPrivate(key);
        }finally {
            lock.writeLock().unlock();
        }
    }
}
