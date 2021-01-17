package chat.config;

import chat.utils.ChatUtils;
import script.core.runtime.AbstractRuntimeContext;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class BaseConfiguration {
    private final String TAG = BaseConfiguration.class.getName();
    private static String oceanusConfigPath = "oceanus.properties";
    private static int httpThreadPoolSize = 500;

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

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getMongoHost() {
        return mongoHost;
    }

    public void setMongoHost(String mongoHost) {
        this.mongoHost = mongoHost;
    }

    public String getMongoConnectionsPerHost() {
        return mongoConnectionsPerHost;
    }

    public void setMongoConnectionsPerHost(String mongoConnectionsPerHost) {
        this.mongoConnectionsPerHost = mongoConnectionsPerHost;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getLogsDBName() {
        return logsDBName;
    }

    public void setLogsDBName(String logsDBName) {
        this.logsDBName = logsDBName;
    }

    public String getConfigDBName() {
        return configDBName;
    }

    public void setConfigDBName(String configDBName) {
        this.configDBName = configDBName;
    }

    public String getMongoUsername() {
        return mongoUsername;
    }

    public void setMongoUsername(String mongoUsername) {
        this.mongoUsername = mongoUsername;
    }

    public String getMongoPassword() {
        return mongoPassword;
    }

    public void setMongoPassword(String mongoPassword) {
        this.mongoPassword = mongoPassword;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public String getGridHost() {
        return gridHost;
    }

    public void setGridHost(String gridHost) {
        this.gridHost = gridHost;
    }

    public String getGirdConnectionsPerHost() {
        return girdConnectionsPerHost;
    }

    public void setGirdConnectionsPerHost(String girdConnectionsPerHost) {
        this.girdConnectionsPerHost = girdConnectionsPerHost;
    }

    public String getGridDbName() {
        return gridDbName;
    }

    public void setGridDbName(String gridDbName) {
        this.gridDbName = gridDbName;
    }

    public String getGridUsername() {
        return gridUsername;
    }

    public void setGridUsername(String gridUsername) {
        this.gridUsername = gridUsername;
    }

    public String getGridPassword() {
        return gridPassword;
    }

    public void setGridPassword(String gridPassword) {
        this.gridPassword = gridPassword;
    }

    public String getIpPrefix() {
        return ipPrefix;
    }

    public void setIpPrefix(String ipPrefix) {
        this.ipPrefix = ipPrefix;
    }

    public String getEthPrefix() {
        return ethPrefix;
    }

    public void setEthPrefix(String ethPrefix) {
        this.ethPrefix = ethPrefix;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getInternalKey() {
        return internalKey;
    }

    public void setInternalKey(String internalKey) {
        this.internalKey = internalKey;
    }

    public Integer getRpcPort() {
        return rpcPort;
    }

    public void setRpcPort(Integer rpcPort) {
        this.rpcPort = rpcPort;
    }

    public Integer getSslRpcPort() {
        return sslRpcPort;
    }

    public void setSslRpcPort(Integer sslRpcPort) {
        this.sslRpcPort = sslRpcPort;
    }

    public String getPublicDomain() {
        return publicDomain;
    }

    public void setPublicDomain(String publicDomain) {
        this.publicDomain = publicDomain;
    }

    public String getRpcSslClientTrustJksPath() {
        return rpcSslClientTrustJksPath;
    }

    public void setRpcSslClientTrustJksPath(String rpcSslClientTrustJksPath) {
        this.rpcSslClientTrustJksPath = rpcSslClientTrustJksPath;
    }

    public String getRpcSslServerJksPath() {
        return rpcSslServerJksPath;
    }

    public void setRpcSslServerJksPath(String rpcSslServerJksPath) {
        this.rpcSslServerJksPath = rpcSslServerJksPath;
    }

    public String getRpcSslJksPwd() {
        return rpcSslJksPwd;
    }

    public void setRpcSslJksPwd(String rpcSslJksPwd) {
        this.rpcSslJksPwd = rpcSslJksPwd;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getRuntimeBootClass() {
        return runtimeBootClass;
    }

    public void setRuntimeBootClass(String runtimeBootClass) {
        this.runtimeBootClass = runtimeBootClass;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public Long getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Long maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Boolean getHotDeployment() {
        return hotDeployment;
    }

    public void setHotDeployment(Boolean hotDeployment) {
        this.hotDeployment = hotDeployment;
    }

    public Boolean getKillProcess() {
        return killProcess;
    }

    public void setKillProcess(Boolean killProcess) {
        this.killProcess = killProcess;
    }

    public String getFileBucket() {
        return fileBucket;
    }

    public void setFileBucket(String fileBucket) {
        this.fileBucket = fileBucket;
    }

    public String getDockerName() {
        return dockerName;
    }

    public void setDockerName(String dockerName) {
        this.dockerName = dockerName;
    }

    public String getScaleInstanceId() {
        return scaleInstanceId;
    }

    public void setScaleInstanceId(String scaleInstanceId) {
        this.scaleInstanceId = scaleInstanceId;
    }

    public String getLanId() {
        return lanId;
    }

    public void setLanId(String lanId) {
        this.lanId = lanId;
    }

    public Boolean getUseHulkAdmin() {
        return useHulkAdmin;
    }

    public void setUseHulkAdmin(Boolean useHulkAdmin) {
        this.useHulkAdmin = useHulkAdmin;
    }

    public String getLibsPath() {
        return libsPath;
    }

    public void setLibsPath(String libsPath) {
        this.libsPath = libsPath;
    }

    public String getMavenSettingsPath() {
        return mavenSettingsPath;
    }

    public void setMavenSettingsPath(String mavenSettingsPath) {
        this.mavenSettingsPath = mavenSettingsPath;
    }

    public Integer getUpstreamPort() {
        return upstreamPort;
    }

    public void setUpstreamPort(Integer upstreamPort) {
        this.upstreamPort = upstreamPort;
    }

    public String getKeystorePwd() {
        return keystorePwd;
    }

    public void setKeystorePwd(String keystorePwd) {
        this.keystorePwd = keystorePwd;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeymanagerPwd() {
        return keymanagerPwd;
    }

    public void setKeymanagerPwd(String keymanagerPwd) {
        this.keymanagerPwd = keymanagerPwd;
    }

    public Integer getUpstreamSslPort() {
        return upstreamSslPort;
    }

    public void setUpstreamSslPort(Integer upstreamSslPort) {
        this.upstreamSslPort = upstreamSslPort;
    }

    public Integer getUpstreamWsPort() {
        return upstreamWsPort;
    }

    public void setUpstreamWsPort(Integer upstreamWsPort) {
        this.upstreamWsPort = upstreamWsPort;
    }

    public Integer getPublicWsPort() {
        return publicWsPort;
    }

    public void setPublicWsPort(Integer publicWsPort) {
        this.publicWsPort = publicWsPort;
    }

    public Boolean getUseProxy() {
        return useProxy;
    }

    public void setUseProxy(Boolean useProxy) {
        this.useProxy = useProxy;
    }

    public Long getMaxUserNumber() {
        return maxUserNumber;
    }

    public void setMaxUserNumber(Long maxUserNumber) {
        this.maxUserNumber = maxUserNumber;
    }

    public Properties getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Properties extraProperties) {
        this.extraProperties = extraProperties;
    }

    public static String getOceanusConfigPath() {
        return oceanusConfigPath;
    }

    public static void setOceanusConfigPath(String oceanusConfigPath) {
        BaseConfiguration.oceanusConfigPath = oceanusConfigPath;
    }

    public static int getHttpThreadPoolSize() {
        return httpThreadPoolSize;
    }

    public static void setHttpThreadPoolSize(int httpThreadPoolSize) {
        BaseConfiguration.httpThreadPoolSize = httpThreadPoolSize;
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
                ", oceanusConfigPath=" + BaseConfiguration.oceanusConfigPath +
                ", httpThreadPoolSize=" + BaseConfiguration.httpThreadPoolSize +
                '}';
    }
}
