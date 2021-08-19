package oceanus.sdk.core.net;

import oceanus.sdk.core.common.InternalTools;
import oceanus.sdk.core.discovery.data.FailedResponse;
import oceanus.sdk.core.discovery.errors.DiscoveryErrorCodes;
import oceanus.sdk.core.net.adapters.data.*;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.core.net.data.Transport;
import oceanus.sdk.core.net.errors.NetErrorCodes;
import oceanus.sdk.core.net.rudpex.communicator.RUDPEXNetworkCommunicator;
import oceanus.sdk.core.net.serializations.SerializationStreamHandler;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.utils.state.StateListener;
import oceanus.sdk.utils.state.StateMachine;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 */
public abstract class NetworkCommunicator {
    /**
     * 0 ~ 100
     */
    public static final short PACKET_TYPE_SERVER_NAME = 1;
    public static final short PACKET_TYPE_STRING = 2;
    public static final short PACKET_TYPE_CONTENT = 3;
    public static final short PACKET_TYPE_ERROR = 4;

    public static final int STATE_NONE = 0;
    public static final int STATE_SERVER = 1;
    public static final int STATE_CLIENT = 2;
    public static final int STATE_SHUTDOWN = 3;
    private static final String TAG = NetworkCommunicator.class.getSimpleName();
    protected int state = STATE_NONE;

    public static final int CONNECTIVITY_STATE_NONE = 1;
    public static final int CONNECTIVITY_STATE_CONNECTING = 10;
    public static final int CONNECTIVITY_STATE_CONNECTED = 100;
    public static final int CONNECTIVITY_STATE_DISCONNECTED = -100;
    public static final int CONNECTIVITY_STATE_TERMINATED = -200;
//    public static final int CONNECTIVITY_STATE_RECONNECTING = 88;
    protected StateMachine<Integer, NetworkCommunicator> connectStateMachine;

    protected String serverName;
    protected Long serverNameCRC;
    protected ConcurrentHashMap<Short, Class<? extends Packet>> typePacketMap = new ConcurrentHashMap<>();
//    private ConcurrentHashMap<String, Class<?>> contentTypeClassMap = new ConcurrentHashMap<>();

    protected CopyOnWriteArrayList<ClosedListener> closedListeners = new CopyOnWriteArrayList<>();
    protected CopyOnWriteArrayList<ConnectedListener> connectedListeners = new CopyOnWriteArrayList<>();
    protected CopyOnWriteArrayList<PacketListener> allPacketListeners = new CopyOnWriteArrayList<>();
    protected CopyOnWriteArrayList<PingListener> pingListeners = new CopyOnWriteArrayList<>();
    protected ConcurrentHashMap<Short, CopyOnWriteArrayList<PacketListener>> typePacketListeners = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Long, CopyOnWriteArrayList<ContentPacketListener>> classContentPacketListeners = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Long, ConcurrentHashMap<Long, CopyOnWriteArrayList<ContentPacketListener>>> serviceContentPacketListeners = new ConcurrentHashMap<>();

    protected InternalTools internalTools;

    public Map<String, Object> memory() {
        Map<String, Object> memoryMap = new HashMap<>();
        memoryMap.put("closedListeners", closedListeners);
        memoryMap.put("connectedListeners", connectedListeners);
        memoryMap.put("allPacketListeners", allPacketListeners);
        memoryMap.put("pingListeners", pingListeners);
        memoryMap.put("typePacketListeners", typePacketListeners);
        memoryMap.put("classContentPacketListeners", classContentPacketListeners);
        memoryMap.put("serviceContentPacketListeners", serviceContentPacketListeners);
        memoryMap.put("connectStateMachine", connectStateMachine);
        memoryMap.put("serverName", serverName);
        memoryMap.put("serverNameCRC", serverNameCRC);
        return memoryMap;
    }

