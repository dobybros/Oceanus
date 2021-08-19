package oceanus.sdk.core.discovery.data.discovery;

import oceanus.sdk.core.net.data.ResponseTransport;

public class NodeRegistrationResponse extends ResponseTransport {
    private String publicIp;
    private int publicPort;
    private boolean needHolePunching;

    public boolean isNeedHolePunching() {
        return needHolePunching;
    }

    public void setNeedHolePunching(boolean needHolePunching) {
        this.needHolePunching = needHolePunching;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(int publicPort) {
        this.publicPort = publicPort;
    }
}
