package chat.config;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
@Getter
@Setter
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
}
