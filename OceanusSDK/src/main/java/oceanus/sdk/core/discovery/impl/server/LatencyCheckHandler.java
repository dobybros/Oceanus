package oceanus.sdk.core.discovery.impl.server;

import oceanus.sdk.core.discovery.DiscoveryManager;
import oceanus.sdk.core.discovery.data.discovery.LatencyCheckRequest;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.ResponseTransport;

import java.net.InetSocketAddress;

public class LatencyCheckHandler extends ServerHandler implements ContentPacketListener<LatencyCheckRequest> {
    public LatencyCheckHandler(DiscoveryManager discoveryManager) {
        super(discoveryManager);
    }

    @Override
    public ResponseTransport contentPacketReceived(ContentPacket<LatencyCheckRequest> contentPacket, long serverIdCRC, InetSocketAddress address) {
        LatencyCheckRequest request = contentPacket.getContent();
        return request.generateResponse();
    }
}
