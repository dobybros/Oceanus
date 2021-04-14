package core.discovery.impl.server;

import core.discovery.DiscoveryManager;
import core.discovery.data.discovery.LatencyCheckRequest;
import core.net.ContentPacketListener;
import core.net.adapters.data.ContentPacket;
import core.net.data.ResponseTransport;

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
