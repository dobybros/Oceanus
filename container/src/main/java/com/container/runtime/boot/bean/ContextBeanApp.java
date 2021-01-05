package com.container.runtime.boot.bean;

import chat.config.BaseConfiguration;
import chat.utils.IPHolder;
import com.docker.context.ContextFactory;
import com.docker.context.impl.DefaultContextFactory;
import com.docker.file.adapters.GridFSFileHandler;
import com.docker.http.MyHttpParameters;
import com.docker.onlineserver.OnlineServerWithStatus;
import com.docker.rpc.QueueSimplexListener;
import com.docker.rpc.impl.RMIServerHandler;
import com.docker.rpc.impl.RMIServerImplWrapper;
import com.docker.rpc.queue.KafkaSimplexListener;
import com.docker.storage.adapters.impl.*;
import com.docker.storage.mongodb.MongoHelper;
import com.docker.storage.mongodb.daos.*;
import com.docker.storage.redis.RedisListenerHandler;
import com.docker.storage.redis.RedisSubscribeHandler;
import com.docker.storage.zookeeper.ZookeeperFactory;
import com.docker.tasks.RepairTaskHandler;
import com.docker.oceansbean.BeanFactory;
import com.container.runtime.boot.manager.BootManager;
import com.container.runtime.executor.DefaultRuntimeExecutor;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import script.core.runtime.RuntimeFactory;
import script.core.runtime.classloader.ClassLoaderFactory;
import script.core.runtime.classloader.impl.DefaultClassLoaderFactory;
import script.core.runtime.impl.DefaultRuntimeFactory;
import script.core.servlets.RequestPermissionHandler;
import script.filter.JsonFilterFactory;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

//import com.dobybros.chat.log.LogIndexQueue;
//import com.dobybros.chat.storage.mongodb.daos.BulkLogDAO;

/**
 * @Auther: lick
 * @Description:
 * @Date:2019/5/26 15:41
 */

class ContextBeanApp {
    private static final String TAG = ContextBeanApp.class.getSimpleName();
    protected static BaseConfiguration baseConfiguration;
    private PlainSocketFactory plainSocketFactory;
    private SSLSocketFactory sslSocketFactory;
    private DefaultHttpClient httpClient;
    private Scheme httpScheme;
    private Scheme httpsScheme;
    private SchemeRegistry schemeRegistry;
    private ThreadSafeClientConnManager clientConnManager;
    private MongoHelper dockerStatusHelper;
    private MongoHelper logsHelper;
    private MongoHelper configHelper;
    private MongoHelper scheduledTaskHelper;
    private MongoHelper repairHelper;
    private DockerStatusDAO dockerStatusDAO;
    private ServersDAO serversDAO;
    private LansDAO lansDAO;
    private SDockerDAO sdockerDAO;
    private ServiceVersionDAO serviceVersionDAO;
    private DeployServiceVersionDAO deployServiceVersionDAO;
    private GridFSFileHandler fileAdapter;
    private MongoHelper gridfsHelper;
    private DockerStatusServiceImpl dockerStatusService;
    private IPHolder ipHolder;
    private JsonFilterFactory jsonFilterFactory;
    private RequestPermissionHandler requestPermissionHandler;
    private BootManager scriptManager;
    private OnlineServerWithStatus onlineServer;
    private RMIServerImplWrapper rpcServer;
    private RMIServerImplWrapper rpcServerSsl;
    private RMIServerImplWrapper dockerRpcServer;
    private RMIServerHandler dockerRpcServerAdapter;
    private RMIServerImplWrapper dockerRpcServerSsl;
    private RMIServerHandler dockerRpcServerAdapterSsl;
    private ServersServiceImpl serversService;
    private LansServiceImpl lansService;
    private SDockersServiceImpl sDockersService;
    private ServiceVersionServiceImpl serviceVersionService;
    private DeployServiceVersionServiceImpl deployServiceVersionService;
    private ScheduledTaskServiceImpl scheduledTaskService;
    private RepairServiceImpl repairService;
    private ScheduledTaskDAO scheduledTaskDAO;
    private RepairDAO repairDAO;
    private RedisSubscribeHandler redisSubscribeHandler;
    private RepairTaskHandler repairTaskHandler;
    private RedisListenerHandler redisListenerHandler;
    private ZookeeperFactory zookeeperFactory;
    synchronized ZookeeperFactory getZookeeperFactory() {
        if (zookeeperFactory == null) {
            zookeeperFactory = new ZookeeperFactory();
        }
        return zookeeperFactory;
    }

