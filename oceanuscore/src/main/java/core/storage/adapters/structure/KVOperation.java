package core.storage.adapters.structure;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by lick on 2020/10/30.
 * Description：
 */
public interface KVOperation {
    /**
     * If key already holds a value, it is overwritten
     * @param bytes Value, it must be an array of bytes, can't be null
     */
    public void set(String key, byte[] bytes) throws IOException;

    /**
     * If key already holds a value, it is overwritten
     * @param bytes Value, it must be an array of bytes, can't be null
     * @param expire Set the specified expire time, in millisecond,can't be null
     */
    public void set(String key, byte[] bytes, Long expire) throws IOException;

    /**
     *  Only set the key if it does not already exist.
     * @param bytes Value, it must be an array of bytes, can't be null
     * @return If key already exist, return old value, otherwise return null
     */
    public byte[] setIfAbsent(String key, byte[] bytes) throws IOException;

    /**
     * Only set the key if it does not already exist
     * @param bytes Value, it must be an array of bytes, can't be null
     * @param expire Set the specified expire time, in millisecond, can't be null
     * @return If successful, return 1, otherwise return 0
     */
    public Integer setIfAbsent(String key, byte[] bytes, Long expire) throws IOException;

    /**
     * If key already holds a value, it is overwritten;
     * It is an atomic operation, there will be no situation where only a part of the settings is successful
     * @param bytesMap Map of bytes corresponding to multiple keys, can't be null
     */
    public void mset(Map<String, byte[]> bytesMap) throws IOException;

    /**
     * It will overwritten old value if key exist
     * @param bytes Value, it must be an array of bytes, can't be null
     * @return If key exist, return old value, otherwise return null
     */
    public byte[] getSet(String key, byte[] bytes) throws IOException;

    /**
     * If key exist, the spliced bytes is behind the original bytes, otherwise set key bytes
     * @param bytes Append value, can't be null
     * @return New bytes after append
     */
    public byte[] append(String key, byte[] bytes) throws IOException;

    /**
     *  Get multiple key's value
     * @return multiple key's value
     */
    public List<byte[]> mget(String... keys) throws IOException;

    /**
     * Get key's value
     * @return key's value
     */
    public byte[] get(String key) throws IOException;

    /**
     * Get length of key's value
     * @return length of key's value
     */
    public Integer len(String key) throws IOException;

    /**
     * If key exist, set key the specified expire time, in millisecond, otherwise throw exception
     * @param expire Set the specified expire time, in millisecond, can't be null
     */
    public void expire(String key, Long expire)throws IOException;

    /**
     * Delete keys
     */
    public void del(String... keys)throws IOException;
    /**
     * Delete key
     */
    public void del(String key)throws IOException;
}
