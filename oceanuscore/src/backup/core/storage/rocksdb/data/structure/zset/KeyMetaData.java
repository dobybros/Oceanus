package core.storage.rocksdb.data.structure.zset;

/**
 * Created by lick on 2020/11/3.
 * Description：
 */
public class KeyMetaData {
    public static final Long TTL_DEFAULT = -1L;
    public Long ttl = TTL_DEFAULT;
    public static final Long VERSION_DEFAULT = 1L;
    public Long version = VERSION_DEFAULT;
    public static Long LENGTH_DEFAULT = 0L;
    public Long length = LENGTH_DEFAULT;
    public static Long LEVEL_DEFAULT = -1L;
    public Long maxLevel = LEVEL_DEFAULT;
    public static final byte[] BYTES_DEFAULT = new byte[0];
    public KeyMetaData(){}
    public KeyMetaData(Long ttl, Long version, Long length, long maxLevel){
        this.ttl = ttl;
        this.version = version;
        this.length = length;
        this.maxLevel = maxLevel;
    }
}
