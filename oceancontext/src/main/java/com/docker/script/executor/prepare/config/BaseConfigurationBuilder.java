package com.docker.script.executor.prepare.config;

import chat.config.BaseConfiguration;
import chat.logs.LoggerEx;
import com.docker.storage.kafka.BaseKafkaConfCenter;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class BaseConfigurationBuilder {
    private final String TAG = BaseConfigurationBuilder.class.getName();
    public BaseConfiguration build(){
        try {
            try (InputStream inStream = BaseConfiguration.class.getClassLoader().getResourceAsStream("oceanus.properties");
                 InputStream appInStream = BaseConfiguration.class.getClassLoader().getResourceAsStream("application.properties");
                 InputStream kafkaProducerInStream = BaseConfiguration.class.getClassLoader().getResourceAsStream("config/kafka/producer.properties");){
                Properties prop = new Properties();
                Properties appProp = new Properties();
                Properties kafkaProducerProp = new Properties();
                Assert.assertNotNull(inStream);
                prop.load(inStream);
                Assert.assertNotNull(appInStream);
                appProp.load(appInStream);
                if(kafkaProducerInStream != null){
                    kafkaProducerProp.load(kafkaProducerInStream);
                    BaseKafkaConfCenter.getInstance().setKafkaConfCenter(kafkaProducerProp, null);
                }
                BaseConfiguration baseConfiguration = new BaseConfiguration();
                baseConfiguration.setMongoHost((String) prop.remove("database.host"));
                baseConfiguration.setMongoConnectionsPerHost((String) prop.remove("connectionsPerHost"));
                baseConfiguration.setDbName((String) prop.remove("dockerstatus.dbname"));
                baseConfiguration.setLogsDBName((String) prop.remove("logs.dbname"));
                baseConfiguration.setConfigDBName((String) prop.remove("config.dbname"));
                baseConfiguration.setMongoUsername((String) prop.remove("mongo.username"));
                baseConfiguration.setMongoPassword((String) prop.remove("mongo.password"));
                baseConfiguration.setGridHost((String) prop.remove("gridfs.host"));
                baseConfiguration.setGirdConnectionsPerHost((String) prop.remove("gridfs.connectionsPerHost"));
                baseConfiguration.setGridDbName((String) prop.remove("gridfs.files.dbname"));
                baseConfiguration.setGridUsername((String) prop.remove("gridfs.username"));
                baseConfiguration.setGridPassword((String) prop.remove("gridfs.password"));

                baseConfiguration.setIpPrefix((String) prop.remove("server.ip.prefix"));
                baseConfiguration.setEthPrefix((String) prop.remove("server.eth.prefix"));
                String type = (String) (String) prop.remove("type");
                if(StringUtils.isNotBlank(type)){
                    baseConfiguration.setType(Integer.valueOf(type));
                }
                baseConfiguration.setServerType((String) prop.remove("server.type"));
                baseConfiguration.setInternalKey((String) prop.remove("internal.key"));
                String rpcPort = (String) prop.remove("rpc.port");
                if(StringUtils.isNotBlank(rpcPort)){
                    baseConfiguration.setRpcPort(Integer.parseInt(rpcPort));
                }
                String rpcSslPort = (String) prop.remove("rpc.sslport");
                if(StringUtils.isNotBlank(rpcSslPort)){
                    baseConfiguration.setSslRpcPort(Integer.parseInt(rpcSslPort));
                }
                baseConfiguration.setFileBucket( (String) prop.remove("gridfs.bucket"));
                baseConfiguration.setPublicDomain((String) prop.remove("public.domain"));
                baseConfiguration.setRpcSslClientTrustJksPath((String) prop.remove("rpc.ssl.clientTrust.jks.path"));
                baseConfiguration.setRpcSslServerJksPath((String) prop.remove("rpc.ssl.server.jks.path"));
                baseConfiguration.setRpcSslJksPwd((String) prop.remove("rpc.ssl.jks.pwd"));
                baseConfiguration.setLocalPath((String) prop.remove("script.local.path"));
                baseConfiguration.setRemotePath((String) prop.remove("script.remote.path"));
                baseConfiguration.setRuntimeBootClass((String) prop.remove("runtimeBootClass"));
                String maxUser = (String) prop.remove("server.max.users");
                if(StringUtils.isNotBlank(maxUser)){
                    baseConfiguration.setMaxUsers(Long.parseLong(maxUser));
                }
                String hotDeployment = (String) prop.remove("hotDeployment");
                if(StringUtils.isNotBlank(hotDeployment)){
                    baseConfiguration.setHotDeployment(Boolean.parseBoolean(hotDeployment));
                }
                String killProcess = (String) prop.remove("killProcess");
                if(StringUtils.isNotBlank(killProcess)){
                    baseConfiguration.setKillProcess(Boolean.parseBoolean(killProcess));
                }
                baseConfiguration.setDockerName((String) prop.remove("docker.name"));
                baseConfiguration.setScaleInstanceId((String) prop.remove("scale.instanceId"));
                baseConfiguration.setRedisHost((String) prop.remove("db.redis.uri"));
                String useHulkAdmin = (String) prop.remove("useHulkAdmin");
                if(StringUtils.isNotBlank(useHulkAdmin)){
                    baseConfiguration.setUseHulkAdmin(Boolean.parseBoolean(useHulkAdmin));
                }
                baseConfiguration.setLanId((String) prop.remove("lan.id"));
                String libsPath = (String)prop.remove("libs.path");
                if(StringUtils.isNotBlank(libsPath)){
                    baseConfiguration.setLibsPath(libsPath);
                }
                baseConfiguration.setMavenSettingsPath((String) prop.remove("maven.settings.path"));
                String serverPort = (String) appProp.remove("server.port");
                if(StringUtils.isNotBlank(serverPort)){
                    baseConfiguration.setServerPort(Integer.parseInt(serverPort));
                }
                String upstreamPort = (String) prop.remove("upstream-port");
                if(StringUtils.isNotBlank(upstreamPort)){
                    baseConfiguration.setUpstreamPort(Integer.parseInt(upstreamPort));
                }
                baseConfiguration.setKeystorePath((String) prop.remove("keystore.pwd"));
                baseConfiguration.setKeystorePath((String) prop.remove("keystore.path"));
                baseConfiguration.setKeymanagerPwd((String) prop.remove("keymanager.pwd"));
                String upstreamSslPort = (String) prop.remove("upstream-ssl-port");
                if(StringUtils.isNotBlank(upstreamSslPort)){
                    baseConfiguration.setUpstreamSslPort(Integer.parseInt(upstreamSslPort));
                }
                String upstreamWsPort = (String) prop.remove("upstream-ws-port");
                if(StringUtils.isNotBlank(upstreamWsPort)){
                    baseConfiguration.setUpstreamWsPort(Integer.parseInt(upstreamWsPort));
                }
                String publicWsPortStr = (String) prop.remove("public-ws-port");
                if(StringUtils.isNotBlank(publicWsPortStr)){
                    baseConfiguration.setPublicWsPort(Integer.parseInt(publicWsPortStr));
                }
                String useProxy = (String) prop.remove("useProxy");
                if(StringUtils.isNotBlank(useProxy)){
                    baseConfiguration.setUseProxy(Boolean.parseBoolean(useProxy));
                }
                String maxUserNumber = (String) prop.remove("maxUserNumber");
                if(StringUtils.isNotBlank(maxUserNumber)){
                    baseConfiguration.setMaxUserNumber(Long.parseLong(maxUserNumber));
                }
                prop.putAll(appProp);
                baseConfiguration.setExtraProperties(prop);
                return baseConfiguration;
            }
        }catch (IOException e){
            LoggerEx.error(TAG, "Get resource failed, errMsg: " + e.getCause());
        }
        return null;
    }
}
