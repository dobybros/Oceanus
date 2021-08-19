package oceanus.sdk.core.discovery.data.discovery;


import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.net.data.ResponseTransport;

public class GetNodeByServerCRCIdResponse extends ResponseTransport {
    private Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
