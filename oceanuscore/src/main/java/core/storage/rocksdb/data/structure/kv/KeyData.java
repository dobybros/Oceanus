package core.storage.rocksdb.data.structure.kv;

/**
 * Created by lick on 2020/10/30.
 * Description：
 */
public class KeyData {
    public byte[] bytes;
    public long ttl;
    public KeyData(byte[] bytes, long ttl){
        this.bytes = bytes;
        this.ttl = ttl;
    }
}
