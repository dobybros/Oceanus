package core.discovery.impl.client;

import core.net.adapters.data.ContentPacket;
import core.net.data.ResponseTransport;

public interface ContentPacketReceiver<K> {
    ResponseTransport contentPacketReceived(ContentPacket<K> contentPacket);
}
