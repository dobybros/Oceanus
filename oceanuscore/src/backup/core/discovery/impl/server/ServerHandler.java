package core.discovery.impl.server;

import core.discovery.DiscoveryManager;

public abstract class ServerHandler {
    protected DiscoveryManager discoveryManager;
    protected ServerHandler(DiscoveryManager discoveryManager) {
        this.discoveryManager = discoveryManager;
    }
}