    public NetworkCommunicator() {

        // 为contentPacket注册失败的listener
        addContentPacketListener(FailedResponse.class, (contentPacket, serverIdCRC, address) -> {
            FailedResponse response = contentPacket.getContent();
            if(response.getTransportId() == null) {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "response.getTransportId() is null for contentPacket " + contentPacket + " from " + address);
                return null;
            }
            if(!sendingRequestMap.containsKey(response.getTransportId())) {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "Unexpected ResponseTransport received, " + response + " as transportId " + response.getTransportId() + " is not in sendingMap, ignore...");
            } else {
                invokeContentPacketListener(response, null, contentPacket, address, serverIdCRC);
//                boolean done = false;
//                ContentPacketContainer<?> contentPacketContainer = sendingRequestMap.get(response.getTransportId());
//                if(contentPacketContainer != null) {
//                    synchronized (contentPacketContainer) {
//                        contentPacketContainer = sendingRequestMap.remove(response.getTransportId());
//                        if(contentPacketContainer != null) {
//                            LoggerEx.info(TAG, "ContentPacketListener address " + address + " sendingRequestMap remove id " + response.getTransportId() + " contentPacket " + contentPacket + " failedContentPacket " + failedContentPacket);
//                            done = contentPacketContainer.done();
//                        }
//                    }
//                }
//                if(done) {
//                    if(contentPacketContainer.responseListener != null) {
//                        try {
//                            contentPacketContainer.responseListener.responseReceived(null, contentPacket, serverIdCRC);
//                        } catch(Throwable t) {
//                            t.printStackTrace();
//                            LoggerEx.error(TAG, "Invoke responseReceived for ensureResponseContentPacketListener failed, " + t.getMessage() + " contentPacket " + contentPacket + " address " + address);
//                        }
//                    }
//                }
            }
            return null;
        });
    }

    // 删除request，并回调该request对应的listener
    private <R extends ResponseTransport> void invokeContentPacketListener(ResponseTransport response, ContentPacket<R> contentPacket, ContentPacket<FailedResponse> failedContentPacket, InetSocketAddress address, long serverIdCRC) {
        boolean done = false;
        ContentPacketContainer<R> contentPacketContainer = (ContentPacketContainer<R>) sendingRequestMap.get(response.getTransportId());
        if(contentPacketContainer != null) {
            synchronized (contentPacketContainer) {
                contentPacketContainer = (ContentPacketContainer<R>) sendingRequestMap.remove(response.getTransportId());
                if(contentPacketContainer != null) {
//                    if(failedContentPacket != null) {
//                        System.out.print("");
//                    }
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "ContentPacketListener address " + address + " sendingRequestMap remove id " + response.getTransportId() + " contentPacket " + contentPacket + " failedContentPacket " + failedContentPacket);
                    done = contentPacketContainer.done();
                }
            }
        }
        if(done) {
            if(contentPacketContainer.responseListener != null) {
                try {
                    contentPacketContainer.responseListener.responseReceived(contentPacket, failedContentPacket, serverIdCRC, address);
                } catch(Throwable t) {
                    t.printStackTrace();
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Invoke responseReceived for ensureResponseContentPacketListener failed, " + t.getMessage() + " contentPacket " + contentPacket + " address " + address);
                }
            }
        }
    }

    // 注册可处理的包类型
    public void init() {
        registerPacketType(PACKET_TYPE_SERVER_NAME, ServerNamePacket.class);
        registerPacketType(PACKET_TYPE_STRING, StringPacket.class);
        registerPacketType(PACKET_TYPE_CONTENT, ContentPacket.class);
        registerPacketType(PACKET_TYPE_ERROR, ErrorPacket.class);
    }

    public void registerPacketType(short type, Class<? extends Packet> packetClass) {
        if(packetClass == null) {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "packetClass is null while registerPacketType, type " + type);
            return;
        }

        typePacketMap.put(type, packetClass);
//        LoggerEx.info(TAG, "registerPacketType " + type + " packetClass " + packetClass + " for " + this);
    }

    public void unregisterPacketType(short type) {
        typePacketMap.remove(type);
    }

