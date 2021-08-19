package core.net;

import core.net.adapters.data.ContentPacket;
import core.net.data.ResponseTransport;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface ContentPacketListener<K> {
    ResponseTransport contentPacketReceived(ContentPacket<K> contentPacket, long serverIdCRC, InetSocketAddress address);
}