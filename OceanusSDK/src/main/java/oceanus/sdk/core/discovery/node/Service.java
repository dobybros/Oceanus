package oceanus.sdk.core.discovery.node;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Service belongs to Business layer and will only run on Tentacle node.
 * Multiple services can be running on one node.
 *
 * Service currently support embedded mode, like Groovy and Java. Don't support hot deploy for embedded.
 * Later planning to use sub-process to run language like Go or others. Then can fully support hot deploy. So light weight language is a must.
 */
public class Service {
    public static final int TYPE_JAVA = 1;
    public static final int TYPE_EMBEDDED_GROOVY = 10;
    public static final int TYPE_EMBEDDED_JAVA = 20;
    //EXTERNAL >= 100, 10 <= EMBEDDED < 100
    public static final int TYPE_EXTERNAL_GO = 100;
    private String service; //Service id
    private String serviceSuffix; //${service}_${serviceSuffix} combine as final service id
    private Integer version;
    private Integer minVersion;
    private Long uploadTime;
    private Integer type;

    public static final int STATUS_WILL_DEPLOY = 1;
    public static final int STATUS_DEPLOYED = 10;
    public static final int STATUS_DEPLOY_FAILED = -10;
    private int status;
//    private List<ServiceAnnotation> serviceAnnotationList;
//    private Long longitude, latitude;
//    private String country;

//    private String owner; //Belongs to which whom, use owner's CA to register.
//    private String project; //Belongs to which project

    public String toString() {
        return "Service: service " + service + "; " +
                "version " + version + "; " +
                "minVersion " + minVersion + "; " +
                "uploadTime " + uploadTime + "; " +
                "serviceSuffix " + serviceSuffix + "; " +
                "status " + status + "; " +
                "type " + type  + "; " /*+
                "owner " + owner + "; " +
                "project " + project + "; "*/;
    }

    public String generateServiceKey() {
        if(StringUtils.isBlank(serviceSuffix)) {
            return service;
        }
        return service + "_" + serviceSuffix;
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

    public Long getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Long uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(Integer minVersion) {
        this.minVersion = minVersion;
    }
//    public List<ServiceAnnotation> getServiceAnnotationList() {
//        return serviceAnnotationList;
//    }
//
//    public void setServiceAnnotationList(List<ServiceAnnotation> serviceAnnotationList) {
//        this.serviceAnnotationList = serviceAnnotationList;
//    }

//    public String getOwner() {
//        return owner;
//    }
//
//    public void setOwner(String owner) {
//        this.owner = owner;
//    }
//
//    public String getProject() {
//        return project;
//    }
//
//    public void setProject(String project) {
//        this.project = project;
//    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getServiceSuffix() {
        return serviceSuffix;
    }

    public void setServiceSuffix(String serviceSuffix) {
        this.serviceSuffix = serviceSuffix;
    }


}
