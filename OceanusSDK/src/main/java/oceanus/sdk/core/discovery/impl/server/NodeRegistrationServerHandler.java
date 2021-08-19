package oceanus.sdk.core.discovery.impl.server;

import oceanus.sdk.core.discovery.DiscoveryManager;
import oceanus.sdk.core.discovery.data.discovery.NodeRegistrationRequest;
import oceanus.sdk.core.discovery.data.discovery.NodeRegistrationResponse;
import oceanus.sdk.core.discovery.errors.DiscoveryErrorCodes;
import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.logger.LoggerEx;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NodeRegistrationServerHandler extends ServerHandler implements ContentPacketListener<NodeRegistrationRequest> {
    private static final String TAG = NodeRegistrationServerHandler.class.getSimpleName();

    public NodeRegistrationServerHandler(DiscoveryManager discoveryManager) {
        super(discoveryManager);
    }

    @Override
    public ResponseTransport contentPacketReceived(ContentPacket<NodeRegistrationRequest> contentPacket, long serverIdCRC, InetSocketAddress address) {
        DiscoveryManagerImpl discoveryManagerImpl = (DiscoveryManagerImpl) discoveryManager;
        ResponseTransport responseTransport = null;
        NodeRegistrationRequest request = contentPacket.getContent();
        NodeRegistrationResponse response = request.generateResponse();
        String remoteAddress = address.getHostString();
        response.setPublicIp(remoteAddress);
        response.setPublicPort(address.getPort());
        if(request.getNode() != null) {
            Node node = request.getNode();
            List<String> ips =  node.getIps();
            if(ips == null) {
                ips = new ArrayList<>();
                node.setIps(ips);
            }
            if(!ips.contains(remoteAddress)) {
                ips.add(remoteAddress);
            }
            node.setPort(address.getPort());
            response.setNeedHolePunching(node.isNeedHolePunching());
            Node old = discoveryManagerImpl.nodeMap.putIfAbsent(serverIdCRC, node);
            if(old != null) {
                if(!old.getServerName().equals(node.getServerName())) {
                    LoggerEx.error(TAG, "serverIdCRC is existing already, old serverName " + old.getServerName() + " new serverName " + node.getServerName() + " new server from " + remoteAddress + " will be denied to join. ");
                    responseTransport = request.generateFailedResponse(DiscoveryErrorCodes.ERROR_DUPLICATED_SERVER_CRC, "Duplicated server crc");
                } else {
                    old.setIps(node.getIps());
                    old.setPort(node.getPort());
                    old.setNeedHolePunching(node.isNeedHolePunching());
//                                            node = old;
                }
            }
            if(responseTransport == null) {
                responseTransport = response;
            }
        }
        discoveryManagerImpl.tentaclePingTimeMap.put(serverIdCRC, System.currentTimeMillis());
        return responseTransport;
    }
}
