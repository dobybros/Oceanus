package core.discovery.impl.server;

import core.discovery.DiscoveryInfo;
import core.discovery.DiscoveryManager;
import core.discovery.data.discovery.GetAllServicesRequest;
import core.discovery.data.discovery.GetAllServicesResponse;
import core.net.ContentPacketListener;
import core.net.adapters.data.ContentPacket;
import core.net.data.ResponseTransport;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class GetAllServicesHandler extends ServerHandler implements ContentPacketListener<GetAllServicesRequest> {
    public GetAllServicesHandler(DiscoveryManager discoveryManager) {
        super(discoveryManager);
    }

    @Override
    public ResponseTransport contentPacketReceived(ContentPacket<GetAllServicesRequest> contentPacket, long serverIdCRC, InetSocketAddress address) {
        GetAllServicesRequest request = contentPacket.getContent();
        ResponseTransport responseTransport = null;
        if(request != null) {
            DiscoveryInfo discoveryInfo = discoveryManager.getDiscoveryInfo();
            GetAllServicesResponse response = new GetAllServicesResponse();
            response.setServices(new ArrayList<>(discoveryInfo.getServiceNodesMap().keySet()));
            responseTransport = response;
        }
        return responseTransport;
    }
}
