package core.storage.adapters.structure;

import core.storage.adapters.data.zset.Tuple;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Created by lick on 2020/10/28.
 * Description：
 */
public interface ZSetOperation {
    /**
     * Add value to set and set value's score
     * @param score value's score, will sort by score in set of key
     * @param bytes Value, must be an array of bytes
     * @return length of set
     */
    public Long add(String key, double score, byte[] bytes) throws IOException;
    /**
     * Add multiple values to set and set value's score
     * @param bytesScores Multiple value's score, will sort by score in set of key
     * @return length of set
     */
    public Long add(String key, Map<byte[], Double> bytesScores) throws IOException;

    /**
     * Get set of key's length
     * @return if key not exist ,return 0, otherwise return set's length
     */
    public Long len(String key) throws IOException;
    /**
     * Get set of key's length which is between minScore and maxScore
     * @return if key not exist ,return 0, otherwise return set's length
     */
    public Long len(String key, double minScore, double maxScore) throws IOException;

    /**
     * if key-value not exist, set value's score to incrScore, otherwise increase the score of value by incrScore
     * @param incrScore Increased score
     * @param bytes value, must be an array of bytes
     * @return Score after increased incrScore
     */
    public Double incrby(String key, double incrScore, byte[] bytes) throws IOException;

    /**
     * Set is sored by score, every score has a subscript corresponding to the value
     * Get the value of the set according to the subscript,
     * if start < 0, start =0;if stop is -1, it is the last one, if stop is -2, it is the second to last one, and so on
     * @param start start index
     * @param stop end index
     * @return Values between subscripts, if start > stop, return null, if start >= length, return null, every tuple contains value and score
     */
    public Set<Tuple> get(String key, long start, long stop) throws IOException;

    /**
     * Get the value of the set according to the score,
     * @param minScore Value's score >= min score
     * @param maxScore Value's score <= max score
     * @return Values between subscripts, every tuple contains value and score
     */
    public Set<Tuple> getByScore(String key, double minScore, double maxScore) throws IOException;

    /**
     * Get value's rank in set of key, 0 is first
     * @param bytes value, must be an array of bytes
     * @return 0 is first, if value not exist, return null
     */
    public Long rank(String key, byte[] bytes) throws IOException;

    /**
     * Delete multiple value
     * @param bytes value, must be an array of bytes
     */
    public void del(String key, byte[]... bytes) throws IOException;
    /**
     * Delete a value
     * @param bytes value, must be an array of bytes
     */
    public void del(String key, byte[] bytes) throws IOException;

    /**
     * Set is sored by score, every score has a subscript corresponding to the value
     * Delete the value by of the set according to the subscript
     * if start < 0, start =0;if stop is -1, it is the last one, if stop is -2, it is the second to last one, and so on
     * @param start start index
     * @param stop end index
     */
    public void delRangeByRank(String key, long start, long stop) throws IOException;
    /**
     * Delete the value of the set according to the score,
     * @param minScore Value's score >= min score
     * @param maxScore Value's score <= max score
     */
    public void delRangeByScore(String key, double minScore, double maxScore) throws IOException;

    /**
     * Get value's score
     * @param bytes value, must be an array of bytes
     * @return value's score
     */
    public Double score(String key, byte[] bytes) throws IOException;
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
