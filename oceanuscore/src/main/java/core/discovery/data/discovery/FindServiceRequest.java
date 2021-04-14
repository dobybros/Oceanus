package core.discovery.data.discovery;

import core.log.LoggerHelper;
import core.net.data.RequestTransport;

public class FindServiceRequest extends RequestTransport<FindServiceResponse> {
    private String owner;
    private String project;
    private String service;
    private Integer version;

    public void setServiceKey(String serviceKey) {
        String[] parts = serviceKey.split("_");
        if(parts.length == 3) {
            owner = parts[0];
            project = parts[1];
            service = parts[2];
        } else {
            LoggerHelper.logger.warn("Illegal service key " + serviceKey);
        }
    }

    public String generateServiceKey() {
        return owner + "_" + project + "_" + service;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