//    protected void cloneContentTypeClassMap(Map<String, Class<?>> map) {
//        if(map != null) {
//            contentTypeClassMap.putAll(map);
//        }
//    }
//
//    public void registerContentType(String contentType, Class<?> contentClass) {
//        contentTypeClassMap.put(contentType, contentClass);
//    }
//
//    public Class<?> unregisterContentType(String contentType) {
//        return contentTypeClassMap.remove(contentType);
//    }

    // 解包，type(2 bytes) + contentType(8 bytes) + serviceKeyExists(1 byte) /* + serviceKey(8 bytes) */ + length(4 bytes) + content(length bytes)
    protected Packet resurrectPacket(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        short type = dis.readShort();
        long contentType = 0;
        Long serviceKey = null;
        if(type == PACKET_TYPE_CONTENT) {
            contentType = dis.readLong();
            byte serviceKeyExists = dis.readByte();
            if(serviceKeyExists == 1) {
                serviceKey = dis.readLong();
            }
        }
        Class<? extends Packet> packetClass = typePacketMap.get(type);
        if(packetClass != null) {
            switch (type) {
                case PACKET_TYPE_CONTENT:
                    Class<?> contentClass = ContentPacket.getCRCClass(contentType);
                    if(contentClass != null) {
                        SerializationStreamHandler serializationStreamHandler = NetRuntime.getSerializationStreamHandler();
                        Object content = serializationStreamHandler.convert(is, contentClass);
                        if(content != null) {
                            return ContentPacket.buildWithTypeAndContentAndServiceKey(contentType, content, serviceKey);
                        } else {
                            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "resurrectPacket failed, because of content can not be parsed from contentClass " + contentClass + " contentType " + contentType + " type " + type);
                        }
                    } else {
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "resurrectPacket failed, because of contentClass is not found for contentType " + contentType + " type " + type);
                    }
                    break;
//                case PACKET_TYPE_SERVER_NAME:
//                case PACKET_TYPE_STRING:
//                case PACKET_TYPE_ERROR:
//                    return NetRuntime.getSerializationStreamHandler().convert(is, packetClass);
                default:
                    return NetRuntime.getSerializationStreamHandler().convert(is, packetClass);
//                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "resurrectPacket failed, because of packetType is illegal, contentType " + contentType + " type " + type);
//                    break;
            }
        }
        return null;
    }

    // 封包
    protected void persistentPacket(Packet packet, OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeShort(packet.getType());
        if(packet.getType() == PACKET_TYPE_CONTENT) {
            dos.writeLong(((ContentPacket)packet).getContentType());
            Long serviceKey = ((ContentPacket)packet).getServiceKey();
            if(serviceKey != null) {
                dos.write(1);
                dos.writeLong(serviceKey);
            } else {
                dos.write(0);
            }
        }
        SerializationStreamHandler serializationStreamHandler = NetRuntime.getSerializationStreamHandler();
        switch (packet.getType()) {
            case PACKET_TYPE_CONTENT:
                ContentPacket contentPacket = (ContentPacket) packet;
                serializationStreamHandler.convert(contentPacket.getContent(), os);
                break;
            default:
                serializationStreamHandler.convert(packet, os);
                break;
        }
//        serializationStreamHandler.convert(packet, os);
        dos.flush();
    }

    /**
     * If server behind NAT, can not fix a public port, then open at any port. Starfish network will do hole punching.
     *
     * @throws IOException
     */
    public abstract NetworkCommunicator startAtAnyPort() throws IOException;
    public abstract NetworkCommunicator startAtFixedPort(int port) throws IOException;
