package core.discovery.data.discovery;

import core.discovery.node.Node;
import core.discovery.node.ServiceNodeResult;
import core.net.data.ResponseTransport;

import java.util.List;

public class FindServiceResponse extends ResponseTransport {
    private ServiceNodeResult serviceNodeResult;

    public ServiceNodeResult getServiceNodeResult() {
        return serviceNodeResult;
    }

    public void setServiceNodeResult(ServiceNodeResult serviceNodeResult) {
        this.serviceNodeResult = serviceNodeResult;
    }
}
