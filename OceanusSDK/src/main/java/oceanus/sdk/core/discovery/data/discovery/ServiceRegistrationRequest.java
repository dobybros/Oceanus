package oceanus.sdk.core.discovery.data.discovery;


import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.core.net.data.RequestTransport;

public class ServiceRegistrationRequest extends RequestTransport<ServiceRegistrationResponse> {
    private Service service;

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
