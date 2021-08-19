package oceanus.sdk.core.discovery;

import oceanus.sdk.core.common.InternalTools;
import oceanus.sdk.core.discovery.impl.client.ServiceRuntime;
import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.core.discovery.node.ServiceNodeResult;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.utils.state.StateListener;
import oceanus.sdk.utils.state.StateMachine;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Every node enter StarFish through this class.
 *
 */
public abstract class NodeRegistrationHandler {
    /**
     * local node information, like ips and port.
     */
    private Node node;
    protected InternalTools internalTools;
    public static final int CONNECTIVITY_STATE_NONE = 1;
    public static final int CONNECTIVITY_STATE_TERMINATED = -200;
    public static final int CONNECTIVITY_STATE_INITIALIZING = 10;
    public static final int CONNECTIVITY_STATE_HOLE_PUNCHING = 20;
    public static final int CONNECTIVITY_STATE_CONNECTED = 100;
    public static final int CONNECTIVITY_STATE_DISCONNECTED = -100;
    public static final int CONNECTIVITY_STATE_RECONNECTING = 88;
    protected StateMachine<Integer, NodeRegistrationHandler> connectivityState;
    /**
     * Start node to register to Aquaman registration center to join the Starfish network.
     * Node means current server, can only be started once.
     * Node communicate with Aquaman must go through public ip, because need geolocation detection.
     *
     * Jobs in this method,
     * 1, Start udp server on one specified port or hole punching port. Every read/write will go through this port.
     * 2, Register current node into Starfish network with public key's fingerprints.
     * 3, Choose a fastest aquamanDomain, aquaman[number].seastarnet.cn, like aquaman3.seastarnet.cn is the fastest aquaman node, current node will connect with it.
     * 4, Health ping/pong with Aquaman node.
     *  @param discoveryHosts
     * @param nodeFingerprints SHA-256 fingerprints generate for the node, need send to Aquaman node to retrieve private key for decrypting messages from this node.
 *                              Each node need to use authorised public key to join Starfish network. Every communication to Aquaman need encrypt by the public key.
     * @param nodePublicKey the public key for encrypting messages from this node during transfer.
     * @return
     */
    public abstract CompletableFuture<NodeRegistrationHandler> startNode(String discoveryHosts, String rpcIp, int rpcPort);
    public abstract void init(int publicUdpPort);
    /**
     * Stop from Starfish network
     */
    public abstract void stopNode();

    public abstract CompletableFuture<ServiceRuntime> registerService(Service service);
    public abstract CompletableFuture<ServiceNodeResult> getNodesWithServices(Collection<String> services, Collection<Long> checkNodesAvailability, boolean onlyNodeServerCRC);
    public abstract CompletableFuture<Node> getNodeByServerCRCId(Long serverCRCId);
    /**
     * Unregister service from Starfish network.
     *
     * @param service
     */
    public abstract NodeRegistrationHandler unregisterService(String service);

    public abstract <K extends RequestTransport<R>, R extends ResponseTransport> CompletableFuture<ContentPacket<R>> sendContentPacket(ContentPacket<K> packet, Class<R> responseClass, String serviceKey);

    public interface NodeEventListener {
        public int NODE_EVENT_ADDED = 1;
        public int NODE_EVENT_DELETED = 2;

        void nodeChanged(int nodeEvent, Node node);
    }

    /**
     * Watch node changed events for public services.
     * Will build the watch relationship between service and one or more @NodeEventListener independently.
     *
     * @param services
     * @param nodeEventListener
     */
    public abstract NodeRegistrationHandler watchNodeEventsForPublicServices(List<String> services, NodeEventListener nodeEventListener);

    /**
     * Remove the watch relationship between service and @NodeEventListener.
     * If nodeEventListener is null, all the @NodeEventListener associated with the service will be removed.
     *
     * @param services
     * @param nodeEventListener
     */
    public abstract NodeRegistrationHandler unwatchNodeEventsForPublicServices(List<String> services, NodeEventListener nodeEventListener);

    public abstract NetworkCommunicator getConnectedNetworkCommunicator();

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public NodeRegistrationHandler addStateListener(StateListener<Integer, NodeRegistrationHandler> stateListener) {
        if(connectivityState != null ) connectivityState.addStateListener(stateListener);
        return this;
    }

    public void removeStateListener(StateListener<Integer, NodeRegistrationHandler> stateListener) {
        if(connectivityState != null ) connectivityState.removeStateListener(stateListener);
    }

    public Map<String, Object> memory() {
        return null;
    }
}
