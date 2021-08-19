package core.discovery.data.discovery;

import core.discovery.node.Service;
import core.net.data.RequestTransport;

public class ServiceRegistrationRequest extends RequestTransport<ServiceRegistrationResponse> {
    private Service service;

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
