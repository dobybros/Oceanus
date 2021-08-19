package oceanus.sdk.core.net.adapters.data;

import oceanus.sdk.core.net.NetworkCommunicator;
public class StringPacket extends Packet{
    private String packetType;
    private String content;
    public StringPacket(String packetType, String content) {
        super(NetworkCommunicator.PACKET_TYPE_STRING);
//        this.contentType = "";
        this.content = content;
        this.packetType = packetType;
    }

    public String getPacketType() {
        return packetType;
    }

    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
