package core.storage.adapters.structure;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by lick on 2020/10/15.
 * Description：
 */
public interface HashOperation {
    /**
     * Get Key-field's value
     * @return Key-field's value, is an array of bytes
     */
    public byte[] get(String key, String field) throws IOException;

    /**
     * Get key's all fields's value
     * @return Multiple key's fields value
     */
    public Map<String, byte[]> getAll(String key)throws IOException;

    /**
     * Set key-field's value, if key-field already holds a value, it is overwritten
     * @param bytes Value, must be an array of bytes
     */
    public void set(String key, String field, byte[] bytes)throws IOException;

    /**
     *
     * @param key
     * @param field
     * @param bytes
     * @param expire
     * @throws IOException
     */
//    public void setEx(String key, String field, byte[] bytes, Long expire)throws IOException;

    /**
     * Only if key-filed is not exist, will set key-field's value
     * @param bytes Value, must be an array of bytes
     * @return if key-field exist, will return old value, otherwise return null
     */
    public byte[] setIfAbsent(String key, String field, byte[] bytes)throws IOException;

    /**
     * Get length of key's fields
     * @return length of key's fields, if key not exist, will return 0
     */
    public Long len(String key)throws IOException;

    /**
     * Delete key, will delete all of fields of key
     */
    public void del(String key)throws IOException;

    /**
     * Delete a key-field
     */
    public void del(String key, String field)throws IOException;

    /**
     * Delete several fields of key
     */
    public void del(String key, String... fields) throws IOException;
    /**
     * Set multiple field-values of key
     */
    public void mSet(String key, Map<String, byte[]> tMap)throws IOException;

    /**
     * Get mutiple fields's value of key
     * @return List contains multiple array of bytes
     */
    public List<byte[]> mGet(String key, String... fields)throws IOException;

    /**
     * If key exist, set key the specified expire time, in millisecond, otherwise throw exception
     * @param expire Set the specified expire time, in millisecond, can't be null
     */
    public void expire(String key, Long expire)throws IOException;


//    public void expire(String key, Long expire, String... fields)throws IOException;
}
