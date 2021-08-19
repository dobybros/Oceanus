package oceanus.sdk.core.net.adapters.data;

import oceanus.sdk.core.net.NetworkCommunicator;

public class ServerNamePacket extends Packet{
    private String serverName;
    public ServerNamePacket(String serverName) {
        super(NetworkCommunicator.PACKET_TYPE_SERVER_NAME);
//        this.contentType = "";
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
