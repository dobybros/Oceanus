package core.storage.rocksdb.data.structure.list;

/**
 * Created by lick on 2020/10/13.
 * Description：
 */
public class KeySeqMetaData {
    public byte[] value;
    public Long preSeq;
    public Long nextSeq;
    public KeySeqMetaData(byte[] value, Long preSeq, Long nextSeq) {
        this.value = value;
        this.preSeq = preSeq;
        this.nextSeq = nextSeq;
    }
}
