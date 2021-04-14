package core.storage.rocksdb.data.structure.set;

/**
 * Created by lick on 2020/10/13.
 * Description：
 */
public class KeyFieldData {
    public static final byte[] BYTES_DEFAULT = new byte[0];
    public byte[] preBytes = BYTES_DEFAULT;
    public byte[] nextBytes = BYTES_DEFAULT;
    public KeyFieldData(byte[] preBytes, byte[] nextBytes){
        this.preBytes = preBytes;
        this.nextBytes = nextBytes;
    }
}
