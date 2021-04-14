package core.storage.rocksdb.data.structure.zset;

/**
 * Created by lick on 2020/11/3.
 * Description：
 */
public class KeyFieldLevelData {
    public byte[] nextValues;
    public byte[] downValues;
    public Integer span;
    public double score;
    public KeyFieldLevelData(byte[] nextValues, byte[] downValues, Integer span, double score){
        this.nextValues = nextValues;
        this.downValues = downValues;
        this.span = span;
        this.score = score;
    }
}
