package core.discovery.data.discovery;

import core.net.data.ResponseTransport;

import java.util.List;

public class FindServiceResponse extends ResponseTransport {
    private List<Long> nodeServers;

    public List<Long> getNodeServers() {
        return nodeServers;
    }

    public void setNodeServers(List<Long> nodeServers) {
        this.nodeServers = nodeServers;
    }
}
