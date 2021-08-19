package core.consensus.model;

public abstract class ClusterNode {
    private long serverIdCRC;

    public long getServerIdCRC() {
        return serverIdCRC;
    }

    public void setServerIdCRC(long serverIdCRC) {
        this.serverIdCRC = serverIdCRC;
    }
}
