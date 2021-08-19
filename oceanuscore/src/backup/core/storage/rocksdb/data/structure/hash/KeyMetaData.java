package core.storage.rocksdb.data.structure.hash;

/**
 * Created by lick on 2020/10/13.
 * Description：
 */
public class KeyMetaData {
    public static final Long VERSION_DEFAULT = 1L;
    public Long version = VERSION_DEFAULT;
    public static final Long TTL_DEFAULT = -1L;
    public Long ttl = -1L;
    public static final String FIELD_DEFAULT = "";
    public String firstField = FIELD_DEFAULT;
    public String lastField = FIELD_DEFAULT;
    public static Long LENGTH_DEFAULT = 0L;
    public Long length = LENGTH_DEFAULT;
    public KeyMetaData(Long version, Long ttl, String firstField, String lastField, Long length) {
        this.version = version;
        this.ttl = ttl;
        this.firstField = firstField;
        this.lastField = lastField;
        this.length = length;
    }
    public KeyMetaData(){}
}
