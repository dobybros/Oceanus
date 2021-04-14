package core.consensus.gossip;

import core.consensus.model.ClusterNodeManager;
import core.net.data.ResponseTransport;
import script.utils.state.StateMachine;

import java.util.concurrent.CompletableFuture;

public class GossipClusterNodeManager extends ClusterNodeManager<GossipClusterNode, GossipActionRequest> {
    private StateMachine<Integer, GossipClusterNodeManager> nodeState;

    public static final int STATE_NONE = 1;

    public GossipClusterNodeManager() {
        nodeState = new StateMachine<>(this.getClass().getSimpleName(), STATE_NONE, this );
    }

    @Override
    public CompletableFuture<ResponseTransport> executeAction(GossipActionRequest action) {
        return null;
    }

    @Override
    public ClusterNodeManager<GossipClusterNode, GossipActionRequest> addNode(GossipClusterNode node) {
        return null;
    }

    @Override
    public void removeNode(long serverIdCRC) {

    }
}
