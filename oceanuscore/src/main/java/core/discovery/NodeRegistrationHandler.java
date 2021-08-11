package core.discovery;

import core.common.InternalTools;
import core.discovery.impl.client.ServiceRuntime;
import core.discovery.node.Node;
import core.discovery.node.Service;
import core.discovery.node.ServiceNodeResult;
import script.utils.state.StateListener;
import script.utils.state.StateMachine;

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
    public abstract CompletableFuture<NodeRegistrationHandler> startNode(String discoveryHosts);
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
