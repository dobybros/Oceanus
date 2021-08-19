package oceanus.sdk.core.discovery;

import oceanus.sdk.core.discovery.node.Node;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class DiscoveryInfo {
        private ConcurrentHashMap<String, ConcurrentSkipListSet<Long>> serviceNodesMap;
        private ConcurrentHashMap<Long, Node> nodeMap;
        public DiscoveryInfo(ConcurrentHashMap<String, ConcurrentSkipListSet<Long>> serviceNodesMap, ConcurrentHashMap<Long, Node> nodeMap) {
            this.serviceNodesMap = serviceNodesMap;
            this.nodeMap = nodeMap;
        }
        public ConcurrentHashMap<String, ConcurrentSkipListSet<Long>> getServiceNodesMap() {
            return serviceNodesMap;
        }

        public void setServiceNodesMap(ConcurrentHashMap<String, ConcurrentSkipListSet<Long>> serviceNodesMap) {
            this.serviceNodesMap = serviceNodesMap;
        }

        public ConcurrentHashMap<Long, Node> getNodeMap() {
            return nodeMap;
        }

        public void setNodeMap(ConcurrentHashMap<Long, Node> nodeMap) {
            this.nodeMap = nodeMap;
        }
    }