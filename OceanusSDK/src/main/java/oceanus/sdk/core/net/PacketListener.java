package oceanus.sdk.core.net;

import oceanus.sdk.core.net.adapters.data.Packet;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface PacketListener<T extends Packet> {
    void packetReceived(T packet, long serverIdCRC, InetSocketAddress inetAddress);
}