//    protected abstract CompletableFuture<NetworkCommunicator> startClient(String host, int port);
    protected abstract void stop();
    public abstract CompletableFuture<Void> sendPacket(Packet packet, SocketAddress address);

    public abstract void ping(SocketAddress address) throws IOException;

    // 将要发送的包组织成此类，并放到内存中
    private class ContentPacketContainer<K extends ResponseTransport> {
        public ContentPacketContainer(ContentPacketResponseListener<K> responseListener, String transportId) {
            this.responseListener = responseListener;
//            this.scheduleTask = scheduleTask;
            this.transportId = transportId;
            taskStatus = TASK_STATUS_PENDING;
        }
        ContentPacketResponseListener<K> responseListener;
        ScheduledFuture<?> scheduleTask;
        int taskStatus;
        String transportId;

        static final int TASK_STATUS_PENDING = 1;
        static final int TASK_STATUS_DONE = 2;
        static final int TASK_STATUS_CANCELED = 3;
        static final int TASK_STATUS_TIMEOUT = 4;
        boolean done() {
            if(taskStatus == TASK_STATUS_PENDING) {
                synchronized (this) {
                    if(taskStatus == TASK_STATUS_PENDING) {
                        if(scheduleTask != null) {
                            scheduleTask.cancel( true);
                            scheduleTask = null;
                        }
                        taskStatus = TASK_STATUS_DONE;
                        return true;
                    }
                }
            }
            return false;
        }

        void cancel() {
            if(taskStatus == TASK_STATUS_PENDING) {
                synchronized (this) {
                    if(taskStatus == TASK_STATUS_PENDING) {
                        if(scheduleTask != null) {
                            scheduleTask.cancel(true);
                            scheduleTask = null;
                        }
                        taskStatus = TASK_STATUS_CANCELED;
                    }
                }
            }
        }

        void timeout() {
            if(taskStatus == TASK_STATUS_PENDING) {
                synchronized (this) {
                    if(taskStatus == TASK_STATUS_PENDING) {
                        taskStatus = TASK_STATUS_TIMEOUT;
                    }
                }
            }
        }
    }

    // 监听contentPacket的response的listener
    public interface ContentPacketResponseListener<T extends ResponseTransport> {
        void responseReceived(ContentPacket<T> response, ContentPacket<? extends FailedResponse> failedResponse, Long serverIdCRC, InetSocketAddress address);
    }
    private ConcurrentHashMap<Class<? extends ResponseTransport>, ContentPacketListener<?>> responseListenerMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ContentPacketContainer<?>> sendingRequestMap = new ConcurrentHashMap<>();

    // 默认20秒的超时发送requestTransport消息
    public <K extends RequestTransport<?>, R extends ResponseTransport> void sendRequestTransport(ContentPacket<K> packet, InetSocketAddress address, Class<R> responseClass, ContentPacketResponseListener<R> responseListener) {
        sendRequestTransport(packet, address, responseClass, responseListener, 20000L);
    }

    // 自定义超时发送requestTransport消息
    public <K extends RequestTransport<?>, R extends ResponseTransport> void sendRequestTransport(ContentPacket<K> packet, final InetSocketAddress address, Class<R> responseClass, ContentPacketResponseListener<R> responseListener, long timeout) {
        if(packet == null || packet.getContent() == null || packet.getContent().getTransportId() == null) {
            throw new IllegalArgumentException("ContentPacket for sending is illegal, " + packet);
        }
        RequestTransport<?> requestTransport = packet.getContent();
        if(sendingRequestMap.containsKey(requestTransport.getTransportId())) {
//            LoggerEx.warn(TAG, "ContentPacket is sending " + packet + " ignore...");
            if(responseListener != null) {
                try {
                    responseListener.responseReceived(null, ContentPacket.buildWithContent(requestTransport.generateFailedResponse(DiscoveryErrorCodes.ERROR_PACKET_IS_SENDING, "packet is sending")), null, address);
                } catch(Throwable t) {
                    t.printStackTrace();
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Invoke responseReceived for sendContentPacket failed, " + t.getMessage() + " packet " + packet + " address " + address + " timeout " + timeout);
                }
            }
            return;
        }
        Class<R> responseTransportClass = (Class<R>) requestTransport.getResponseTransportClass(packet.getContent().getClass());
        final ContentPacketContainer<R> contentPacketContainer = new ContentPacketContainer<>(responseListener, requestTransport.getTransportId());
        ensureResponseContentPacketListener(responseTransportClass);
//        contentPacketContainer.scheduleTask = internalTools.getTimer().schedule(() -> {
//            if(contentPacketContainer.taskStatus == ContentPacketContainer.TASK_STATUS_PENDING) {
//                synchronized (contentPacketContainer) {
//                    if(contentPacketContainer.taskStatus == ContentPacketContainer.TASK_STATUS_PENDING) {
//                        sendingRequestMap.remove(contentPacketContainer.transportId);
//                        LoggerEx.info(TAG, "Timeout sendingRequestMap remove transportId " + contentPacketContainer.transportId);
//                        contentPacketContainer.timeout();
//                        if(contentPacketContainer.responseListener != null) {
//                            try {
//                                contentPacketContainer.responseListener.responseReceived(null, ContentPacket.buildWithContent(new TimeoutResponse(DiscoveryErrorCodes.ERROR_TIMEOUT, "timeout")), null);
//                            } catch(Throwable t) {
//                                t.printStackTrace();
//                                LoggerEx.error(TAG, "Invoke responseReceived for sendContentPacket failed, " + t.getMessage() + " packet " + packet + " address " + address + " timeout " + timeout);
//                            }
//                        }
//                    } else {
//                        LoggerEx.warn(TAG, "This scheduleTask shall be canceled already, should not run here. transportId " + contentPacketContainer.transportId);
//                    }
//                }
//            }
//        }, timeout);
        ContentPacketContainer<?> old = sendingRequestMap.putIfAbsent(requestTransport.getTransportId(), contentPacketContainer);
        if(old != null) {
            contentPacketContainer.cancel();
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "sendingRequestMap put requestTransport id " + requestTransport.getTransportId() + " already exists, scheduleTask id " + contentPacketContainer.scheduleTask + " has been be cancelled");
        } else {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendContentPacket packet " + packet + " address " + address + " sendingRequestMap put id " + requestTransport.getTransportId());
            sendPacket(packet, address).whenComplete((aVoid, throwable) -> {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendContentPacket completed " + packet + " address " + address + " id " + requestTransport.getTransportId() + " will wait response. timeout " + timeout);
                contentPacketContainer.scheduleTask = internalTools.getScheduledExecutorService().schedule(() -> {
                    handleContentPacketFailed(contentPacketContainer, requestTransport, requestTransport.generateTimeoutResponse(NetErrorCodes.ERROR_TIMEOUT, "Timeout after " + timeout), address);
                }, timeout, TimeUnit.MILLISECONDS);
            }).exceptionally(throwable -> {
                handleContentPacketFailed(contentPacketContainer, requestTransport, requestTransport.generateTimeoutResponse(NetErrorCodes.ERROR_PACKET_SEND_FAILED, throwable.getMessage()), address);
                return null;
            });
        }
    }

    // 发送超时后的处理：删除正在发送的request，并调用该request对应的listener
    private <R extends ResponseTransport> void handleContentPacketFailed(ContentPacketContainer<R> contentPacketContainer, RequestTransport<?> requestTransport, FailedResponse failedResponse, InetSocketAddress address) {
        if(contentPacketContainer.taskStatus == ContentPacketContainer.TASK_STATUS_PENDING) {
            boolean bool = false;
            synchronized (contentPacketContainer) {
                if(contentPacketContainer.taskStatus == ContentPacketContainer.TASK_STATUS_PENDING) {
                    boolean removed = sendingRequestMap.remove(contentPacketContainer.transportId, contentPacketContainer);
                    if(removed) {
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "Timeout sendingRequestMap remove transportId " + contentPacketContainer.transportId);
                        contentPacketContainer.timeout();
                        bool = true;
                    } else {
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "contentPacketContainer shall be removed by transportId " + contentPacketContainer.transportId + " but removed is false, ignore... contentPacketContainer " + contentPacketContainer);
                    }
                } else {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "This scheduleTask shall be canceled already, should not run here. transportId " + contentPacketContainer.transportId);
                }
            }
            if(bool) {
                if(contentPacketContainer.responseListener != null) {
                    try {
                        contentPacketContainer.responseListener.responseReceived(null, ContentPacket.buildWithContent(failedResponse), null, address);
                    } catch(Throwable t) {
                        t.printStackTrace();
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Invoke responseReceived for sendContentPacket failed, " + t.getMessage() + " requestTransport " + requestTransport);
                    }
                }
            }
        }
    }

    // 确定contentPacket中的requestTransport（content）是否添加了listener
    private <R extends ResponseTransport> void ensureResponseContentPacketListener(Class<R> responseTransportClass) {
        if(!responseListenerMap.containsKey(responseTransportClass)) {
            ContentPacketListener<R> contentPacketListener = (contentPacket, serverIdCRC, address) -> {
                R response = contentPacket.getContent();
                if(response.getTransportId() == null || !sendingRequestMap.containsKey(response.getTransportId())) {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "Unexpected ResponseTransport received, " + response + " as transportId " + response.getTransportId() + " is not in sendingMap, ignore...");
                } else {
                    invokeContentPacketListener(response, contentPacket, null, address, serverIdCRC);
//                    boolean done = false;
//                    ContentPacketContainer<R> contentPacketContainer = (ContentPacketContainer<R>) sendingRequestMap.get(response.getTransportId());
//                    if(contentPacketContainer != null) {
//                        synchronized (contentPacketContainer) {
//                            contentPacketContainer = (ContentPacketContainer<R>) sendingRequestMap.remove(response.getTransportId());
//                            if(contentPacketContainer != null) {
//                                LoggerEx.info(TAG, "ContentPacketListener address " + address + " sendingRequestMap remove id " + response.getTransportId() + " contentPacket " + contentPacket + " failedContentPacket " + failedContentPacket);
//                                done = contentPacketContainer.done();
//                            }
//                        }
//                    }
//                    if(done) {
//                        if(contentPacketContainer.responseListener != null) {
//                            try {
//                                contentPacketContainer.responseListener.responseReceived(contentPacket, null, serverIdCRC);
//                            } catch(Throwable t) {
//                                t.printStackTrace();
//                                LoggerEx.error(TAG, "Invoke responseReceived for ensureResponseContentPacketListener failed, " + t.getMessage() + " contentPacket " + contentPacket + " address " + address);
//                            }
//                        }
//                    }
                }
                return null;
            };
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "responseListenerMap put responseTransportClass " + responseTransportClass + " contentPacketListener " + contentPacketListener);
            ContentPacketListener<?> old = responseListenerMap.putIfAbsent(responseTransportClass, contentPacketListener);
            if(old == null) {
                addContentPacketListener(responseTransportClass, contentPacketListener);
            }
        }
    }

    public void close() {
        state = STATE_SHUTDOWN;
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "NetworkCommunicator closed for " + this);
        allPacketListeners.clear();
        typePacketListeners.clear();
        closedListeners.clear();
        connectedListeners.clear();
        stop();
    }
