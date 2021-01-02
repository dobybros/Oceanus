package container.container.bean;

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
import com.docker.script.executor.prepare.config.BaseConfigurationBuilder;
import com.docker.storage.adapters.impl.*;
import com.docker.storage.mongodb.MongoHelper;
import com.docker.storage.mongodb.daos.*;
import com.docker.storage.redis.RedisListenerHandler;
import com.docker.storage.redis.RedisSubscribeHandler;
import com.docker.storage.zookeeper.ZookeeperFactory;
import com.docker.tasks.RepairTaskHandler;
import com.docker.utils.BeanFactory;
import com.container.runtime.BootManager;
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

public class ContextBeanApp {
    private static final String TAG = ContextBeanApp.class.getSimpleName();
    private static volatile ContextBeanApp instance;
    protected static BaseConfiguration baseConfiguration;
    private PlainSocketFactory plainSocketFactory;
    private BeanFactory.SpringContextUtil springContextUtil;
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
    public synchronized BeanFactory.SpringContextUtil getSpringContextUtil(){
        if(instance.springContextUtil == null){
            instance.springContextUtil = new BeanFactory.SpringContextUtil();
        }
        return instance.springContextUtil;
    }
    public synchronized ZookeeperFactory getZookeeperFactory() {
        if (instance.zookeeperFactory == null) {
            instance.zookeeperFactory = new ZookeeperFactory();
        }
        return instance.zookeeperFactory;
    }

    public synchronized RepairTaskHandler getRepairTaskHandler() {
        if (instance.repairTaskHandler == null) {
            instance.repairTaskHandler = new RepairTaskHandler();
        }
        return instance.repairTaskHandler;
    }

    public synchronized RedisSubscribeHandler getRedisSubscribeHandler() {
        if (instance.redisSubscribeHandler == null) {
            instance.redisSubscribeHandler = new RedisSubscribeHandler();
        }
        return instance.redisSubscribeHandler;
    }

    private QueueSimplexListener queueSimplexListener;

