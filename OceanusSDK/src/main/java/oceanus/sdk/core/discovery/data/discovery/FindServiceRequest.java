package oceanus.sdk.core.discovery.data.discovery;

import oceanus.sdk.core.net.data.RequestTransport;

import java.util.Collection;
import java.util.List;

public class FindServiceRequest extends RequestTransport<FindServiceResponse> {
    private boolean onlyNodeServerCRC;
    private Collection<String> services;
    private Collection<Long> checkNodesAvailability;

    public boolean isOnlyNodeServerCRC() {
        return onlyNodeServerCRC;
    }

    public void setOnlyNodeServerCRC(boolean onlyNodeServerCRC) {
        this.onlyNodeServerCRC = onlyNodeServerCRC;
    }

    public Collection<String> getServices() {
        return services;
    }

    public void setServices(Collection<String> services) {
        this.services = services;
    }

    public Collection<Long> getCheckNodesAvailability() {
        return checkNodesAvailability;
    }

    public void setCheckNodesAvailability(Collection<Long> checkNodesAvailability) {
        this.checkNodesAvailability = checkNodesAvailability;
    }
}