//    public void sendPacket(Packet packet) throws IOException {
//        sendPacketInternally(packet);
//    }

    // 添加接收packet的listener
    public NetworkCommunicator addAllPacketListener(PacketListener packetListener) {
        if(!allPacketListeners.contains(packetListener)) {
            allPacketListeners.add(packetListener);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "addPacketListener " + packetListener + " for " + this);
        }
        return this;
    }

    public boolean removeAllPacketListener(PacketListener packetListener) {
        return allPacketListeners.remove(packetListener);
    }

    // 添加接收ping的listener
    public NetworkCommunicator addPingListener(PingListener pingListener) {
        if(!pingListeners.contains(pingListener)) {
            pingListeners.add(pingListener);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "pingListeners " + pingListener + " for " + this);
        }
        return this;
    }

    public boolean removePingListener(PingListener pingListener) {
        if(pingListeners != null) {
            return pingListeners.remove(pingListener);
        }
        return false;
    }

    // 处理收到的packet
    protected void packetReceived(Packet packet, long serverIdCRC, InetSocketAddress inetAddress) {
        if(allPacketListeners != null) {
            for (PacketListener packetListener : allPacketListeners) {
                try {
                    packetListener.packetReceived(packet, serverIdCRC, inetAddress);
                } catch (Throwable t) {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Packet received for allPacketListener " + packetListener + " failed, " + t.getMessage() + " ignore the error to next listeners");
                }
            }
            short type = packet.getType();
            switch (type) {
                case PACKET_TYPE_CONTENT:
                    ContentPacket<? extends Transport> contentPacket = (ContentPacket<? extends Transport>) packet;
                    Transport transport = contentPacket.getContent();
                    executeServiceContentPacketListener(contentPacket, serverIdCRC, inetAddress);
                    executeContentPacketListener(contentPacket, serverIdCRC, inetAddress);
                    // 如果没有listener处理这条消息，就通知发送方失败response
                    if(!contentPacket.isCommitted() && transport instanceof RequestTransport) {
                        RequestTransport<?> requestTransport = (RequestTransport<?>) transport;
                        ResponseTransport responseTransport = requestTransport.generateFailedResponse(DiscoveryErrorCodes.ERROR_NO_RESULT, "No result");
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "Send back response " + responseTransport + " for request " + requestTransport + " id " + requestTransport.getTransportId());
                        sendPacket(requestTransport, responseTransport, inetAddress).exceptionally(e -> {
                            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send back (packetReceived) " + responseTransport.getClass().getSimpleName() + " failed, " + e.getMessage() + " to address " + inetAddress + " request " + requestTransport + " response " + responseTransport + " id " + requestTransport.getTransportId());
                            return null;
                        });
                    }
                    break;
//                case PACKET_TYPE_STRING:
//                    break;
            }
            executePacketListener(type, packet, serverIdCRC, inetAddress);
        }
    }

    // 通知监听contentPacket的service，并向发送方发送response
    private <T extends Transport> void executeServiceContentPacketListener(ContentPacket<T> contentPacket, long serverIdCRC, InetSocketAddress inetAddress) {
        Long serviceCRC = contentPacket.getServiceKey();
        if(serviceCRC != null) {
            ConcurrentHashMap<Long, CopyOnWriteArrayList<ContentPacketListener>> classListeners = serviceContentPacketListeners.get(serviceCRC);
            if(classListeners != null) {
                long classCRC = contentPacket.getContentType();
                CopyOnWriteArrayList<ContentPacketListener> listeners = classListeners.get(classCRC);
                if(listeners != null) {
                    for (ContentPacketListener listener : listeners) {
                        try {
//                            listener.contentPacketReceived(contentPacket, serverIdCRC, inetAddress, port);
                            T transport = contentPacket.getContent();
                            ResponseTransport responseTransport = listener.contentPacketReceived(contentPacket, serverIdCRC, inetAddress);
                            if(transport instanceof RequestTransport) {
                                if(responseTransport != null && !contentPacket.isCommitted()) {
                                    contentPacket.setCommitted(true);
                                    sendPacket((RequestTransport) transport, responseTransport, inetAddress).exceptionally(e -> {
                                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send back (executeServiceContentPacketListener) " + responseTransport.getClass().getSimpleName() + " failed, " + e.getMessage() + " to address " + inetAddress + " request " + transport + " response " + responseTransport);
                                        return null;
                                    });
                                }
                            }
                        } catch(Throwable t) {
                            t.printStackTrace();
                            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Receive ContentPacket failed, " + t.getMessage() + " for ContentPacketListener " + listener + " packet " + contentPacket);
                        }
                    }
                }
            }
        }
    }

    // 通知监听contentPacket的listener，并向发送方发送response
    private <T extends Transport> void executeContentPacketListener(ContentPacket<T> contentPacket, long serverIdCRC, InetSocketAddress inetAddress) {
        long classCRC = contentPacket.getContentType();

        CopyOnWriteArrayList<ContentPacketListener> listeners = classContentPacketListeners.get(classCRC);
        if(listeners != null) {
            for (ContentPacketListener listener : listeners) {
//                try {
                T transport = contentPacket.getContent();
//                    LoggerEx.info(TAG, "contentPacketReceived transport " + transport);
                ResponseTransport responseTransport = listener.contentPacketReceived(contentPacket, serverIdCRC, inetAddress);
                if(transport instanceof RequestTransport) {
                    if(responseTransport != null && !contentPacket.isCommitted()) {
                        contentPacket.setCommitted(true);
                        sendPacket((RequestTransport) transport, responseTransport, inetAddress).exceptionally(e -> {
                            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send back (executeContentPacketListener) " + responseTransport.getClass().getSimpleName() + " failed, " + e.getMessage() + " to address " + inetAddress + " request " + transport + " response " + responseTransport);
                            return null;
                        });
                    }
                }
//                } catch(Throwable t) {
//                    t.printStackTrace();
//                    LoggerEx.error(TAG, "Receive ContentPacket failed, " + t.getMessage() + " for ContentPacketListener " + listener + " packet " + contentPacket);
//                }
            }
        }
    }

    // 发送消息
    private CompletableFuture<Void> sendPacket(RequestTransport requestTransport, ResponseTransport responseTransport, InetSocketAddress inetSocketAddress) {
        if(responseTransport.getTransportId() == null)
            responseTransport.setTransportId(requestTransport.getTransportId());
        return this.sendPacket(ContentPacket.buildWithContent(responseTransport), inetSocketAddress);

//        try {
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            LoggerEx.error(TAG, "Send back " + responseTransport.getClass().getSimpleName() + " failed, " + e.getMessage() + " to address " + inetSocketAddress + " response " + responseTransport);
//            responseTransport = requestTransport.generateFailedResponse(DiscoveryErrorCodes.ERROR_IO, "send response failed");
//            try {
//                this.sendPacket(ContentPacket.buildWithContent(responseTransport), inetSocketAddress);
//            } catch (IOException ex) {
//                ex.printStackTrace();
//                LoggerEx.error(TAG, "Send back FailedResponse for " + responseTransport.getClass().getSimpleName() + " failed, " + e.getMessage() + " to address " + inetSocketAddress + " response " + responseTransport);
//            }
//        }
//        return null;
    }

    // 通知监听某种类型消息的listener
    private void executePacketListener(short type, Packet packet, long serverIdCRC, InetSocketAddress inetAddress) {
        CopyOnWriteArrayList<PacketListener> packetListeners = typePacketListeners.get(type);
        if(packetListeners != null) {
            for(PacketListener packetListener : packetListeners) {
                try {
                    packetListener.packetReceived(packet, serverIdCRC, inetAddress);
                } catch(Throwable t) {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Receive ContentPacket failed, " + t.getMessage() + " for PacketListener " + packetListener + " packet " + packet);
                }
            }
        }
    }

    // 通知监听ping的listener
    protected void executePingListener(long serverIdCRC, InetSocketAddress inetAddress) {
        if(pingListeners != null) {
            for(PingListener pingListener : pingListeners) {
                try {
                    pingListener.pingReceived(serverIdCRC, inetAddress);
                } catch(Throwable t) {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Receive ping failed, " + t.getMessage() + " for PingListener " + pingListener);
                }
            }
        }
    }

    public <T> boolean hasContentPacketListener(Class<T> clazz, ContentPacketListener<T> packetListener) {
        long classCRC = ContentPacket.getClassCRC(clazz);
        CopyOnWriteArrayList<ContentPacketListener> listeners = classContentPacketListeners.get(classCRC);
        return listeners != null && listeners.contains(packetListener);
    }

    // 为某个service添加contentPacket的listener
    public <T> NetworkCommunicator addServiceContentPacketListener(String serviceKey, Class<T> clazz, ContentPacketListener<T> packetListener) {
        long serviceKeyCRC = ContentPacket.getServiceKeyCRC(serviceKey);
        ConcurrentHashMap<Long, CopyOnWriteArrayList<ContentPacketListener>> serviceListeners = serviceContentPacketListeners.get(serviceKeyCRC);
        if(serviceListeners == null) {
            serviceListeners = new ConcurrentHashMap<>();
            ConcurrentHashMap<Long, CopyOnWriteArrayList<ContentPacketListener>> old = serviceContentPacketListeners.putIfAbsent(serviceKeyCRC, serviceListeners);
            if(old != null) {
                serviceListeners = old;
            }
        }
        long classCRC = ContentPacket.getClassCRC(clazz);
        CopyOnWriteArrayList<ContentPacketListener> listeners = serviceListeners.get(classCRC);
        if(listeners == null) {
            listeners = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<ContentPacketListener> old = serviceListeners.putIfAbsent(classCRC, listeners);
            if(old != null) {
                listeners = old;
            }
        }
        if(!listeners.contains(packetListener)) {
            listeners.add(packetListener);
        }
        return this;
    }

    public <T> void removeServiceContentPacketListener(String serviceKey, Class<T> clazz, ContentPacketListener<T> packetListener) {
        long serviceKeyCRC = ContentPacket.getServiceKeyCRC(serviceKey);
        ConcurrentHashMap<Long, CopyOnWriteArrayList<ContentPacketListener>> serviceListeners = serviceContentPacketListeners.get(serviceKeyCRC);
        if(serviceListeners != null) {
            long classCRC = ContentPacket.getClassCRC(clazz);
            CopyOnWriteArrayList<ContentPacketListener> listeners = serviceListeners.get(classCRC);
            if(listeners != null) {
                listeners.remove(packetListener);
            }
        }
    }

    public void removeServiceContentPacketListeners(String serviceKey) {
        long serviceKeyCRC = ContentPacket.getServiceKeyCRC(serviceKey);
        serviceContentPacketListeners.remove(serviceKeyCRC);
    }

    // 为contentPacket中的requestTransport（content）添加listener
    public <T> NetworkCommunicator addContentPacketListener(Class<T> clazz, ContentPacketListener<T> packetListener) {
        long classCRC = ContentPacket.getClassCRC(clazz);
        CopyOnWriteArrayList<ContentPacketListener> listeners = classContentPacketListeners.get(classCRC);
        if(listeners == null) {
            listeners = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<ContentPacketListener> old = classContentPacketListeners.putIfAbsent(classCRC, listeners);
            if(old != null) {
                listeners = old;
            }
        }
        if(!listeners.contains(packetListener)) {
            listeners.add(packetListener);
        }
        return this;
    }

    public <T> boolean removeContentPacketListener(Class<T> clazz, PacketListener<ContentPacket<T>> packetListener) {
        long classCRC = ContentPacket.getClassCRC(clazz);
        CopyOnWriteArrayList<ContentPacketListener> listeners = classContentPacketListeners.get(classCRC);
        if(listeners != null)
            return listeners.remove(packetListener);
        return false;
    }

    public boolean removeContentPacketListeners(Class<?> clazz) {
        long classCRC = ContentPacket.getClassCRC(clazz);
        CopyOnWriteArrayList<ContentPacketListener> listeners = classContentPacketListeners.get(classCRC);
        if(listeners != null) {
            listeners.clear();
            //Don't remove the key with the empty list to avoid possible thread unsafe issue.
            return true;
        }
        return false;
    }

    // 为stringPacket这种类型添加listener
    public NetworkCommunicator addStringPacketListener(PacketListener<StringPacket> packetListener) {
        return addPacketListener(PACKET_TYPE_STRING, packetListener);
    }


    public boolean removePacketListener(short type, PacketListener<StringPacket> packetListener) {
        CopyOnWriteArrayList<PacketListener> listeners = typePacketListeners.get(type);
        if(listeners != null)
            return listeners.remove(packetListener);
        return false;
    }

    public boolean removePacketListeners(short type) {
        CopyOnWriteArrayList<PacketListener> listeners = typePacketListeners.get(type);
        if(listeners != null) {
            listeners.clear();
            //Don't remove the key with the empty list to avoid possible thread unsafe issue.
            return true;
        }
        return false;
    }

    public NetworkCommunicator addPacketListener(short type, PacketListener packetListener) {
        CopyOnWriteArrayList<PacketListener> listeners = typePacketListeners.get(type);
        if(listeners == null) {
            listeners = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<PacketListener> old = typePacketListeners.putIfAbsent(type, listeners);
            if(old != null) {
                listeners = old;
            }
        }
        if(!listeners.contains(packetListener)) {
            listeners.add(packetListener);
        }
        return this;
    }

    // 添加connected的listener
    public NetworkCommunicator addConnectedListener(ConnectedListener connectedListener) {
        if(!connectedListeners.contains(connectedListener)) {
            connectedListeners.add(connectedListener);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "addConnectedListener " + connectedListener + " for " + this);
        }
        return this;
    }

    public boolean removeConnectedListener(ConnectedListener connectedListener) {
        return connectedListeners.remove(connectedListener);
    }

    // 添加close的listener
    public NetworkCommunicator addClosedListener(ClosedListener closedListener) {
        if(!closedListeners.contains(closedListener)) {
            closedListeners.add(closedListener);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "addClosedListener " + closedListener + " for " + this);
        }
        return this;
    }
    public boolean removeClosedListener(ClosedListener closedListener) {
        return closedListeners.remove(closedListener);
    }

    public interface PingListener {
        void pingReceived(long serverIdCRC, InetSocketAddress address);
    }

    public interface ClosedListener {
        void closed();
    }
    public interface ConnectedListener {
        void connected();
    }
    public String getServerName() {
        return serverName;
    }

    public Long getServerNameCRC() {
        return serverNameCRC;
    }

    public int getConnectStateMachine() {
        return connectStateMachine.getCurrentState();
    }

    // 状态机管理
    public NetworkCommunicator addStateListener(StateListener<Integer, NetworkCommunicator> stateListener) {
        if(connectStateMachine != null) {
            connectStateMachine.addStateListener(stateListener);
        }
        return this;
    }

    public boolean removeStateListener(StateListener<Integer, NetworkCommunicator> stateListener) {
        if(connectStateMachine != null) {
            return connectStateMachine.removeStateListener(stateListener);
        }
        return false;
    }
}
