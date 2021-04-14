package core.discovery.impl.server;

import core.common.CoreRuntime;
import core.discovery.DiscoveryManager;
import core.discovery.data.discovery.FindServiceRequest;
import core.discovery.data.discovery.FindServiceResponse;
import core.discovery.errors.DiscoveryErrorCodes;
import core.net.ContentPacketListener;
import core.net.adapters.data.ContentPacket;
import core.net.data.ResponseTransport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class FindServiceServerHandler extends ServerHandler implements ContentPacketListener<FindServiceRequest> {
    public FindServiceServerHandler(DiscoveryManager discoveryManager) {
        super(discoveryManager);
    }

    @Override
    public ResponseTransport contentPacketReceived(ContentPacket<FindServiceRequest> contentPacket, long serverIdCRC, InetSocketAddress address) {
        DiscoveryManagerImpl discoveryManagerImpl = (DiscoveryManagerImpl) discoveryManager;
        FindServiceRequest request = contentPacket.getContent();
        ResponseTransport responseTransport = null;
        if(request != null) {
            String serviceKey = request.generateServiceKey();
            if(serviceKey != null) {
                //TODO only return nodes who ping last 6 seconds
                ConcurrentSkipListSet<Long> existingNodeServers = discoveryManagerImpl.serviceNodesMap.get(serviceKey);
                if(existingNodeServers != null) {
                    List<Long> returnServers = new ArrayList<>();
                    for(Long existingNodeServer : existingNodeServers) {
                        Long time = discoveryManagerImpl.tentaclePingTimeMap.get(existingNodeServer);
                        if(time != null && System.currentTimeMillis() - time < CoreRuntime.FIND_SERVICE_PING_TIMEOUT) {
                            returnServers.add(existingNodeServer);
                        }
                    }

                    FindServiceResponse response = request.generateResponse();
                    if(response != null) {
                        response.setNodeServers(new ArrayList<>(returnServers));
                        responseTransport = response;
                    } else {
                        responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_REQUEST_GENERATE_RESPONSE_FAILED, "generate response failed");
                    }
                } else {
                    responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_FIND_SERVICE_NOT_FOUND, "Service " + serviceKey + " not found") ;
                }
            } else {
                responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_SERVICE_KEY_NULL, "ServiceKey is null");
            }
        }
        return responseTransport;
    }
}
