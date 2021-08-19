package core.storage.adapters.impl;

import core.common.InternalTools;
import core.storage.adapters.LocalStorage;
import core.storage.adapters.structure.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;

/**
 * Created by lick on 2020/10/15.
 * Description：
 */
public class RocksDBLocalStorage extends LocalStorage {
    private HashOperation hashOperation;
    private ListOperation listOperation;
    private SetOperation setOperation;
    private ZSetOperation zSetOperation;
    private KVOperation kvOperation;

    @Override
    public KVOperation getKVOperation() throws IOException {
        init();
        if(kvOperation == null){
            synchronized (hashLock){
                try {
                    Class<?> clazz = Class.forName("core.storage.rocksdb.handler.operation.RocksDBKVOperation");
                    kvOperation = (KVOperation) clazz.getDeclaredConstructor(RocksDB.class, InternalTools.class).newInstance(rocksDB, internalTools);
                }catch(Throwable t){
                    throw new IOException("Get kv operation failed " + t);
                }
            }
        }
        return kvOperation;
    }

    @Override
    public HashOperation getHashOperation() throws IOException {
        init();
        if(hashOperation == null){
            synchronized (hashLock){
                try {
                    Class<?> clazz = Class.forName("core.storage.rocksdb.handler.operation.RocksDBHashOperation");
                    hashOperation = (HashOperation) clazz.getDeclaredConstructor(RocksDB.class, InternalTools.class).newInstance(rocksDB, internalTools);
                }catch(Throwable t){
                    throw new IOException("Get hash operation failed " + t);
                }
            }
        }
        return hashOperation;
    }

    @Override
    public ListOperation getListOperation() throws IOException {
        init();
        if(listOperation == null){
            synchronized (hashLock){
                try {
                    Class<?> clazz = Class.forName("core.storage.rocksdb.handler.operation.RocksDBListOperation");
                    listOperation = (ListOperation) clazz.getDeclaredConstructor(RocksDB.class, InternalTools.class).newInstance(rocksDB, internalTools);
                }catch(Throwable t){
                    throw new IOException("Get list operation failed " + t);
                }
            }
        }
        return listOperation;
    }

    @Override
    public SetOperation getSetOperation() throws IOException {
        init();
        if(setOperation == null){
            synchronized (hashLock){
                try {
                    Class<?> clazz = Class.forName("core.storage.rocksdb.handler.operation.RocksDBSetOperation");
                    setOperation = (SetOperation) clazz.getDeclaredConstructor(RocksDB.class, InternalTools.class).newInstance(rocksDB, internalTools);
                }catch(Throwable t){
                    throw new IOException("Get set operation failed " + t);
                }
            }
        }
        return setOperation;
    }

    @Override
    public ZSetOperation getZSetOperation() throws IOException {
        init();
        if(zSetOperation == null){
            synchronized (hashLock){
                try {
                    Class<?> clazz = Class.forName("core.storage.rocksdb.handler.operation.RocksDBZSetOperation");
                    zSetOperation = (ZSetOperation) clazz.getDeclaredConstructor(RocksDB.class, InternalTools.class).newInstance(rocksDB, internalTools);
                }catch(Throwable t){
                    throw new IOException("Get zset operation failed " + t);
                }
            }
        }
        return zSetOperation;
    }

    private final byte[] hashLock = new byte[0];
    private RocksDB rocksDB = null;
    private void init() throws IOException {
        if(rocksDB == null){
            synchronized (RocksDBLocalStorage.class){
                if(rocksDB == null){
                    String realPath = FilenameUtils.concat(new File(path).getAbsolutePath(), name);
                    FileUtils.forceMkdir(new File(realPath));
                    Options options = new Options();
                    options.setCreateIfMissing(true);
                    try {
                        rocksDB = RocksDB.open(options, realPath);
                    } catch (RocksDBException e) {
                        e.printStackTrace();
                        throw new IOException("Open database on " + realPath + " failed, " + e.getMessage());
                    }
                }
            }
        }
    }
}
