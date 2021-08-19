package core.discovery.data.discovery;

import core.discovery.node.Node;
import core.net.data.ResponseTransport;

public class GetNodeByServerCRCIdResponse extends ResponseTransport {
    private Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
