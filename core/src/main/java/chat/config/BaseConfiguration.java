package chat.config;

import chat.utils.ChatUtils;
import lombok.Data;
import script.core.runtime.AbstractRuntimeContext;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
@Data
public class BaseConfiguration {
    private final String TAG = BaseConfiguration.class.getName();
    private String server = ChatUtils.generateFixedRandomString();
    private String mongoHost;
    private String mongoConnectionsPerHost;
    private String dbName;
    private String logsDBName;
    private String configDBName;
    private String mongoUsername;
    private String mongoPassword;
    private String redisHost;

    private String gridHost;
    private String girdConnectionsPerHost;
    private String gridDbName;
    private String gridUsername;
    private String gridPassword;

    private String ipPrefix;
    private String ethPrefix;
    private Integer type;
    /**
     * container's serverType, such as gdmid
     */
    private String serverType;
    private String internalKey;
    private Integer rpcPort;
    private Integer sslRpcPort;
    private String publicDomain;
    private String rpcSslClientTrustJksPath;
    private String rpcSslServerJksPath;
    private String rpcSslJksPwd;
    /**
     * used to storage service source code
     */
    private String localPath;
    private String remotePath;
    private String runtimeBootClass;
    private Integer serverPort;
    private Long maxUsers;
    private Boolean hotDeployment = true;
    private Boolean killProcess = false;
    private String fileBucket;
    private String dockerName;
    private String scaleInstanceId;
    private String lanId;
    private Boolean useHulkAdmin = false;
    private String libsPath;
    private String mavenSettingsPath;

    private Integer upstreamPort;
    private String keystorePwd;
    private String keystorePath;
    private String keymanagerPwd;
    private Integer upstreamSslPort;
    private Integer upstreamWsPort;
    private Integer publicWsPort;
    private Boolean useProxy;
    private Long maxUserNumber;
    private Properties extraProperties;
    public String getDefaultConfigFileName(){
        return "config.properties";
    }
    public String getDefaultConfigDependencyFileName(){
        return "config_dependencies";
    }

    private Map<String, AbstractRuntimeContext> serviceConfiguration = new ConcurrentHashMap<>();
    private ReentrantReadWriteLock runtimeLock = new ReentrantReadWriteLock();
    public void addRuntimeContext(String service, AbstractRuntimeContext runtimeContext){
        try {
            runtimeLock.writeLock().lock();
            AbstractRuntimeContext oldRuntimeContext = serviceConfiguration.put(service, runtimeContext);
            if(oldRuntimeContext != null){
                oldRuntimeContext.close();
            }
        }finally {
            runtimeLock.writeLock().unlock();
        }
    }

    public AbstractRuntimeContext getRuntimeContext(String service){
        try {
            runtimeLock.readLock().lock();
            return serviceConfiguration.get(service);
        }finally {
            runtimeLock.readLock().unlock();
        }
    }

    public AbstractRuntimeContext removeRuntimeContext(String service){
        try {
            runtimeLock.writeLock().lock();
            return serviceConfiguration.remove(service);
        }finally {
            runtimeLock.writeLock().unlock();
        }
    }

    public void close(){
        serviceConfiguration.values().forEach(AbstractRuntimeContext::close);
        serviceConfiguration.clear();
    }

    @Override
    public String toString() {
        return "BaseConfiguration{" +
                "server='" + server + '\'' +
                ", mongoHost='" + mongoHost + '\'' +
                ", mongoConnectionsPerHost='" + mongoConnectionsPerHost + '\'' +
                ", dbName='" + dbName + '\'' +
                ", logsDBName='" + logsDBName + '\'' +
                ", configDBName='" + configDBName + '\'' +
                ", mongoUsername='" + mongoUsername + '\'' +
                ", mongoPassword='" + mongoPassword + '\'' +
                ", redisHost='" + redisHost + '\'' +
                ", gridHost='" + gridHost + '\'' +
                ", girdConnectionsPerHost='" + girdConnectionsPerHost + '\'' +
                ", gridDbName='" + gridDbName + '\'' +
                ", gridUsername='" + gridUsername + '\'' +
                ", gridPassword='" + gridPassword + '\'' +
                ", ipPrefix='" + ipPrefix + '\'' +
                ", ethPrefix='" + ethPrefix + '\'' +
                ", type=" + type +
                ", serverType='" + serverType + '\'' +
                ", internalKey='" + internalKey + '\'' +
                ", rpcPort=" + rpcPort +
                ", sslRpcPort=" + sslRpcPort +
                ", publicDomain='" + publicDomain + '\'' +
                ", rpcSslClientTrustJksPath='" + rpcSslClientTrustJksPath + '\'' +
                ", rpcSslServerJksPath='" + rpcSslServerJksPath + '\'' +
                ", rpcSslJksPwd='" + rpcSslJksPwd + '\'' +
                ", localPath='" + localPath + '\'' +
                ", remotePath='" + remotePath + '\'' +
                ", runtimeBootClass='" + runtimeBootClass + '\'' +
                ", serverPort=" + serverPort +
                ", maxUsers=" + maxUsers +
                ", hotDeployment=" + hotDeployment +
                ", killProcess=" + killProcess +
                ", fileBucket='" + fileBucket + '\'' +
                ", dockerName='" + dockerName + '\'' +
                ", scaleInstanceId='" + scaleInstanceId + '\'' +
                ", lanId='" + lanId + '\'' +
                ", useHulkAdmin=" + useHulkAdmin +
                ", libsPath='" + libsPath + '\'' +
                ", mavenSettingsPath='" + mavenSettingsPath + '\'' +
                ", upstreamPort=" + upstreamPort +
                ", keystorePwd='" + keystorePwd + '\'' +
                ", keystorePath='" + keystorePath + '\'' +
                ", keymanagerPwd='" + keymanagerPwd + '\'' +
                ", upstreamSslPort=" + upstreamSslPort +
                ", upstreamWsPort=" + upstreamWsPort +
                ", publicWsPort=" + publicWsPort +
                ", useProxy=" + useProxy +
                ", maxUserNumber=" + maxUserNumber +
                ", extraProperties=" + extraProperties +
                '}';
    }
}
