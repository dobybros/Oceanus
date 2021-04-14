package core.storage.rocksdb.data.structure.common;

/**
 * Created by lick on 2020/10/19.
 * Description：
 */
public class KeyExpireData {
    public int length;
    public byte[] value;
    public KeyExpireData(int length, byte[] value){
        this.length = length;
        this.value = value;
    }
}
