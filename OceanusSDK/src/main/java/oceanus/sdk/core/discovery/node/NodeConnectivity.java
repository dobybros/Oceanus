package oceanus.sdk.core.discovery.node;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

/**
 * Each client will choose the best ip to connect to a @ServerNode
 * This class record the information for best connectivity
 */
public class NodeConnectivity {
    public void reset() {
        ip = null;
        availableIps = null;
    }

    public class IPWithLatency {
        public IPWithLatency(String ip, long latency) {
            this.ip = ip;
            this.latency = latency;
        }
        private String ip;
        private long latency;
        private InetSocketAddress socketAddress;

        public InetSocketAddress getSocketAddress() {
            if(socketAddress == null) {
                socketAddress = new InetSocketAddress(ip, node.getPort());
            }
            return socketAddress;
        }
    }
    private IPWithLatency ip;
    private HashMap<String, IPWithLatency> availableIps;

    private Node node;

    public NodeConnectivity(Node node) {
        this.node = node;
    }

    public synchronized void registerIp(String ip, long latency) {
        if(this.ip == null) {
            this.ip = new IPWithLatency(ip, latency);
        } else {
            addAvailableIp(ip, latency);
        }
    }

    public synchronized void addAvailableIp(String ip, long latency) {
        if(availableIps == null) {
            availableIps = new HashMap<>();
        }
        if(!availableIps.containsKey(ip)) {
            availableIps.put(ip, new IPWithLatency(ip, latency));
        }
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public IPWithLatency getIp() {
        return ip;
    }

    public void registerIp(IPWithLatency ip) {
        this.ip = ip;
    }

    public HashMap<String, IPWithLatency> getAvailableIps() {
        return availableIps;
    }

    public void setAvailableIps(HashMap<String, IPWithLatency> availableIps) {
        this.availableIps = availableIps;
    }
}
