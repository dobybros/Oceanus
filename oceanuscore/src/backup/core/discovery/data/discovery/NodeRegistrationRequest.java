package core.discovery.data.discovery;

import core.discovery.node.Node;
import core.net.data.RequestTransport;

public class NodeRegistrationRequest extends RequestTransport<NodeRegistrationResponse> {
    private Node node;
    private byte[] nodeFingerprints;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public byte[] getNodeFingerprints() {
        return nodeFingerprints;
    }

    public void setNodeFingerprints(byte[] nodeFingerprints) {
        this.nodeFingerprints = nodeFingerprints;
    }
}