    synchronized RepairTaskHandler getRepairTaskHandler() {
        if (repairTaskHandler == null) {
            repairTaskHandler = new RepairTaskHandler();
        }
        return repairTaskHandler;
    }

    synchronized RedisSubscribeHandler getRedisSubscribeHandler() {
        if (redisSubscribeHandler == null) {
            redisSubscribeHandler = new RedisSubscribeHandler();
        }
        return redisSubscribeHandler;
    }

    private KafkaSimplexListener queueSimplexListener;

    synchronized QueueSimplexListener getQueueSimplexListener() {
        if (queueSimplexListener == null) {
            try {
                queueSimplexListener = new KafkaSimplexListener();
                Map<String, String> config = new HashMap<>();
//                config.put("bootstrap.servers", getKafkaServers());
//                config.put("producer.key.serializer", getKafkaProducerKeySerializer());
//                config.put("producer.value.serializer", getKafkaProducerValueSerializer());
//                config.put("retries", getKafkaProducerRetries());
//                config.put("linger.ms", getKafkaProducerLingerMs());
//                config.put("consumer.key.serializer", getKafkaConsumerKeySerializer());
//                config.put("consumer.value.serializer", getKafkaConsumerValueSerializer());
                queueSimplexListener.setConfig(config);
                queueSimplexListener.setDockerRpcServer(getDockerRpcServer());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return queueSimplexListener;
    }
    synchronized LansServiceImpl getLansService(){
        if(lansService == null){
            lansService = new LansServiceImpl();
            lansService.setLansDAO(getLansDAO());
        }
        return lansService;
    }

    synchronized SDockersServiceImpl getsDockersService(){
        if(sDockersService == null){
            sDockersService = new SDockersServiceImpl();
            sDockersService.setsDockerDAO(getSdockerDAO());
        }
        return sDockersService;
    }
    synchronized ScheduledTaskServiceImpl getScheduledTaskService() {
        if (scheduledTaskService == null) {
            scheduledTaskService = new ScheduledTaskServiceImpl();
            scheduledTaskService.setScheduledTaskDAO(getScheduledTaskDAO());
        }
        return scheduledTaskService;
    }

    synchronized RedisListenerHandler getRedisListenerHandler() {
        if (redisListenerHandler == null) {
            redisListenerHandler = new RedisListenerHandler();
        }
        return redisListenerHandler;
    }

    synchronized RepairServiceImpl getRepairService() {
        if (repairService == null) {
            repairService = new RepairServiceImpl();
            repairService.setRepairDAO(getRepairDAO());
        }
        return repairService;
    }

    private DefaultRuntimeFactory runtimeFactory;
    synchronized DefaultRuntimeFactory getRuntimeFactory(){
        if(runtimeFactory == null){
            runtimeFactory = new DefaultRuntimeFactory();
        }
        return runtimeFactory;
    }
    private DefaultClassLoaderFactory classLoaderFactory;
    DefaultClassLoaderFactory getClassLoaderFactory(){
        if(classLoaderFactory == null){
            classLoaderFactory = new DefaultClassLoaderFactory();
        }
        return classLoaderFactory;
    }
    synchronized ScheduledTaskDAO getScheduledTaskDAO() {
        if (scheduledTaskDAO == null) {
            scheduledTaskDAO = new ScheduledTaskDAO();
            scheduledTaskDAO.setMongoHelper(getScheduledTaskHelper());
            scheduledTaskDAO.init();
        }
        return scheduledTaskDAO;
    }

    synchronized RepairDAO getRepairDAO() {
        if (repairDAO == null) {
            repairDAO = new RepairDAO();
            repairDAO.setMongoHelper(getRepairHelper());
            repairDAO.init();
        }
        return repairDAO;
    }

    synchronized ServiceVersionServiceImpl getServiceVersionService() {
        if (serviceVersionService == null) {
            serviceVersionService = new ServiceVersionServiceImpl();
            serviceVersionService.setServiceVersionDAO(getServiceVersionDAO());
        }
        return serviceVersionService;
    }

    synchronized DeployServiceVersionServiceImpl getDeployServiceVersionService() {
        if (deployServiceVersionService == null) {
            deployServiceVersionService = new DeployServiceVersionServiceImpl();
            deployServiceVersionService.setDeployServiceVersionDAO(getDeployServiceVersionDAO());
        }
        return deployServiceVersionService;
    }


    synchronized ServiceVersionDAO getServiceVersionDAO() {
        if (serviceVersionDAO == null) {
            serviceVersionDAO = new ServiceVersionDAO();
            serviceVersionDAO.setMongoHelper(getDockerStatusHelper());
            serviceVersionDAO.init();
        }
        return serviceVersionDAO;
    }

    synchronized DeployServiceVersionDAO getDeployServiceVersionDAO() {
        if (deployServiceVersionDAO == null) {
            deployServiceVersionDAO = new DeployServiceVersionDAO();
            deployServiceVersionDAO.setMongoHelper(getDockerStatusHelper());
            deployServiceVersionDAO.init();
        }
        return deployServiceVersionDAO;
    }

    synchronized ServersServiceImpl getServersService() {
        if (serversService == null) {
            serversService = new ServersServiceImpl();
            serversService.setServersDAO(getServersDAO());
        }
        return serversService;
    }

    synchronized DockerStatusDAO getDockerStatusDAO() {
        if (dockerStatusDAO == null) {
            dockerStatusDAO = new DockerStatusDAO();
            dockerStatusDAO.setMongoHelper(getDockerStatusHelper());
            dockerStatusDAO.init();
        }
        return dockerStatusDAO;
    }

    synchronized RMIServerHandler getDockerRpcServerAdapterSsl() {
        if (dockerRpcServerAdapterSsl == null) {
            dockerRpcServerAdapterSsl = new RMIServerHandler();
            dockerRpcServerAdapterSsl.setServerImpl(getDockerRpcServerSsl());
            dockerRpcServerAdapterSsl.setIpHolder(getIpHolder());
            dockerRpcServerAdapterSsl.setRmiPort(baseConfiguration.getSslRpcPort());
            dockerRpcServerAdapterSsl.setEnableSsl(true);
            dockerRpcServerAdapterSsl.setRpcSslClientTrustJksPath(baseConfiguration.getRpcSslClientTrustJksPath());
            dockerRpcServerAdapterSsl.setRpcSslServerJksPath(baseConfiguration.getRpcSslServerJksPath());
            dockerRpcServerAdapterSsl.setRpcSslJksPwd(baseConfiguration.getRpcSslJksPwd());
        }
        return dockerRpcServerAdapterSsl;
    }

    synchronized RMIServerImplWrapper getDockerRpcServerSsl() {
        if (dockerRpcServerSsl == null) {
            try {
                dockerRpcServerSsl = getRpcServerSsl();
//                dockerRpcServerSsl = new com.docker.rpc.impl.RMIServerImplWrapper(Integer.valueOf(getRpcPort()));
                dockerRpcServerSsl.setRmiServerHandler(getDockerRpcServerAdapterSsl());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return dockerRpcServerSsl;
    }

    synchronized RMIServerHandler getDockerRpcServerAdapter() {
        if (dockerRpcServerAdapter == null) {
            dockerRpcServerAdapter = new RMIServerHandler();
            dockerRpcServerAdapter.setServerImpl(getDockerRpcServer());
            dockerRpcServerAdapter.setIpHolder(getIpHolder());
            dockerRpcServerAdapter.setRmiPort(baseConfiguration.getRpcPort());
        }
        return dockerRpcServerAdapter;
    }

    synchronized RMIServerImplWrapper getDockerRpcServer() {
        if (dockerRpcServer == null) {
            try {
                dockerRpcServer = getRpcServer();
//                dockerRpcServer = new com.docker.rpc.impl.RMIServerImplWrapper(Integer.valueOf(getRpcPort()));
                dockerRpcServer.setRmiServerHandler(getDockerRpcServerAdapter());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return dockerRpcServer;
    }

    synchronized RMIServerImplWrapper getRpcServerSsl() {
        if (rpcServerSsl == null) {
            try {
                rpcServerSsl = new RMIServerImplWrapper(baseConfiguration.getSslRpcPort());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return rpcServerSsl;
    }
    BaseConfiguration getBaseConfiguration(){
        return baseConfiguration;
    }
    private DefaultContextFactory contextFactory;
    synchronized DefaultContextFactory getContextFactory(){
        if(contextFactory == null){
            contextFactory = new DefaultContextFactory();
        }
        return contextFactory;
    }
    synchronized RMIServerImplWrapper getRpcServer() {
        if (rpcServer == null) {
            try {
                rpcServer = new RMIServerImplWrapper(baseConfiguration.getRpcPort());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return rpcServer;
    }

    synchronized OnlineServerWithStatus getOnlineServer() {
        if (onlineServer == null) {
            onlineServer = new OnlineServerWithStatus();
            onlineServer.setDockerStatusService(getDockerStatusService());
            onlineServer.setIpHolder(getIpHolder());
        }
        return onlineServer;
    }

    synchronized BootManager getBootManager() {
        if (scriptManager == null) {
            scriptManager = new BootManager();
            scriptManager.setRuntimeExecutor(new DefaultRuntimeExecutor());
        }
        return scriptManager;
    }

    synchronized RequestPermissionHandler getRequestPermissionHandler() {
        if (requestPermissionHandler == null) {
            requestPermissionHandler = new RequestPermissionHandler();
        }
        return requestPermissionHandler;
    }

    synchronized JsonFilterFactory getJsonFilterFactory() {
        if (jsonFilterFactory == null) {
            jsonFilterFactory = new JsonFilterFactory();
        }
        return jsonFilterFactory;
    }

    synchronized IPHolder getIpHolder() {
        if (ipHolder == null) {
            ipHolder = new IPHolder();
            ipHolder.setEthPrefix(baseConfiguration.getEthPrefix());
            ipHolder.setIpPrefix(baseConfiguration.getIpPrefix());
        }
        return ipHolder;
    }


    synchronized DockerStatusServiceImpl getDockerStatusService() {
        if (dockerStatusService == null) {
            dockerStatusService = new DockerStatusServiceImpl();
            dockerStatusService.setDockerStatusDAO(getDockerStatusDAO());
        }
        return dockerStatusService;
    }

    synchronized PlainSocketFactory getPlainSocketFactory() {
        if (plainSocketFactory == null) {
            plainSocketFactory = PlainSocketFactory.getSocketFactory();
        }
        return plainSocketFactory;
    }

    synchronized SSLSocketFactory getSslSocketFactory() {
        if (sslSocketFactory == null) {
            sslSocketFactory = SSLSocketFactory.getSocketFactory();
        }
        return sslSocketFactory;
    }

    synchronized Scheme getHttpScheme() {
        if (httpScheme == null) {
            httpScheme = new Scheme("http", 80, getPlainSocketFactory());
        }
        return httpScheme;
    }

    synchronized Scheme getHttpsScheme() {
        if (httpsScheme == null) {
            httpsScheme = new Scheme("https", 443, getSslSocketFactory());
        }
        return httpsScheme;
    }

    synchronized DefaultHttpClient getHttpClient() {
        if (httpClient == null) {
            MyHttpParameters myHttpParameters = new MyHttpParameters();
            myHttpParameters.setCharset("utf8");
            myHttpParameters.setConnectionTimeout(30000);
            myHttpParameters.setSocketTimeout(30000);
            httpClient = new DefaultHttpClient(getClientConnManager(), myHttpParameters);
        }
        return httpClient;
    }

    synchronized SchemeRegistry getSchemeRegistry() {
        if (schemeRegistry == null) {
            schemeRegistry = new SchemeRegistry();
            Map map = new HashMap();
            map.put("http", getHttpScheme());
            map.put("https", getHttpsScheme());
            schemeRegistry.setItems(map);
        }
        return schemeRegistry;
    }

    synchronized ThreadSafeClientConnManager getClientConnManager() {
        if (clientConnManager == null) {
            clientConnManager = new ThreadSafeClientConnManager(getSchemeRegistry());
            clientConnManager.setMaxTotal(20);
        }
        return clientConnManager;
    }

    synchronized MongoHelper getDockerStatusHelper() {
        if (dockerStatusHelper == null) {
            dockerStatusHelper = new MongoHelper();
            dockerStatusHelper.setHost(baseConfiguration.getMongoHost());
            dockerStatusHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            dockerStatusHelper.setDbName(baseConfiguration.getDbName());
            dockerStatusHelper.init();
        }
        return dockerStatusHelper;
    }

    synchronized MongoHelper getScheduledTaskHelper() {
        if (scheduledTaskHelper == null) {
            scheduledTaskHelper = new MongoHelper();
            scheduledTaskHelper.setHost(baseConfiguration.getMongoHost());
            scheduledTaskHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            scheduledTaskHelper.setDbName("scheduled");
            scheduledTaskHelper.init();
        }
        return scheduledTaskHelper;
    }

    synchronized MongoHelper getRepairHelper() {
        if (repairHelper == null) {
            repairHelper = new MongoHelper();
            repairHelper.setHost(baseConfiguration.getMongoHost());
            repairHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            repairHelper.setDbName("extras");
            repairHelper.init();
        }
        return repairHelper;
    }

    synchronized MongoHelper getLogsHelper() {
        if (logsHelper == null) {
            logsHelper = new MongoHelper();
            logsHelper.setHost(baseConfiguration.getMongoHost());
            logsHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            logsHelper.setDbName(baseConfiguration.getLogsDBName());
            logsHelper.init();
        }
        return logsHelper;
    }

    synchronized MongoHelper getConfigHelper() {
        if (configHelper == null) {
            configHelper = new MongoHelper();
            configHelper.setHost(baseConfiguration.getMongoHost());
            configHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            configHelper.setDbName(baseConfiguration.getConfigDBName());
            configHelper.init();
        }
        return configHelper;
    }

    synchronized ServersDAO getServersDAO() {
        if (serversDAO == null) {
            serversDAO = new ServersDAO();
            serversDAO.setMongoHelper(getConfigHelper());
            serversDAO.init();
        }
        return serversDAO;
    }

    synchronized LansDAO getLansDAO() {
        if (lansDAO == null) {
            lansDAO = new LansDAO();
            lansDAO.setMongoHelper(getConfigHelper());
            lansDAO.init();
        }
        return lansDAO;
    }

    synchronized SDockerDAO getSdockerDAO() {
        if (sdockerDAO == null) {
            sdockerDAO = new SDockerDAO();
            sdockerDAO.setMongoHelper(getConfigHelper());
            sdockerDAO.init();
        }
        return sdockerDAO;
    }

    synchronized MongoHelper getGridfsHelper() {
        if (gridfsHelper == null) {
            gridfsHelper = new MongoHelper();
            gridfsHelper.setHost(baseConfiguration.getGridHost());
            gridfsHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getGirdConnectionsPerHost()));
            gridfsHelper.setDbName(baseConfiguration.getGridDbName());
            gridfsHelper.init();
        }
        return gridfsHelper;
    }

    synchronized GridFSFileHandler getFileAdapter() {
        if (fileAdapter == null) {
            fileAdapter = new GridFSFileHandler();
            fileAdapter.setResourceHelper(getGridfsHelper());
            fileAdapter.setBucketName(baseConfiguration.getFileBucket());
            fileAdapter.init();
        }
        return fileAdapter;
    }

//    static ContextBeanApp getInstance() {
//        if (instance == null) {
//            synchronized (ContextBeanApp.class) {
//                if (instance == null) {
//                    instance = new ContextBeanApp();
//                    baseConfiguration = new BaseConfigurationBuilder().build();
//                    BeanFactory.init(baseConfiguration);
//                }
//            }
//        }
//        return instance;
//    }
}
