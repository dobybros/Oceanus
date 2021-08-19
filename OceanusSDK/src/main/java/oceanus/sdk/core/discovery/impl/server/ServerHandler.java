package oceanus.sdk.core.discovery.impl.server;

import oceanus.sdk.core.discovery.DiscoveryManager;

public abstract class ServerHandler {
    protected DiscoveryManager discoveryManager;
    protected ServerHandler(DiscoveryManager discoveryManager) {
        this.discoveryManager = discoveryManager;
    }
}
