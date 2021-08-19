package oceanus.sdk.core.discovery.impl.client;

import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.ResponseTransport;

public interface ContentPacketReceiver<K> {
    ResponseTransport contentPacketReceived(ContentPacket<K> contentPacket);
}
