package oceanus.sdk.core.discovery.impl.client;

import oceanus.sdk.core.discovery.NodeRegistrationHandler;
import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.data.RequestTransport;

public class ServiceRuntime {
    private Service service;
    NodeRegistrationHandler nodeRegistrationHandler;

    public ServiceRuntime(Service service) {
        this.service = service;
    }

    public <T extends RequestTransport<?>> ServiceRuntime addContentPacketReceiver(Class<T> clazz, ContentPacketReceiver<T> packetListener) {
        nodeRegistrationHandler.getConnectedNetworkCommunicator().addServiceContentPacketListener(service.generateServiceKey(), clazz, (contentPacket, serverIdCRC, address) -> packetListener.contentPacketReceived(contentPacket));
        return this;
    }

    public <T extends RequestTransport<?>> void removeContentPacketListener(Class<T> clazz, ContentPacketListener<T> packetListener) {

    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
