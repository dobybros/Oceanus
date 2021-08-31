package core.discovery.data.discovery;

import core.net.data.ResponseTransport;

import java.util.List;

public class GetAllServicesResponse extends ResponseTransport {
    private List<String> services;

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