    public synchronized QueueSimplexListener getQueueSimplexListener() {
        if (instance.queueSimplexListener == null) {
            try {
                Class<?> queueSimplexListenerClass = Class.forName("com.docker.rpc.queue.KafkaSimplexListener");
                instance.queueSimplexListener = (QueueSimplexListener) queueSimplexListenerClass.getDeclaredConstructor().newInstance();
                Map<String, String> config = new HashMap<>();
//                config.put("bootstrap.servers", instance.getKafkaServers());
//                config.put("producer.key.serializer", instance.getKafkaProducerKeySerializer());
//                config.put("producer.value.serializer", instance.getKafkaProducerValueSerializer());
//                config.put("retries", instance.getKafkaProducerRetries());
//                config.put("linger.ms", instance.getKafkaProducerLingerMs());
//                config.put("consumer.key.serializer", instance.getKafkaConsumerKeySerializer());
//                config.put("consumer.value.serializer", instance.getKafkaConsumerValueSerializer());
                instance.queueSimplexListener.setConfig(config);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return instance.queueSimplexListener;
    }

    public synchronized ScheduledTaskServiceImpl getScheduledTaskService() {
        if (instance.scheduledTaskService == null) {
            instance.scheduledTaskService = new ScheduledTaskServiceImpl();
            instance.scheduledTaskService.setScheduledTaskDAO(instance.getScheduledTaskDAO());
        }
        return instance.scheduledTaskService;
    }

    public synchronized RedisListenerHandler getRedisListenerHandler() {
        if (instance.redisListenerHandler == null) {
            instance.redisListenerHandler = new RedisListenerHandler();
        }
        return instance.redisListenerHandler;
    }

    public synchronized RepairServiceImpl getRepairService() {
        if (instance.repairService == null) {
            instance.repairService = new RepairServiceImpl();
        }
        return instance.repairService;
    }

    private RuntimeFactory runtimeFactory;
    public synchronized RuntimeFactory getRuntimeFactory(){
        if(instance.runtimeFactory == null){
            instance.runtimeFactory = new DefaultRuntimeFactory();
        }
        return instance.runtimeFactory;
    }
    private ClassLoaderFactory classLoaderFactory;
    public ClassLoaderFactory getClassLoaderFactory(){
        if(instance.classLoaderFactory == null){
            instance.classLoaderFactory = new DefaultClassLoaderFactory();
        }
        return instance.classLoaderFactory;
    }
    public synchronized ScheduledTaskDAO getScheduledTaskDAO() {
        if (instance.scheduledTaskDAO == null) {
            instance.scheduledTaskDAO = new ScheduledTaskDAO();
            instance.scheduledTaskDAO.setMongoHelper(instance.getScheduledTaskHelper());
        }
        return instance.scheduledTaskDAO;
    }

    public synchronized RepairDAO getRepairDAO() {
        if (instance.repairDAO == null) {
            instance.repairDAO = new RepairDAO();
            instance.repairDAO.setMongoHelper(instance.getRepairHelper());
        }
        return instance.repairDAO;
    }

    public synchronized ServiceVersionServiceImpl getServiceVersionService() {
        if (instance.serviceVersionService == null) {
            instance.serviceVersionService = new ServiceVersionServiceImpl();
            instance.serviceVersionService.setServiceVersionDAO(instance.getServiceVersionDAO());
        }
        return instance.serviceVersionService;
    }

    public synchronized DeployServiceVersionServiceImpl getDeployServiceVersionService() {
        if (instance.deployServiceVersionService == null) {
            instance.deployServiceVersionService = new DeployServiceVersionServiceImpl();
            instance.deployServiceVersionService.setDeployServiceVersionDAO(instance.getDeployServiceVersionDAO());
        }
        return instance.deployServiceVersionService;
    }


    public synchronized ServiceVersionDAO getServiceVersionDAO() {
        if (instance.serviceVersionDAO == null) {
            instance.serviceVersionDAO = new ServiceVersionDAO();
            instance.serviceVersionDAO.setMongoHelper(instance.getDockerStatusHelper());
        }
        return instance.serviceVersionDAO;
    }

    public synchronized DeployServiceVersionDAO getDeployServiceVersionDAO() {
        if (instance.deployServiceVersionDAO == null) {
            instance.deployServiceVersionDAO = new DeployServiceVersionDAO();
            instance.deployServiceVersionDAO.setMongoHelper(instance.getDockerStatusHelper());
        }
        return instance.deployServiceVersionDAO;
    }

    public synchronized ServersServiceImpl getServersService() {
        if (instance.serversService == null) {
            instance.serversService = new ServersServiceImpl();
        }
        return instance.serversService;
    }

    public synchronized DockerStatusDAO getDockerStatusDAO() {
        if (instance.dockerStatusDAO == null) {
            instance.dockerStatusDAO = new DockerStatusDAO();
            instance.dockerStatusDAO.setMongoHelper(instance.getDockerStatusHelper());
        }
        return instance.dockerStatusDAO;
    }

    public synchronized RMIServerHandler getDockerRpcServerAdapterSsl() {
        if (instance.dockerRpcServerAdapterSsl == null) {
            instance.dockerRpcServerAdapterSsl = new RMIServerHandler();
            instance.dockerRpcServerAdapterSsl.setServerImpl(instance.getDockerRpcServerSsl());
            instance.dockerRpcServerAdapterSsl.setIpHolder(instance.getIpHolder());
            instance.dockerRpcServerAdapterSsl.setRmiPort(baseConfiguration.getSslRpcPort());
            instance.dockerRpcServerAdapterSsl.setEnableSsl(true);
            instance.dockerRpcServerAdapterSsl.setRpcSslClientTrustJksPath(baseConfiguration.getRpcSslClientTrustJksPath());
            instance.dockerRpcServerAdapterSsl.setRpcSslServerJksPath(baseConfiguration.getRpcSslServerJksPath());
            instance.dockerRpcServerAdapterSsl.setRpcSslJksPwd(baseConfiguration.getRpcSslJksPwd());
        }
        return instance.dockerRpcServerAdapterSsl;
    }

    public synchronized RMIServerImplWrapper getDockerRpcServerSsl() {
        if (instance.dockerRpcServerSsl == null) {
            try {
                instance.dockerRpcServerSsl = instance.getRpcServerSsl();
//                dockerRpcServerSsl = new com.docker.rpc.impl.RMIServerImplWrapper(Integer.valueOf(getRpcPort()));
                instance.dockerRpcServerSsl.setRmiServerHandler(instance.getDockerRpcServerAdapterSsl());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return instance.dockerRpcServerSsl;
    }

    public synchronized RMIServerHandler getDockerRpcServerAdapter() {
        if (instance.dockerRpcServerAdapter == null) {
            instance.dockerRpcServerAdapter = new RMIServerHandler();
            instance.dockerRpcServerAdapter.setServerImpl(instance.getDockerRpcServer());
            instance.dockerRpcServerAdapter.setIpHolder(instance.getIpHolder());
            instance.dockerRpcServerAdapter.setRmiPort(baseConfiguration.getRpcPort());
        }
        return instance.dockerRpcServerAdapter;
    }

    public synchronized RMIServerImplWrapper getDockerRpcServer() {
        if (instance.dockerRpcServer == null) {
            try {
                instance.dockerRpcServer = instance.getRpcServer();
//                dockerRpcServer = new com.docker.rpc.impl.RMIServerImplWrapper(Integer.valueOf(getRpcPort()));
                instance.dockerRpcServer.setRmiServerHandler(instance.getDockerRpcServerAdapter());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return instance.dockerRpcServer;
    }

    public synchronized RMIServerImplWrapper getRpcServerSsl() {
        if (instance.rpcServerSsl == null) {
            try {
                instance.rpcServerSsl = new RMIServerImplWrapper(baseConfiguration.getSslRpcPort());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return instance.rpcServerSsl;
    }
    public BaseConfiguration getBaseConfiguration(){
        return baseConfiguration;
    }
    private ContextFactory contextFactory;
    public synchronized ContextFactory getContextFactory(){
        if(contextFactory == null){
            contextFactory = new DefaultContextFactory();
        }
        return contextFactory;
    }
    public synchronized RMIServerImplWrapper getRpcServer() {
        if (instance.rpcServer == null) {
            try {
                instance.rpcServer = new RMIServerImplWrapper(baseConfiguration.getRpcPort());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return instance.rpcServer;
    }

    public synchronized OnlineServerWithStatus getOnlineServer() {
        if (instance.onlineServer == null) {
            instance.onlineServer = new OnlineServerWithStatus();
            instance.onlineServer.setDockerStatusService(instance.getDockerStatusService());
            instance.onlineServer.setIpHolder(instance.getIpHolder());
        }
        return instance.onlineServer;
    }

    public synchronized BootManager getScriptManager() {
        if (instance.scriptManager == null) {
            instance.scriptManager = new BootManager();
            instance.scriptManager.setRuntimeExecutor(new DefaultRuntimeExecutor());
        }
        return instance.scriptManager;
    }

    public synchronized RequestPermissionHandler getRequestPermissionHandler() {
        if (instance.requestPermissionHandler == null) {
            instance.requestPermissionHandler = new RequestPermissionHandler();
        }
        return instance.requestPermissionHandler;
    }

    public synchronized JsonFilterFactory getJsonFilterFactory() {
        if (instance.jsonFilterFactory == null) {
            instance.jsonFilterFactory = new JsonFilterFactory();
        }
        return instance.jsonFilterFactory;
    }

    public synchronized IPHolder getIpHolder() {
        if (instance.ipHolder == null) {
            instance.ipHolder = new IPHolder();
            instance.ipHolder.setEthPrefix(baseConfiguration.getEthPrefix());
            instance.ipHolder.setIpPrefix(baseConfiguration.getIpPrefix());
        }
        return instance.ipHolder;
    }


    public synchronized DockerStatusServiceImpl getDockerStatusService() {
        if (instance.dockerStatusService == null) {
            instance.dockerStatusService = new DockerStatusServiceImpl();
            instance.dockerStatusService.setDockerStatusDAO(instance.getDockerStatusDAO());
        }
        return instance.dockerStatusService;
    }

    public synchronized PlainSocketFactory getPlainSocketFactory() {
        if (instance.plainSocketFactory == null) {
            instance.plainSocketFactory = PlainSocketFactory.getSocketFactory();
        }
        return instance.plainSocketFactory;
    }

    public synchronized SSLSocketFactory getSslSocketFactory() {
        if (instance.sslSocketFactory == null) {
            instance.sslSocketFactory = SSLSocketFactory.getSocketFactory();
        }
        return instance.sslSocketFactory;
    }

    public synchronized Scheme getHttpScheme() {
        if (instance.httpScheme == null) {
            instance.httpScheme = new Scheme("http", 80, getPlainSocketFactory());
        }
        return instance.httpScheme;
    }

    public synchronized Scheme getHttpsScheme() {
        if (instance.httpsScheme == null) {
            instance.httpsScheme = new Scheme("https", 443, getSslSocketFactory());
        }
        return instance.httpsScheme;
    }

    public synchronized DefaultHttpClient getHttpClient() {
        if (instance.httpClient == null) {
            MyHttpParameters myHttpParameters = new MyHttpParameters();
            myHttpParameters.setCharset("utf8");
            myHttpParameters.setConnectionTimeout(30000);
            myHttpParameters.setSocketTimeout(30000);
            instance.httpClient = new DefaultHttpClient(getClientConnManager(), myHttpParameters);
        }
        return instance.httpClient;
    }

    public synchronized SchemeRegistry getSchemeRegistry() {
        if (instance.schemeRegistry == null) {
            instance.schemeRegistry = new SchemeRegistry();
            Map map = new HashMap();
            map.put("http", instance.getHttpScheme());
            map.put("https", instance.getHttpsScheme());
            instance.schemeRegistry.setItems(map);
        }
        return instance.schemeRegistry;
    }

    public synchronized ThreadSafeClientConnManager getClientConnManager() {
        if (instance.clientConnManager == null) {
            instance.clientConnManager = new ThreadSafeClientConnManager(getSchemeRegistry());
            instance.clientConnManager.setMaxTotal(20);
        }
        return instance.clientConnManager;
    }

    public synchronized MongoHelper getDockerStatusHelper() {
        if (instance.dockerStatusHelper == null) {
            instance.dockerStatusHelper = new MongoHelper();
            instance.dockerStatusHelper.setHost(baseConfiguration.getMongoHost());
            instance.dockerStatusHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            instance.dockerStatusHelper.setDbName(baseConfiguration.getDbName());
        }
        return instance.dockerStatusHelper;
    }

    public synchronized MongoHelper getScheduledTaskHelper() {
        if (instance.scheduledTaskHelper == null) {
            instance.scheduledTaskHelper = new MongoHelper();
            instance.scheduledTaskHelper.setHost(baseConfiguration.getMongoHost());
            instance.scheduledTaskHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            instance.scheduledTaskHelper.setDbName("scheduled");
        }
        return instance.scheduledTaskHelper;
    }

    public synchronized MongoHelper getRepairHelper() {
        if (instance.repairHelper == null) {
            instance.repairHelper = new MongoHelper();
            instance.repairHelper.setHost(baseConfiguration.getMongoHost());
            instance.repairHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            instance.repairHelper.setDbName("extras");
        }
        return instance.repairHelper;
    }

    public synchronized MongoHelper getLogsHelper() {
        if (instance.logsHelper == null) {
            instance.logsHelper = new MongoHelper();
            instance.logsHelper.setHost(baseConfiguration.getMongoHost());
            instance.logsHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            instance.logsHelper.setDbName(baseConfiguration.getLogsDBName());
        }
        return instance.logsHelper;
    }

    public synchronized MongoHelper getConfigHelper() {
        if (instance.configHelper == null) {
            instance.configHelper = new MongoHelper();
            instance.configHelper.setHost(baseConfiguration.getMongoHost());
            instance.configHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getMongoConnectionsPerHost()));
            instance.configHelper.setDbName(baseConfiguration.getConfigDBName());
        }
        return instance.configHelper;
    }

    public synchronized ServersDAO getServersDAO() {
        if (instance.serversDAO == null) {
            instance.serversDAO = new ServersDAO();
            instance.serversDAO.setMongoHelper(instance.getConfigHelper());
        }
        return instance.serversDAO;
    }

    public synchronized LansDAO getLansDAO() {
        if (instance.lansDAO == null) {
            instance.lansDAO = new LansDAO();
            instance.lansDAO.setMongoHelper(instance.getConfigHelper());
        }
        return instance.lansDAO;
    }

    public synchronized SDockerDAO getSdockerDAO() {
        if (instance.sdockerDAO == null) {
            instance.sdockerDAO = new SDockerDAO();
            instance.sdockerDAO.setMongoHelper(instance.getConfigHelper());
        }
        return instance.sdockerDAO;
    }

    public synchronized MongoHelper getGridfsHelper() {
        if (instance.gridfsHelper == null) {
            instance.gridfsHelper = new MongoHelper();
            instance.gridfsHelper.setHost(baseConfiguration.getGridHost());
            instance.gridfsHelper.setConnectionsPerHost(Integer.valueOf(baseConfiguration.getGirdConnectionsPerHost()));
            instance.gridfsHelper.setDbName(baseConfiguration.getGridDbName());
        }
        return instance.gridfsHelper;
    }

    public synchronized GridFSFileHandler getFileAdapter() {
        if (instance.fileAdapter == null) {
            instance.fileAdapter = new GridFSFileHandler();
            instance.fileAdapter.setResourceHelper(instance.getGridfsHelper());
            instance.fileAdapter.setBucketName(baseConfiguration.getFileBucket());
        }
        return instance.fileAdapter;
    }

    public static ContextBeanApp getInstance() {
        if (instance == null) {
            synchronized (ContextBeanApp.class) {
                if (instance == null) {
                    instance = new ContextBeanApp();
                    baseConfiguration = new BaseConfigurationBuilder().build();
                    BeanFactory.init(baseConfiguration);
                }
            }
        }
        return instance;
    }
}
