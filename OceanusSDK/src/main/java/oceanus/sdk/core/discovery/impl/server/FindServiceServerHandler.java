package oceanus.sdk.core.discovery.impl.server;

import oceanus.sdk.core.common.CoreRuntime;
import oceanus.sdk.core.discovery.DiscoveryManager;
import oceanus.sdk.core.discovery.data.discovery.FindServiceRequest;
import oceanus.sdk.core.discovery.data.discovery.FindServiceResponse;
import oceanus.sdk.core.discovery.errors.DiscoveryErrorCodes;
import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.discovery.node.ServiceNodeResult;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.ResponseTransport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
            Collection<String> services = request.getServices();
            Collection<Long> nodeCRCIds = request.getCheckNodesAvailability();
            boolean onlyNodeServerCRC = request.isOnlyNodeServerCRC();
            ServiceNodeResult serviceNodeResult = new ServiceNodeResult();
            if(services != null) {
                if(onlyNodeServerCRC)
                    serviceNodeResult.setServiceNodeCRCIds(new HashMap<>());
                else
                    serviceNodeResult.setServiceNodes(new HashMap<>());

                for(String serviceKey : services) {
                    //TODO only return nodes who ping last 6 seconds
                    ConcurrentSkipListSet<Long> existingNodeServers = discoveryManagerImpl.serviceNodesMap.get(serviceKey);
                    if(existingNodeServers != null) {
                        List<Node> returnServers = new ArrayList<>();
                        List<Long> returnServersLong = new ArrayList<>();
                        for(Long existingNodeServer : existingNodeServers) {
                            Long time = discoveryManagerImpl.tentaclePingTimeMap.get(existingNodeServer);
                            if(time != null && System.currentTimeMillis() - time < CoreRuntime.FIND_SERVICE_PING_TIMEOUT) {
                                Node node = discoveryManagerImpl.nodeMap.get(existingNodeServer);
                                if(node != null) {
                                    if(onlyNodeServerCRC) {
                                        returnServersLong.add(existingNodeServer);
                                    } else {
                                        returnServers.add(node);
                                    }
                                }
                            }
                        }
                        if(onlyNodeServerCRC) {
                            serviceNodeResult.getServiceNodeCRCIds().put(serviceKey, returnServersLong);
                        } else {
                            serviceNodeResult.getServiceNodes().put(serviceKey, returnServers);
                        }
                    } /*else {
                        responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_FIND_SERVICE_NOT_FOUND, "Service " + serviceKey + " not found") ;
                    }*/
                }
            } /*else {
                responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_SERVICE_KEY_NULL, "ServiceKey is null");
            }*/
            if(nodeCRCIds != null) {
                serviceNodeResult.setDeadNodes(new ArrayList<>());
                for(Long nodeCRCId : nodeCRCIds) {
                    if(!discoveryManagerImpl.nodeMap.containsKey(nodeCRCId)) {
                        serviceNodeResult.getDeadNodes().add(nodeCRCId);
                    }
                }
            }

            FindServiceResponse response = request.generateResponse();
            if(response != null) {
                response.setServiceNodeResult(serviceNodeResult);
                responseTransport = response;
            } else {
                responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_REQUEST_GENERATE_RESPONSE_FAILED, "generate response failed");
            }
            
        }
        return responseTransport;
    }
}
