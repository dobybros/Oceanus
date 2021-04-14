package core.discovery.node;

import java.util.List;

/**
 * Every server will have one node, stand for the connectivity information for itself.
 *
 * Currently only support IPv4
 */
public class Node {
    /**
     * Node's unique server name, generated from @NetworkCommunicatorFactory#getServerName
     */
    private String serverName;
    /**
     * Every server may discovery multiple ips, like multiple intranet ips and public ips
     * All the ips will provide to client to try the connectivity speed and choose the best one.
     */
    private List<String> ips;
    /**
     * udp port
     * Specified port need open to public internet, otherwise please specify the port to -1, then hole punching will start.
     */
    private int port;

    /**
     * RPC port
     */
    private int rpcPort;

    /**
     * Need hole punching or not.
     */
    private boolean needHolePunching;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isNeedHolePunching() {
        return needHolePunching;
    }

    public void setNeedHolePunching(boolean needHolePunching) {
        this.needHolePunching = needHolePunching;
    }

    public int getRpcPort() {
        return rpcPort;
    }

    public void setRpcPort(int rpcPort) {
        this.rpcPort = rpcPort;
    }
}
