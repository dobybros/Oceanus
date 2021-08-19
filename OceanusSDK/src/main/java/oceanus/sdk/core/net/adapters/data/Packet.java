package oceanus.sdk.core.net.adapters.data;

public abstract class Packet {

    protected short type;

    protected boolean needReliable = true;

    public Packet(short type) {
        this.type = type;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public boolean isNeedReliable() {
        return needReliable;
    }

    public void setNeedReliable(boolean needReliable) {
        this.needReliable = needReliable;
    }

}
