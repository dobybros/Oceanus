package oceanus.sdk.server;

import oceanus.apis.NewObjectInterception;
import oceanus.sdk.core.discovery.DiscoveryRuntime;
import oceanus.sdk.core.discovery.NodeRegistrationHandler;
import oceanus.sdk.core.discovery.errors.DiscoveryErrorCodes;
import oceanus.sdk.core.discovery.impl.client.ServiceRuntime;
import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.core.net.NetRuntime;
import oceanus.apis.CoreException;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.server.remote.RuntimeServiceStubManagerFactory;
import oceanus.sdk.utils.IPHolder;
import oceanus.sdk.utils.OceanusProperties;
import oceanus.sdk.utils.annotation.ClassAnnotationHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class OnlineServer {
    private static final String TAG = OnlineServer.class.getSimpleName();
    private IPHolder ipHolder;
    private NewObjectInterception newObjectInterception;
//    private SDockersService sdockersService;

//    private DockerStatusService dockerStatusService;

    private ConcurrentHashMap<Object, ClassAnnotationHandler> classAnnotationHandlerMap;


    private static OnlineServer instance;

    private NodeRegistrationHandler nodeRegistrationHandler;
//    private DockerStatus dockerStatus;

    private OnlineServerStartHandler startHandler;
    private RuntimeServiceStubManagerFactory serviceStubManagerFactory;
    private final Object nodeLock = new Object();
    public static final int NODE_STATUS_NONE = 1;
    public static final int NODE_STATUS_REGISTERING = 2;
    public static final int NODE_STATUS_REGISTERED = 4;
    private AtomicInteger nodeRegisterStatus = new AtomicInteger(NODE_STATUS_NONE);
    private ConcurrentHashMap<Service, CompletableFuture<ServiceRuntime>> pendingServiceMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Class<?>, Object> beanMap = new ConcurrentHashMap<>();

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
                        DiscoveryRuntime.getAndInitNodeRegistrationHandler(-1).startNode(OceanusProperties.getInstance().getDiscoveryHost(), OnlineServer.getInstance().getIp(), OceanusProperties.getInstance().getRpcPort()).
                                thenAccept(consumer).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            LoggerEx.error(TAG, "Register node to "  + OceanusProperties.getInstance().getDiscoveryHost() + " failed, " + throwable);
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
    public void setClassAnnotationHandlerMap(ConcurrentHashMap<Object, ClassAnnotationHandler> classAnnotationHandlerMap) {
        this.classAnnotationHandlerMap = classAnnotationHandlerMap;
    }

    public ClassAnnotationHandler getClassAnnotationHandler(Object key) {
        return this.classAnnotationHandlerMap.get(key);
    }

    public static interface OnlineServerStartHandler {
        public void serverWillStart(OnlineServer onlineServer) throws CoreException;

        public void serverWillShutdown(OnlineServer onlineServer);
    }

    public OnlineServer() {
        instance = this;
        this.serviceStubManagerFactory = new RuntimeServiceStubManagerFactory();
        ipHolder = new IPHolder();
        ipHolder.setEthPrefix(OceanusProperties.getInstance().getEthPrefix());
        ipHolder.setIpPrefix(OceanusProperties.getInstance().getIpPrefix());
        ipHolder.init();
    }

    public IPHolder getIpHolder() {
        return ipHolder;
    }

    public RuntimeServiceStubManagerFactory getServiceStubManagerFactory() {
        return serviceStubManagerFactory;
    }

    public static OnlineServer getInstance() {
        return instance;
    }
    public String getServer() {
        return String.valueOf(NetRuntime.getServerNameCRC());
    }
    public void prepare() {
    }

    public String getIp() {
        if (ipHolder != null)
            return ipHolder.getIp();
        return null;
    }

    public void start() {

    }
    public static void shutdownNow() {
        OnlineServer onlineServer = OnlineServer.getInstance();
        if (onlineServer != null)
            onlineServer.shutdown();
    }

    public void shutdown() {
        LoggerEx.info(TAG, "OnlineServer " + getServer() + " is shutting down");
        if (startHandler != null) {
            try {
                startHandler.serverWillShutdown(this);
            } catch (Exception e) {
                e.printStackTrace();
                LoggerEx.fatal(TAG, "StartHandler " + startHandler + " shutdown failed, " + e.getMessage());
            }
        }
    }

    public NewObjectInterception getNewObjectInterception() {
        return newObjectInterception;
    }

    public void setNewObjectInterception(NewObjectInterception newObjectInterception) {
        this.newObjectInterception = newObjectInterception;
    }


    public <T> T getOrCreateObject(Class<T> clazz) {
        T t = null;
        t = (T) beanMap.get(clazz);
        if(t == null) {
            synchronized (this) {
                t = (T) beanMap.get(clazz);
                if(t == null) {
                    if(newObjectInterception != null) {
                        t = (T) newObjectInterception.newObject(clazz);
                    }
                    if(t == null) {
                        try {
                            Constructor<?> constructor = clazz.getConstructor();
                            t = (T) constructor.newInstance();
                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(t != null) {
                    beanMap.putIfAbsent(clazz, t);
                }
            }
        }
        return t;
    }

    public NodeRegistrationHandler getNodeRegistrationHandler() {
        return nodeRegistrationHandler;
    }
}