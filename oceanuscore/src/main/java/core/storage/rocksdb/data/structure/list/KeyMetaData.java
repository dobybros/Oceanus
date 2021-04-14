package core.storage.rocksdb.data.structure.list;

/**
 * Created by lick on 2020/10/13.
 * Description：
 */
public class KeyMetaData {
    public Long version;
    public Long ttl;
    public Long firstSeq;
    public Long lastSeq;
    public Long length;
    public KeyMetaData(Long version, Long ttl, Long firstSeq, Long lastSeq, Long length){
        this.version = version;
        this.ttl = ttl;
        this.firstSeq = firstSeq;
        this.lastSeq = lastSeq;
        this.length = length;
    }
}
