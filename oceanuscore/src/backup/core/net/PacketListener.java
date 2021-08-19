package core.net;

import core.net.adapters.data.Packet;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface PacketListener<T extends Packet> {
    void packetReceived(T packet, long serverIdCRC, InetSocketAddress inetAddress);
}