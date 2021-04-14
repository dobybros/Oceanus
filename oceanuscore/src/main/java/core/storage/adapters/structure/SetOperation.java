package core.storage.adapters.structure;

import java.io.IOException;
import java.util.Set;

/**
 * Created by lick on 2020/10/27.
 * Description：
 */
public interface SetOperation {
    /**
     * Add value to set of key, can't be repeated
     * @param bytes Value, must be an array of bytes
     * @return length of set
     */
    public Long add(String key, byte[]... bytes) throws IOException;

    /**
     * Get all values of key
     * @return values of key
     */
    public Set<byte[]> get(String key) throws IOException;

    /**
     * Get length of set
     * @return if key not exist, return 0, otherwise return set's length
     */
    public Long len(String key) throws IOException;

    /**
     * Determine whether it is a member of key
     * @param bytes Value, must be an array of bytes
     * @return
     */
    public Boolean isMember(String key, byte[] bytes) throws IOException;

    /**
     * Move a value from sourceKey to destinationKey
     * @param sourceKey source set
     * @param destinationKey destination set
     * @param bytes Value, must be an array of bytes
     * @return Moved value
     */
    public byte[] move(String sourceKey, String destinationKey, byte[] bytes) throws IOException;

    /**
     * Delete multiple value of key
     * @param bytes Multiple array of bytes
     */
    public void del(String key, byte[]... bytes) throws IOException;
    /**
     * Delete a value of key
     * @param bytes Value, must be an array of bytes
     */
    public void del(String key, byte[] bytes) throws IOException;
    /**
     * If key exist, set key the specified expire time, in millisecond, otherwise throw exception
     * @param expire Set the specified expire time, in millisecond, can't be null
     */
    public void expire(String key, Long expire)throws IOException;
    /**
     * Delete set of key
     */
    public void del(String key)throws IOException;
}
