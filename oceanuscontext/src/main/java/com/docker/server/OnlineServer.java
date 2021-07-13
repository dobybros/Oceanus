package com.docker.server;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.main.ServerStart;
import chat.utils.IPHolder;
import chat.config.BaseConfiguration;
import com.docker.oceansbean.BeanFactory;
import com.docker.storage.cache.CacheStorageFactory;
import com.docker.storage.cache.CacheStorageMethod;
import com.docker.tasks.Task;
import core.discovery.DiscoveryRuntime;
import core.discovery.NodeRegistrationHandler;
import core.discovery.errors.DiscoveryErrorCodes;
import core.discovery.impl.client.ServiceRuntime;
import core.discovery.node.Service;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class OnlineServer {
    private static final String TAG = OnlineServer.class.getSimpleName();
    private List<Task> tasks;
    private IPHolder ipHolder;
//    private SDockersService sdockersService;

//    private DockerStatusService dockerStatusService;



    private static OnlineServer instance;

    private NodeRegistrationHandler nodeRegistrationHandler;
//    private DockerStatus dockerStatus;

    private OnlineServerStartHandler startHandler;
    protected BaseConfiguration baseConfiguration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());

    private final Object nodeLock = new Object();
    public static final int NODE_STATUS_NONE = 1;
    public static final int NODE_STATUS_REGISTERING = 2;
    public static final int NODE_STATUS_REGISTERED = 4;
    private AtomicInteger nodeRegisterStatus = new AtomicInteger(NODE_STATUS_NONE);
    private ConcurrentHashMap<Service, CompletableFuture<ServiceRuntime>> pendingServiceMap = new ConcurrentHashMap<>();

    public CompletableFuture<ServiceRuntime> registerService(Service service) {
        CompletableFuture<ServiceRuntime> future = new CompletableFuture<>();
        Consumer<NodeRegistrationHandler> consumer = nodeRegistrationHandler -> {
            synchronized (nodeLock) {
                this.nodeRegistrationHandler = nodeRegistrationHandler;
                if(nodeRegisterStatus.compareAndSet(NODE_STATUS_REGISTERING, NODE_STATUS_REGISTERED) || nodeRegisterStatus.get() == NODE_STATUS_REGISTERED) {
                    Collection<Service> services = pendingServiceMap.keySet();
                    for(Service theService : services) {
                        final CompletableFuture<ServiceRuntime> serviceFuture = pendingServiceMap.get(theService);
                        nodeRegistrationHandler.registerService(theService).thenAccept(serviceFuture::complete).exceptionally(throwable -> {
                            LoggerEx.error(TAG, "Register service future#get failed, " + throwable.getMessage() + " service " + service);
                            serviceFuture.completeExceptionally(new CoreException(DiscoveryErrorCodes.ERROR_REGISTER_SERVICE_FAILED, "Register service future#get failed, " + throwable.getMessage() + " service " + service));
                            return null;
                        });
                    }
                    pendingServiceMap.clear();
                } else {
                    LoggerEx.warn(TAG, "Change node status from registering to registered failed, current status is " + nodeRegisterStatus.get());
                }
            }
//            try {
//                return serviceFuture.get();
//            } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//                throw new CoreException(DiscoveryErrorCodes.ERROR_REGISTER_SERVICE_FAILED, "Register service future#get failed, " + e.getMessage() + " service " + service);
//            }
        };
        if(nodeRegistrationHandler == null) {
            synchronized (nodeLock) {
                if(nodeRegistrationHandler == null) {
                    pendingServiceMap.put(service, future);
                    if(nodeRegisterStatus.compareAndSet(NODE_STATUS_NONE, NODE_STATUS_REGISTERING)) {
                        configSystemProperties();
                        DiscoveryRuntime.getAndInitNodeRegistrationHandler(-1).startNode(baseConfiguration.getDiscoveryHost(), baseConfiguration.getRpcPort()).
                                thenAccept(consumer).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            LoggerEx.error(TAG, "Register node to "  + baseConfiguration.getDiscoveryHost() + " failed, " + throwable);
                            System.exit(0);
                            return null;
                        });
                    } else {
                        LoggerEx.warn(TAG, "node registration process is ongoing, status " + nodeRegisterStatus.get() + " record service " + service + ", register service when node registered");
                    }
                }
            }
        } else {
            synchronized (nodeLock) {
                pendingServiceMap.put(service, future);
                consumer.accept(nodeRegistrationHandler);
            }
        }
        return future;
    }

    private void configSystemProperties() {
        //这个是使用RUDP做微服务调用， 但是现在基本上没有使用， 所以做最小配置
        System.setProperty("starfish.discovery.packet.send.pool.size", "1");
    }

    public static interface OnlineServerStartHandler {
        public void serverWillStart(OnlineServer onlineServer) throws CoreException;

        public void serverWillShutdown(OnlineServer onlineServer);
    }

    public OnlineServer() {
        instance = this;
    }

    public static OnlineServer getInstance() {
        return instance;
    }

    public void prepare() {
    }

    public String getIp() {
        if (ipHolder != null)
            return ipHolder.getIp();
        return null;
    }

    public void setIpHolder(IPHolder ipHolder) {
        this.ipHolder = ipHolder;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

//    public void setSdockersService(SDockersService sdockersService) {
//        this.sdockersService = sdockersService;
//    }

//    public void setDockerStatusService(DockerStatusService dockerStatusService) {
//        this.dockerStatusService = dockerStatusService;
//    }
//
//    protected DockerStatus generateDockerStatus(Integer port) {
//        DockerStatus dockerStatus = new DockerStatus();
//        dockerStatus.setServer(baseConfiguration.getServer());
//        dockerStatus.setServerType(baseConfiguration.getServerType());
//        dockerStatus.setDockerName(baseConfiguration.getDockerName());
//        dockerStatus.setIp(ipHolder.getIp());
//        dockerStatus.setType(baseConfiguration.getType());
//        dockerStatus.setMaxUserNumber(baseConfiguration.getMaxUserNumber());
//        dockerStatus.setRpcPort(baseConfiguration.getRpcPort());
//        dockerStatus.setSslRpcPort(baseConfiguration.getSslRpcPort());
//        dockerStatus.setHttpPort(port);
//        dockerStatus.setLanId(baseConfiguration.getLanId());
//        if (baseConfiguration.getScaleInstanceId() != null) {
//            dockerStatus.setHealth(DockerStatus.HEALTH_MAX);
//        } else {
//            dockerStatus.setHealth(DockerStatus.HEALTH_MIN);
//        }
//        dockerStatus.setSslRpcPort(baseConfiguration.getSslRpcPort());
//        dockerStatus.setPublicWsPort(baseConfiguration.getPublicWsPort());
//        dockerStatus.setTime(ChatUtils.dateString(System.currentTimeMillis()));
//        dockerStatus.setStatus(DockerStatus.STATUS_STARTING);
//        Map<String, Object> info = new HashMap<String, Object>();
//        dockerStatus.setInfo(info);
//        return dockerStatus;
//    }

    public void start() {
        try {
//            if (dockerStatusService != null) {
//                dockerStatus = generateDockerStatus(baseConfiguration.getServerPort());
//                try {
//                    dockerStatusService.deleteDockerStatus(OnlineServer.getInstance().getIp(), baseConfiguration.getServerType(), baseConfiguration.getDockerName());
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                }
//                dockerStatusService.addDockerStatus(dockerStatus);
//            }
            if (tasks != null) {
                for (Task task : tasks) {
                    task.setOnlineServer(this);
                    task.init();
                    LoggerEx.info(TAG, "Task " + task + " initialized!");
                    int numOfThreads = task.getNumOfThreads();
                    for (int i = 0; i < numOfThreads; i++) {
                        ServerStart.getInstance().getGatewayThreadPoolExecutor().execute(task);
                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Start online server " + baseConfiguration.getServer() + " failed, " + ExceptionUtils.getFullStackTrace(e));
//            if (dockerStatusService != null) {
//                try {
//                    dockerStatusService.deleteDockerStatus(baseConfiguration.getServer());
//                    LoggerEx.info(TAG, "Deleted OnlineServer " + baseConfiguration.getServer() + " because of error " + ExceptionUtils.getFullStackTrace(e));
//                } catch (CoreException e1) {
//                    e.printStackTrace();
//                    LoggerEx.info(TAG, "Remove online server " + baseConfiguration.getServer() + " failed, " + ExceptionUtils.getFullStackTrace(e1));
//                }
//            }
            OnlineServer.shutdownNow();
            System.exit(0);
        }
    }
    public static void shutdownNow() {
        OnlineServer onlineServer = OnlineServer.getInstance();
        if (onlineServer != null)
            onlineServer.shutdown();
    }

    public void shutdown() {
        LoggerEx.info(TAG, "OnlineServer " + baseConfiguration.getServer() + " is shutting down");
        if (startHandler != null) {
            try {
                startHandler.serverWillShutdown(this);
            } catch (Exception e) {
                e.printStackTrace();
                LoggerEx.fatal(TAG, "StartHandler " + startHandler + " shutdown failed, " + ExceptionUtils.getFullStackTrace(e));
            }
        }
        if (tasks != null) {
            LoggerEx.info(TAG, "Deleted tasks " + tasks + " size " + tasks.size());
            for (Task task : tasks) {
                try {
                    LoggerEx.info(TAG, "Task " + task + " is shutting down");
                    task.shutdown();
                    LoggerEx.info(TAG, "Task " + task + " has been shutdown");
                } catch (Exception e) {
                    e.printStackTrace();
                    LoggerEx.fatal(TAG, "Task " + task + " shutdown failed, " + ExceptionUtils.getFullStackTrace(e));
                }
            }
        }
        CacheStorageFactory.getInstance().releaseAllCacheStorageAdapter(CacheStorageMethod.METHOD_REDIS);
//        if (shutdownList != null) {
//            LoggerEx.info(TAG, "Deleted shutdownListener " + shutdownList + " size " + shutdownList.size());
//            for (ShutdownListener shutdownListener : shutdownList) {
//                try {
//                    LoggerEx.info(TAG, "shutdownListener " + shutdownListener + " is shutting down");
//                    shutdownListener.shutdown();
//                    LoggerEx.info(TAG, "shutdownListener " + shutdownListener + " has been shutdown");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    LoggerEx.fatal(TAG, "shutdownListener " + shutdownListener + " shutdown failed, " + e.getMessage());
//                }
//            }
//        }
    }

    public NodeRegistrationHandler getNodeRegistrationHandler() {
        return nodeRegistrationHandler;
    }
}