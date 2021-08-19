package core.consensus.model.data;

public abstract class VersionData {
    private long version;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
