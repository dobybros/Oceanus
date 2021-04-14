package core.storage.adapters.assist.impl.lock;

import core.common.InternalTools;
import core.storage.adapters.data.zset.Tuple;
import core.storage.adapters.assist.CommonOperation;
import core.storage.adapters.structure.ZSetOperation;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by lick on 2020/10/28.
 * Description：
 */
public class CommonZSetOperation extends CommonOperation implements ZSetOperation {
    protected final String structure = "ZSET";

    @Override
    public Long add(String key, double score, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return zaddPrivate(key, score, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Long add(String key, Map<byte[], Double> bytesScores) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return zaddPrivate(key, bytesScores);
        }finally {
            lock.writeLock().unlock();
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            return zincrbyPrivate(key, incrScore, bytes);
        }finally {
            lock.writeLock().unlock();
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            zdelPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void del(String key, byte[] bytes) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            zdelPrivate(key, bytes);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delRangeByRank(String key, long start, long stop) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            zdelRangeByRankPrivate(key, start, stop);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delRangeByScore(String key, double minScore, double maxScore) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            zdelRangeByScorePrivate(key, minScore, maxScore);
        }finally {
            lock.writeLock().unlock();
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
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            zexpirePrivate(key, expire);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void del(String key) throws IOException {
        ReadWriteLock lock = getLock(key);
        try {
            lock.writeLock().lock();
            zdelPrivate(key);
        }finally {
            lock.writeLock().unlock();
        }
    }
}
