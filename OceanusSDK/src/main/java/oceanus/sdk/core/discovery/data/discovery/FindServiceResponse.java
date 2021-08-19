package oceanus.sdk.core.discovery.data.discovery;

import oceanus.sdk.core.discovery.node.ServiceNodeResult;
import oceanus.sdk.core.net.data.ResponseTransport;

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
