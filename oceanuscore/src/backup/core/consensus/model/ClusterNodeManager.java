package core.consensus.model;

import core.consensus.model.data.ClusterActionRequest;
import core.net.data.ResponseTransport;

import java.util.concurrent.CompletableFuture;

public abstract class ClusterNodeManager<Node extends ClusterNode, ActionRequest extends ClusterActionRequest> {
    protected Node currentNode;
    protected NodeCommunicationListener nodeCommunicator;
    protected NodeStorageListener nodeStorageHandler;

    public void start(Node node) {
        currentNode = node;
    }

    public void stop() {

    }

    public abstract CompletableFuture<ResponseTransport> executeAction(ActionRequest action);
    public abstract ClusterNodeManager<Node, ActionRequest> addNode(Node node);
    public abstract void removeNode(long serverIdCRC);

    public NodeCommunicationListener getNodeCommunicator() {
        return nodeCommunicator;
    }

    public void setNodeCommunicator(NodeCommunicationListener nodeCommunicator) {
        this.nodeCommunicator = nodeCommunicator;
    }

    public NodeStorageListener getNodeStorageHandler() {
        return nodeStorageHandler;
    }

    public void setNodeStorageHandler(NodeStorageListener nodeStorageHandler) {
        this.nodeStorageHandler = nodeStorageHandler;
    }
}
