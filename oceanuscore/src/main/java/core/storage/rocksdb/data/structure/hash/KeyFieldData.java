package core.storage.rocksdb.data.structure.hash;

/**
 * Created by lick on 2020/10/13.
 * Description：
 */
public class KeyFieldData {
    public static final String FIELD_DEFAULT = "";
    public String preField;
    public String nextField;
    public byte[] value;
    public KeyFieldData(String preField, String nextField, byte[] value){
        this.preField = preField;
        this.nextField = nextField;
        this.value = value;
    }
}
