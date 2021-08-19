package oceanus.sdk.core.discovery.impl.server;

import oceanus.sdk.core.common.CoreRuntime;
import oceanus.sdk.core.discovery.DiscoveryInfo;
import oceanus.sdk.core.discovery.DiscoveryManager;
import oceanus.sdk.core.discovery.data.discovery.*;
import oceanus.sdk.core.discovery.errors.DiscoveryErrorCodes;
import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.core.net.ContentPacketListener;
import oceanus.sdk.core.net.NetRuntime;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.adapters.data.ErrorPacket;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.utils.state.StateListener;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public final class DiscoveryManagerImpl extends DiscoveryManager {
    private static final String TAG = DiscoveryManagerImpl.class.getSimpleName();
//    private NetworkCommunicatorFactory networkCommunicatorFactory = NetRuntime.getNetworkCommunicatorFactory();
    ConcurrentHashMap<Long, Node> nodeMap = new ConcurrentHashMap<>();
    NetworkCommunicator networkCommunicator;
    ConcurrentHashMap<Long, Long> tentaclePingTimeMap = new ConcurrentHashMap<>();
    /**
     * {owner}_{project}_{service} as key
     */
    ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, ConcurrentSkipListSet<Long>> serviceNodesMap = new ConcurrentHashMap<>();
    private DiscoveryInfo discoveryInfo = new DiscoveryInfo(serviceNodesMap, nodeMap);
    final ConcurrentHashMap<Class<? extends RequestTransport<?>>, Class<? extends ContentPacketListener<? extends RequestTransport<?>>>> contentPacketHandlerMap = new ConcurrentHashMap<>();

    @Override
    public <T extends RequestTransport<?>, P extends ContentPacketListener<T>> DiscoveryManager registerContentPacketClass(Class<T> clazz, Class<P> packetListenerClass) {
        contentPacketHandlerMap.put(clazz, packetListenerClass);
        return this;
    }

    @Override
    public Map<String, Object> memory() {
        Map<String, Object> memoryMap = new HashMap<>();
        memoryMap.put("networkCommunicator", networkCommunicator.memory());
        memoryMap.put("nodeMap", nodeMap);
        memoryMap.put("tentaclePingTimeMap", tentaclePingTimeMap);
        memoryMap.put("serviceMap", serviceMap);
        memoryMap.put("serviceNodesMap", serviceNodesMap);
        memoryMap.put("contentPacketHandlerMap", contentPacketHandlerMap);
        return memoryMap;
    }

    @Override
    public DiscoveryInfo getDiscoveryInfo() {
        return discoveryInfo;
    }

    public DiscoveryManagerImpl() {
        registerContentPacketClass(NodeRegistrationRequest.class, NodeRegistrationServerHandler.class);
        registerContentPacketClass(ServiceRegistrationRequest.class, ServiceRegistrationServerHandler.class);
        registerContentPacketClass(FindServiceRequest.class, FindServiceServerHandler.class);
        registerContentPacketClass(GetNodeByServerCRCIdRequest.class, GetNodeByServerCRCIdHandler.class);
        registerContentPacketClass(LatencyCheckRequest.class, LatencyCheckHandler.class);
        registerContentPacketClass(NodeRegistrationRequest.class, NodeRegistrationServerHandler.class);
    }
    public DiscoveryManager addStateListener(StateListener<Integer, NetworkCommunicator> stateListener) {
        if(networkCommunicator != null) {
            networkCommunicator.addStateListener(stateListener);
        }
        return this;
    }

    public boolean removeStateListener(StateListener<Integer, NetworkCommunicator> stateListener) {
        if(networkCommunicator != null) {
            return networkCommunicator.removeStateListener(stateListener);
        }
        return false;
    }
    @Override
    public void start() throws IOException {
        start(port);
    }
    @Override
    public void start(int discoveryPort) throws IOException {
        if(networkCommunicator == null) {
            synchronized (this) {
                if(networkCommunicator == null) {
                    port = discoveryPort;
                    networkCommunicator = NetRuntime.buildNetworkCommunicator().startAtFixedPort(port);

                    /**
                     * Register server handlers here
                     */
                    contentPacketHandlerMap.forEach((BiConsumer<Class<? extends RequestTransport<?>>, Class<? extends ContentPacketListener<?>>>) (keyClass, valueClass) -> {
                        try {
                            Constructor<? extends ContentPacketListener<?>> constructor = valueClass.getConstructor(DiscoveryManager.class);
                            ContentPacketListener contentPacketListener = constructor.newInstance(DiscoveryManagerImpl.this);
                            networkCommunicator.addContentPacketListener(keyClass, contentPacketListener);
                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                            LoggerEx.error(TAG, "ContentPacketListener " + valueClass + " newInstance failed, " + e.getMessage());
                        }
                    });
//                    networkCommunicator
//                            .addContentPacketListener(NodeRegistrationRequest.class, new NodeRegistrationServerHandler(this))
//                            .addContentPacketListener(ServiceRegistrationRequest.class, new ServiceRegistrationServerHandler(this))
//                            .addContentPacketListener(FindServiceRequest.class, new FindServiceServerHandler(this))
//                            .addContentPacketListener(GetNodeByServerCRCIdRequest.class, new GetNodeByServerCRCIdHandler(this))
//                            .addContentPacketListener(LatencyCheckRequest.class, new LatencyCheckHandler(this));


                    networkCommunicator.addPingListener((serverIdCRC1, address1) -> {
                        Long time = tentaclePingTimeMap.get(serverIdCRC1);
                        Node node = nodeMap.get(serverIdCRC1);
                        if(node == null) {
                            tentaclePingTimeMap.remove(serverIdCRC1);
                            networkCommunicator.sendPacket(new ErrorPacket(DiscoveryErrorCodes.ERROR_UNKNOWN_NODE, "Unknown node").waitSeconds(10), address1).exceptionally(throwable -> {
                                throwable.printStackTrace();
                                LoggerEx.error(TAG, "Send back ErrorPacket for ERROR_UNKNOWN_NODE failed, " + throwable.getMessage() + " to address " + address1);
                                return null;
                            });
                        } else {
                            long currentTime = System.currentTimeMillis();
                            if(time == null || time < currentTime) {
                                tentaclePingTimeMap.put(serverIdCRC1, System.currentTimeMillis());
                            }
                            try {
//                                networkCommunicator.sendPacket(new ErrorPacket(ErrorCodes.ERROR_UNKNOWN_NODE, "Unknown node").waitSeconds(10), new InetSocketAddress(address1, port1));
                                networkCommunicator.ping(address1);
                            } catch (IOException e) {
                                e.printStackTrace();
                                LoggerEx.error(TAG, "Send back Ping failed, " + e.getMessage() + " to address " + address1);
                            }
                        }
                    });


//                    final long PING_TIMEOUT = TimeUnit.HOURS.toMillis(1);
                    final long PING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
                    internalTools.getScheduledExecutorService().scheduleAtFixedRate(() -> {
                        Set<Map.Entry<Long, Long>> entries = tentaclePingTimeMap.entrySet();
                        for(Map.Entry<Long, Long> entry : entries) {
                            Long time = entry.getValue();
                            Long key = entry.getKey();
                            if(key != null && (time == null || System.currentTimeMillis() - time > PING_TIMEOUT)) {
                                /**
                                 * Remove node information after 1 hour if no ping received. Means the node is dead totally.
                                 */
                                nodeDown(key);
                            }
                        }
                    }, CoreRuntime.PERIOD_CLEAN_PING_TIMEOUT, CoreRuntime.PERIOD_CLEAN_PING_TIMEOUT, TimeUnit.MILLISECONDS);
                }
            }
        }
//        try {

//            networkCommunicatorFactory.registerContentType(ClientHelloRequest.class);
//            networkCommunicatorFactory.startAsServer(port/*, createdNetworkCommunicator -> {//created
////                createdNetworkCommunicator.sendPacket();
//                createdNetworkCommunicator.addPacketListener(Packet.TYPE_OBJECT, receivedPacket -> {
//                    String contentType = receivedPacket.getContentType();
//                    if(contentType != null && !contentType.isBlank()) {
//                        switch (contentType) {
//                            case "ClientHelloRequest":
//                                ObjectPacket<ClientHelloRequest> clientHelloPacket = (ObjectPacket<ClientHelloRequest>) receivedPacket;
//
//                                break;
//                        }
//                    }
//                });
//            }, destroyedNetworkCommunicator -> {//destroyed
//
//            }*/);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    void nodeDown(Long key) {
        tentaclePingTimeMap.remove(key);
        nodeMap.remove(key);

        Collection<ConcurrentSkipListSet<Long>> values = serviceNodesMap.values();
        for(ConcurrentSkipListSet<Long> value : values) {
            value.remove(key);
        }
    }

    @Override
    public void stop() {
        if(networkCommunicator != null) {
            synchronized (this) {
                if(networkCommunicator != null) {
                    networkCommunicator.close();
                    networkCommunicator = null;
                }
            }
        }
    }

}
