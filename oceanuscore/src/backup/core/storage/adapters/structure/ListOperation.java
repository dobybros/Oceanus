package core.storage.adapters.structure;

import java.io.IOException;
import java.util.List;

/**
 * Created by lick on 2020/10/25.
 * Description：
 */
public interface ListOperation {

    /**
     * Get list of key's size
     * @return if key not exist, return 0
     */
    public Long len(String key) throws IOException;

    /**
     * Take the first value from the left of the list
     * @return array of bytes
     */
    public byte[] lpop(String key) throws IOException;

    /**
     * Take the first value from the right of the list
     * @return array of bytes
     */
    public byte[] rpop(String key) throws IOException;

    /**
     * Add to the left of the list
     * @param bytes Value, must be an array of bytes
     * @return length of list
     */
    public Long lpush(String key, byte[]... bytes) throws IOException;

    /**
     * If list of key exist, add to the left of the list
     * @param bytes Value, must be an array of bytes
     * @return length of list
     */
    public Long lpushIfExists(String key, byte[] bytes) throws IOException;

    /**
     * Add to the right of the list
     * @param bytes Value, must be an array of bytes
     * @return length of list
     */
    public Long rpush(String key, byte[]... bytes) throws IOException;

    /**
     * If list of key exist, add to the right of the list
     * @param bytes Value, must be an array of bytes
     * @return length of list
     */
    public Long rpushIfExists(String key, byte[] bytes) throws IOException;

    /**
     * Get the value of the list according to the subscript,
     * if start < 0, start =0;if stop is -1, it is the last one, if stop is -2, it is the second to last one, and so on
     * @param start first index
     * @param stop end index
     * @return Values between subscripts, if start > stop, return null, if start >= length, return null
     */
    public List<byte[]> get(String key, long start, long stop) throws IOException;

    /**
     * Take the first value from the right of the source list, add  add to the left of the destination list, it is atomic
     * @param sourceKey source list
     * @param destinationKey destination list
     * @return If sourceKey exist, will return the first value of source list, otherwise return null
     */
    public byte[] rpopLpush(String sourceKey, String destinationKey) throws IOException;

    /**
     * If key exist, set key the specified expire time, in millisecond, otherwise throw exception
     * @param expire Set the specified expire time, in millisecond, can't be null
     */
    public void expire(String key, Long expire)throws IOException;

    /**
     * Delete list of key
     */
    public void del(String key)throws IOException;
}
