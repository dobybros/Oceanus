package oceanus.sdk.core.discovery.impl.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import oceanus.sdk.core.common.InternalTools;
import oceanus.sdk.core.net.NetRuntime;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.logger.LoggerEx;

import java.util.Random;
import java.util.concurrent.*;

public class ServiceNodesManager {
    private static final String TAG = ServiceNodesManager.class.getSimpleName();
    Random random = new Random();
    NetworkCommunicator networkCommunicator;
    ConcurrentHashMap<String, ServiceNodesHandler> serviceNodesMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, NodeConnectivityHandler> serverCRCIdNodeMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, NodeConnectivityHandler> serviceNodesCacheMap = new ConcurrentHashMap<>();
    ExecutorService sendingThreadPool;
    DiscoveryHostManager discoveryHostManager;
    InternalTools internalTools;

    public ServiceNodesManager(DiscoveryHostManager discoveryHostManager) {
        this.discoveryHostManager = discoveryHostManager;
    }

    ExecutorService getSendingThreadPool() {
        if(sendingThreadPool == null) {
            synchronized (ServiceNodesManager.class) {
                if(sendingThreadPool == null) {
                    String poolSizeStr = System.getProperty("starfish.discovery.packet.send.pool.size");
                    int poolSize = -1;
                    try {
                        poolSize = Integer.parseInt(poolSizeStr);
                    } catch (Throwable t) {
                    }
                    if(poolSize < 0)
                        poolSize =  NetRuntime.getCpuCores() * 2;;
                    LoggerEx.info(TAG, "starfish.discovery.packet.send.pool.size is " + poolSize);
                    ThreadFactory namedThreadFactory =
                            new ThreadFactoryBuilder().setNameFormat("PacketSending-ThreadPool-%d").build();
                    sendingThreadPool = Executors.newFixedThreadPool(poolSize, namedThreadFactory);
                }
            }
        }
        return sendingThreadPool;
    }

    public <K extends RequestTransport<R>, R extends ResponseTransport> CompletableFuture<ContentPacket<R>> sendContentPacket(ContentPacket<K> packet, Class<R> responseClass, String serviceKey) {
        ServiceNodesHandler serviceNodesHandler = serviceNodesMap.get(serviceKey);
        if(serviceNodesHandler == null) {
            synchronized (serviceNodesMap) {
                serviceNodesHandler = serviceNodesMap.get(serviceKey);
                if(serviceNodesHandler == null) {
                    serviceNodesHandler = new ServiceNodesHandler(this, serviceKey);
                    ServiceNodesHandler old = serviceNodesMap.putIfAbsent(serviceKey, serviceNodesHandler);
                    if(old != null) {
                        serviceNodesHandler = old;
                    } else {
                        serviceNodesHandler.start();
                    }
                }
            }
        }
        packet.setServiceKey(serviceKey);
        return serviceNodesHandler.sendContentPacket(packet, responseClass, serviceKey);
//        return ServiceInvocation.build(this, connectivityHandlerHelper).sendContentPacket(packet, responseClass, serviceKey);

//        NodeConnectivityHandler handler = serviceNodesCacheMap.get(serviceKey);
//        ServiceInvocation invocation = ServiceInvocation.build(this, handler);
//        CompletableFuture<R> future =  invocation.sendContentPacket(packet, responseClass, serviceKey);
//        future.whenComplete((r, throwable) -> {
//            NodeConnectivityHandler theHandler = invocation.getActualNodeConnectivityHandler();
//            if(theHandler != null && !theHandler.equals(handler)) {
//                serviceNodesCacheMap.put(serviceKey, theHandler);
//            }
//        });
    }

    public NetworkCommunicator getNetworkCommunicator() {
        return networkCommunicator;
    }

    public void setNetworkCommunicator(NetworkCommunicator networkCommunicator) {
        this.networkCommunicator = networkCommunicator;
    }
}
