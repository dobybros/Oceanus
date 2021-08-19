package oceanus.sdk.core.discovery.impl.server;

import oceanus.sdk.core.discovery.DiscoveryManager;
import oceanus.sdk.core.discovery.data.discovery.GetNodeByServerCRCIdRequest;
import oceanus.sdk.core.discovery.data.discovery.GetNodeByServerCRCIdResponse;
import oceanus.sdk.core.discovery.errors.DiscoveryErrorCodes;
import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.ResponseTransport;

import java.net.InetSocketAddress;

public class GetNodeByServerCRCIdHandler extends ServerHandler implements ContentPacketListener<GetNodeByServerCRCIdRequest> {
    public GetNodeByServerCRCIdHandler(DiscoveryManager discoveryManager) {
        super(discoveryManager);
    }

    @Override
    public ResponseTransport contentPacketReceived(ContentPacket<GetNodeByServerCRCIdRequest> contentPacket, long serverIdCRC, InetSocketAddress address) {
        DiscoveryManagerImpl discoveryManagerImpl = (DiscoveryManagerImpl) discoveryManager;
        GetNodeByServerCRCIdRequest request = contentPacket.getContent();
        ResponseTransport responseTransport = null;
        if(request != null) {
            Long serverCRCId = request.getServerCRCId();
            if(serverCRCId != null) {
                Node node = discoveryManagerImpl.nodeMap.get(serverCRCId);
                if(node != null) {
                    GetNodeByServerCRCIdResponse response = request.generateResponse();
                    if(response != null) {
                        response.setNode(node);
                        responseTransport = response;
                    } else {
                        responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_REQUEST_GENERATE_RESPONSE_FAILED, "generate response failed");
                    }
                } else {
                    responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_FIND_NODE_NOT_FOUND, "Node " + serverCRCId + " not found") ;
                }
            } else {
                responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_SERVER_CRC_ID_NULL, "serverCRCId is null");
            }
        }
        return responseTransport;
    }
}
