package core.storage.rocksdb.data.structure.set;

/**
 * Created by lick on 2020/10/13.
 * Description：
 */
public class KeyMetaData {
    public static final Long VERSION_DEFAULT = 1L;
    public Long version = VERSION_DEFAULT;
    public static final Long TTL_DEFAULT = -1L;
    public Long ttl = -1L;
    public static final byte[] BYTES_DEFAULT = new byte[0];
    public byte[] firstBytes = BYTES_DEFAULT;
    public byte[] lastBytes = BYTES_DEFAULT;
    public static Long LENGTH_DEFAULT = 0L;
    public Long length = LENGTH_DEFAULT;
    public KeyMetaData(Long version, Long ttl, byte[] firstBytes, byte[] lastBytes, Long length) {
        this.version = version;
        this.ttl = ttl;
        this.firstBytes = firstBytes;
        this.lastBytes = lastBytes;
        this.length = length;
    }
    public KeyMetaData(){}
}
