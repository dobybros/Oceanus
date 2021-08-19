package oceanus.sdk.core.discovery.impl.client;

import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.core.net.data.ResponseTransport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServiceInvocation<R extends ResponseTransport, K extends RequestTransport<R>> {
    private ServiceNodesManager serviceNodesManager;
    private ServiceNodesHandler serviceNodesHandler;
    private List<NodeConnectivityHandler> pendingConnectivityHandlers;
    private ContentPacket<K> contentPacket;
    private Class<R> responseClass;
    private String serviceKey;
    private CompletableFuture<ContentPacket<R>> future;
    private NodeConnectivityHandler connectivityHandler;
    public ServiceInvocation(ServiceNodesManager serviceNodesManager, ServiceNodesHandler serviceNodesHandler, ContentPacket<K> packet, Class<R> responseClass, CompletableFuture<ContentPacket<R>> future) {
        this.serviceNodesManager = serviceNodesManager;
        this.serviceNodesHandler = serviceNodesHandler;
        this.contentPacket = packet;
        this.responseClass = responseClass;
        this.future = future;
    }

//    public static ServiceInvocation<? extends ResponseTransport, ? extends RequestTransport<?>> build(ServiceNodesManager serviceNodesManager, ServiceNodesHandler handler, ContentPacket<? extends RequestTransport<?>> packet, Class<? extends ResponseTransport> responseClass) {
//        ServiceInvocation<R, K> invocation = new ServiceInvocation<R, K>(serviceNodesManager, packet, responseClass);
//        invocation.serviceNodesHandler = handler;
//        return invocation;
//    }

    public void sendContentPacket(String serviceKey) throws IOException {
        List<NodeConnectivityHandler> connectivityHandlers = serviceNodesHandler.connectedConnectivityHandlers;
        if(connectivityHandlers.isEmpty()) {
            throw new IOException("connectedConnectivityHandlers: No available service node for serviceKey " + serviceKey + " to send " + contentPacket);
        }
//        this.contentPacket = packet;
//        this.responseClass = responseClass;
        this.serviceKey = serviceKey;
//        this.future = future;
        pendingConnectivityHandlers = new ArrayList<>(connectivityHandlers);
        send();
    }

    private void send() {
        if (!pendingConnectivityHandlers.isEmpty()) {
            int index = serviceNodesManager.random.nextInt(pendingConnectivityHandlers.size());
            connectivityHandler = pendingConnectivityHandlers.get(index);
            if(connectivityHandler != null && connectivityHandler.getCurrentState() == NodeConnectivityHandler.STATE_CONNECTED) {
                CompletableFuture<ContentPacket<R>> theFuture = connectivityHandler.sendContentPacket(contentPacket, responseClass, serviceKey);
                theFuture.thenAccept(future::complete).exceptionally(this::sendAgain);
            } else {
                pendingConnectivityHandlers.remove(connectivityHandler);
            }
        } else {
            future.completeExceptionally(new IOException("pendingConnectivityHandlers: No available nodes for serviceKey " + serviceKey + " to send " + contentPacket));
        }
    }

    private Void sendAgain(Throwable throwable) {
        if(connectivityHandler != null) {
            pendingConnectivityHandlers.remove(connectivityHandler);
        }
        send();
        return null;
    }

//    private NodeConnectivityHandler selectBestConnectivityHandler() {
//    }

//    public NodeConnectivityHandler getActualNodeConnectivityHandler() {
//        return cachedConnectivityHandler;
//    }

}
