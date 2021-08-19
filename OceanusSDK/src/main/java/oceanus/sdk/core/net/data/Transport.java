package oceanus.sdk.core.net.data;

public abstract class Transport {
    protected String transportId;

    public String getTransportId() {
        return transportId;
    }

    public void setTransportId(String transportId) {
        this.transportId = transportId;
    }
}
