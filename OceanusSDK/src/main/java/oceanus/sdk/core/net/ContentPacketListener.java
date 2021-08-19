package oceanus.sdk.core.net;

import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.ResponseTransport;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface ContentPacketListener<K> {
    ResponseTransport contentPacketReceived(ContentPacket<K> contentPacket, long serverIdCRC, InetSocketAddress address);
}