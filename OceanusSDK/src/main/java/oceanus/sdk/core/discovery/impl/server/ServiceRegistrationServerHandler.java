package oceanus.sdk.core.discovery.impl.server;

import oceanus.sdk.core.discovery.DiscoveryManager;
import oceanus.sdk.core.discovery.data.discovery.ServiceRegistrationRequest;
import oceanus.sdk.core.discovery.data.discovery.ServiceRegistrationResponse;
import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.logger.LoggerEx;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentSkipListSet;

public class ServiceRegistrationServerHandler extends ServerHandler implements ContentPacketListener<ServiceRegistrationRequest> {
    private static final String TAG = ServiceRegistrationServerHandler.class.getSimpleName();

    public ServiceRegistrationServerHandler(DiscoveryManager discoveryManager) {
        super(discoveryManager);
    }

    @Override
    public ResponseTransport contentPacketReceived(ContentPacket<ServiceRegistrationRequest> contentPacket, long serverIdCRC, InetSocketAddress address) {
        DiscoveryManagerImpl discoveryManagerImpl = (DiscoveryManagerImpl) discoveryManager;
        ServiceRegistrationRequest request = contentPacket.getContent();
        ResponseTransport responseTransport = null;
        if(request != null) {
            Service service = request.getService();
            String serviceKey = service.generateServiceKey();
            Service old = discoveryManagerImpl.serviceMap.put(serviceKey, service);
            if(old != null) {
                LoggerEx.info(TAG, "ServiceRegistrationServerHandler: Service is replaced by " + service + " old is " + old);
            }
            ConcurrentSkipListSet<Long> nodeServers = discoveryManagerImpl.serviceNodesMap.get(serviceKey);
            if(nodeServers == null) {
                synchronized (discoveryManagerImpl) {
                    nodeServers = discoveryManagerImpl.serviceNodesMap.get(serviceKey);
                    if(nodeServers == null) {
                        nodeServers = new ConcurrentSkipListSet<>();
                        ConcurrentSkipListSet<Long> existingNodeServers = discoveryManagerImpl.serviceNodesMap.putIfAbsent(serviceKey, nodeServers);
                        if(existingNodeServers != null) {
                            nodeServers = existingNodeServers;
                        }
                    }
                }
            }
            nodeServers.add(serverIdCRC);
            ServiceRegistrationResponse response = request.generateResponse();
            responseTransport = response;
        }
        return responseTransport;
    }


}
