package chat.config;

import java.io.File;
import java.util.Properties;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class Configuration {
    /**
     * service's name, such as tcuser
     */
    private String service;
    /**
     * service's version which spcified by deployserviceversion
     */
    private Integer version;
    /**
     * service's develop language
     */
    private String languageType = LANGEUAGE_GROOVY;
    public static final String LANGEUAGE_GROOVY = "Groovy";
    public static final String LANGEUAGE_JAVA = "Java";
    public static final String LANGEUAGE_JAVA_JAR = "Jar";
    //service's config
    private Properties config;
    //local source's path,such as ../local/gdim/groovy
    private String localPath;
    //used to hot deployment
    private Long deployVersion;

    private String localDependencyLibsPath;
    //groovycloud's properties
    private BaseConfiguration baseConfiguration;

    public String getServiceVersion(){
        return service + "_v" + version;
    }

    public String getFileName(){
        switch (languageType){
            case LANGEUAGE_JAVA:
                return "java.zip";
            case LANGEUAGE_JAVA_JAR:
                return "java.jar";
            default:return "groovy.zip";
        }
    }

    public String[] getExcludeDependencies(){
        if (LANGEUAGE_GROOVY.equals(languageType)) {
            return new String[]{"log4j", "slf4j"};
        }
        return null;
    }

    public void setBaseConfiguration(BaseConfiguration baseConfiguration){
        this.baseConfiguration = baseConfiguration;
        this.localPath = baseConfiguration.getLocalPath() + File.separator
                + baseConfiguration.getDockerName() + File.separator
                + getServiceVersion() +File.separator + languageType;
    }

    @Override
    public String toString() {
        return "service='" + service + '\'' +
                ", version=" + version +
                ", languageType='" + languageType + '\'' +
                ", config=" + config;
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

    public String getLanguageType() {
        return languageType;
    }

    public void setLanguageType(String languageType) {
        this.languageType = languageType;
    }

    public Properties getConfig() {
        return config;
    }

    public void setConfig(Properties config) {
        this.config = config;
    }

    public String getLocalPath() {
        return localPath;
    }

    public Long getDeployVersion() {
        return deployVersion;
    }

    public void setDeployVersion(Long deployVersion) {
        this.deployVersion = deployVersion;
    }

    public String getLocalDependencyLibsPath() {
        return localDependencyLibsPath;
    }

    public void setLocalDependencyLibsPath(String localDependencyLibsPath) {
        this.localDependencyLibsPath = localDependencyLibsPath;
    }

    public BaseConfiguration getBaseConfiguration() {
        return baseConfiguration;
    }
}
