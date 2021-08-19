package oceanus.sdk.core.discovery.node;

import java.util.List;
import java.util.Map;

public class ServiceNodeResult {
    private Map<String, List<Long>> serviceNodeCRCIds;
    private Map<String, List<Node>> serviceNodes;

    private List<Long> deadNodes;

    public Map<String, List<Long>> getServiceNodeCRCIds() {
        return serviceNodeCRCIds;
    }

    public void setServiceNodeCRCIds(Map<String, List<Long>> serviceNodeCRCIds) {
        this.serviceNodeCRCIds = serviceNodeCRCIds;
    }

    public Map<String, List<Node>> getServiceNodes() {
        return serviceNodes;
    }

    public void setServiceNodes(Map<String, List<Node>> serviceNodes) {
        this.serviceNodes = serviceNodes;
    }

    public List<Long> getDeadNodes() {
        return deadNodes;
    }

    public void setDeadNodes(List<Long> deadNodes) {
        this.deadNodes = deadNodes;
    }
}
