package oceanus.sdk.core.discovery.impl.client;


import oceanus.sdk.core.common.CoreRuntime;
import oceanus.sdk.core.discovery.NodeRegistrationHandler;
import oceanus.sdk.core.discovery.data.FailedResponse;
import oceanus.sdk.core.discovery.data.discovery.*;
import oceanus.sdk.core.discovery.errors.DiscoveryErrorCodes;
import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.discovery.node.Service;
import oceanus.sdk.core.discovery.node.ServiceNodeResult;
import oceanus.sdk.core.net.NetRuntime;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.adapters.data.ErrorPacket;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.utils.ChatUtils;
import oceanus.sdk.utils.state.StateMachine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class NodeRegistrationHandlerImpl extends NodeRegistrationHandler {
//    private NetworkCommunicatorFactory networkCommunicatorFactory = NetRuntime.getNetworkCommunicatorFactory();
    private static final String TAG = NodeRegistrationHandlerImpl.class.getSimpleName();

    private int publicUdpPort;
    private NetworkCommunicator networkCommunicator;
//    private String aquamanHost;
//    private int aquamanPort;
//    private InetSocketAddress aquamanAddress;
    private DiscoveryHostManager discoveryHostManager;
    private Node node;
    private ScheduledFuture<?> pingTask;
    private ErrorPacket errorPacket;

    private ConcurrentHashMap<Long, Long> aquamanPingTimeMap = new ConcurrentHashMap<>();
    private CompletableFuture<NodeRegistrationHandler> startNodeFuture;
    private final int MAX_RETRIES_WHEN_START = 10, MAX_RETRIES_AFTER_CONNECTED = Integer.MAX_VALUE;

    private int maxRetries = MAX_RETRIES_WHEN_START;
    private int retryTimes = 0;

    /**
     * RUDP适合DiscoveryService进行通信， 节点和节点之间仍然时候RMI通信， 确保稳定性。
     *
     * @Deprecated
     */
    private ServiceNodesManager serviceNodesManager;
    private int rpcPort;
    private String rpcIp;
    private ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> memory() {
        Map<String, Object> memoryMap = new HashMap<>();
        memoryMap.put("discoveryHostManager", discoveryHostManager.memory());
        memoryMap.put("node", node);
        memoryMap.put("aquamanPingTimeMap", aquamanPingTimeMap);
        memoryMap.put("serviceMap", serviceMap);
        memoryMap.put("networkCommunicator", networkCommunicator.memory());
        memoryMap.put("pingTask", pingTask);
        memoryMap.put("errorPacket", errorPacket);
        return memoryMap;
    }

    public void init(int publicUdpPort) {
        retryTimes = 0;
        maxRetries = MAX_RETRIES_WHEN_START;
        if(publicUdpPort == -1) {
            this.publicUdpPort = publicUdpPort;
        } else if(publicUdpPort >= 0 && publicUdpPort <= 65535) {
            this.publicUdpPort = publicUdpPort;
        } else {
            throw new IllegalArgumentException("public UDP port illegal, " + publicUdpPort);
        }

        connectivityState = new StateMachine<>("NodeRegistrationHandlerImpl#" + publicUdpPort, CONNECTIVITY_STATE_NONE, this);
        connectivityState
                .configState(CONNECTIVITY_STATE_NONE, connectivityState.execute()
                        .nextStates(CONNECTIVITY_STATE_INITIALIZING))
                .configState(CONNECTIVITY_STATE_TERMINATED, connectivityState.execute(this::handleTerminated)
                        .nextStates(CONNECTIVITY_STATE_INITIALIZING))
                .configState(CONNECTIVITY_STATE_INITIALIZING, connectivityState.execute(this::handleInitializing)
                        .nextStates(CONNECTIVITY_STATE_CONNECTED, CONNECTIVITY_STATE_DISCONNECTED, CONNECTIVITY_STATE_HOLE_PUNCHING))
                .configState(CONNECTIVITY_STATE_HOLE_PUNCHING, connectivityState.execute(this::handleHolePunching)
                        .nextStates(CONNECTIVITY_STATE_CONNECTED, CONNECTIVITY_STATE_DISCONNECTED))
                .configState(CONNECTIVITY_STATE_CONNECTED, connectivityState.execute(this::handleConnected).leaveState(this::handleLeaveConnected)
                        .nextStates(CONNECTIVITY_STATE_INITIALIZING, CONNECTIVITY_STATE_DISCONNECTED))
                .configState(CONNECTIVITY_STATE_DISCONNECTED, connectivityState.execute(this::handleDisconnected)
                        .nextStates(CONNECTIVITY_STATE_INITIALIZING, CONNECTIVITY_STATE_TERMINATED))
//                .configState(CONNECTIVITY_STATE_RECONNECTING, connectivityState.newState(this::handleReconnecting)
//                        .nextStates(CONNECTIVITY_STATE_CONNECTED, CONNECTIVITY_STATE_DISCONNECTED))
                .errorOccurred(this::handleError);
    }

    private void handleTerminated(NodeRegistrationHandler nodeRegistrationHandler, StateMachine<Integer, NodeRegistrationHandler> integerNodeRegistrationHandlerStateMachine) {
        if(startNodeFuture != null) {
            startNodeFuture.completeExceptionally(new IllegalStateException("Terminated"));
            startNodeFuture = null;
        }
        retryTimes = 0;
    }

    private void handleHolePunching(NodeRegistrationHandler nodeRegistrationHandler, StateMachine<Integer, NodeRegistrationHandler> integerNodeRegistrationHandlerStateMachine) throws IOException {
        if(networkCommunicator != null) {
            networkCommunicator.close();
        }
        networkCommunicator = NetRuntime.buildNetworkCommunicator().startAtFixedPort(node.getPort());
        connectivityState.gotoState(CONNECTIVITY_STATE_CONNECTED, "Open connection at opened port " + node.getPort() + " from hole punching");
    }

    private void handleError(Throwable throwable, Integer from, Integer to, NodeRegistrationHandler nodeRegistrationHandler, StateMachine<Integer, NodeRegistrationHandler> stateMachine) {
    }
    private void handleInitializing(NodeRegistrationHandler nodeRegistrationHandler, StateMachine<Integer, NodeRegistrationHandler> stateMachine) throws IOException {
        errorPacket = null;
        node = new Node();
        node.setRpcPort(rpcPort);
        node.setRpcIp(rpcIp);
        node.setPort(publicUdpPort);
        if(networkCommunicator == null) {
            if(publicUdpPort == -1) {
                networkCommunicator = NetRuntime.buildNetworkCommunicator().startAtAnyPort();
                node.setNeedHolePunching(true);
            } else {
                networkCommunicator = NetRuntime.buildNetworkCommunicator().startAtFixedPort(publicUdpPort);
                node.setNeedHolePunching(false);
            }
        }
        node.setServerName(networkCommunicator.getServerName());
        node.setServerNameCRC(networkCommunicator.getServerNameCRC());

        List<String> networkIFS = ChatUtils.getIps();
        if(networkIFS == null || networkIFS.isEmpty()) {
            throw new IllegalStateException("No available network interfaces to start node.");
        }
        LinkedHashSet<String> ipSet = new LinkedHashSet<>(networkIFS);
        
        node.setIps(new ArrayList<>(ipSet));
        NodeRegistrationRequest nodeRegistrationRequest = new NodeRegistrationRequest();
        nodeRegistrationRequest.setNode(node);
        discoveryHostManager.sendRequestTransport(networkCommunicator, ContentPacket.buildWithContent(nodeRegistrationRequest), NodeRegistrationResponse.class
                , (response, failedResponse, serverIdCRC, address) -> {
                    if(response != null && response.getContent() != null) {
                        NodeRegistrationResponse nodeRegistrationResponse = response.getContent();
                        node.setPort(nodeRegistrationResponse.getPublicPort());
                        List<String> ips = node.getIps();
                        if(!ips.contains(nodeRegistrationResponse.getPublicIp())) {
                            ips.add(nodeRegistrationResponse.getPublicIp());
                        }
                        if(serverIdCRC != null)
                            aquamanPingTimeMap.putIfAbsent(serverIdCRC, System.currentTimeMillis());
                        if(nodeRegistrationResponse.isNeedHolePunching()) {
                            connectivityState.gotoState(CONNECTIVITY_STATE_HOLE_PUNCHING, "Need hole punching to port " + nodeRegistrationResponse.getPublicPort() + " ip " + nodeRegistrationResponse.getPublicIp());
                        } else {
                            connectivityState.gotoState(CONNECTIVITY_STATE_CONNECTED, "Connected to port " + nodeRegistrationResponse.getPublicPort() + " ip " + nodeRegistrationResponse.getPublicIp());
                        }
                    } else if(failedResponse != null) {
                        connectivityState.gotoState(CONNECTIVITY_STATE_DISCONNECTED, "Disconnected as timeout receiving NodeRegistrationResponse " + failedResponse);
                    }
                }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
//        networkCommunicator.sendRequestTransport(ContentPacket.buildWithContent(nodeRegistrationRequest), addresses.get(0)
//                , NodeRegistrationResponse.class
//                , (response, failedResponse, serverIdCRC) -> {
//            if(response != null && response.getContent() != null) {
//                NodeRegistrationResponse nodeRegistrationResponse = response.getContent();
//                node.setPort(nodeRegistrationResponse.getPublicPort());
//                List<String> ips = node.getIps();
//                if(!ips.contains(nodeRegistrationResponse.getPublicIp())) {
//                    ips.add(nodeRegistrationResponse.getPublicIp());
//                }
//                if(serverIdCRC != null)
//                    aquamanPingTimeMap.putIfAbsent(serverIdCRC, System.currentTimeMillis());
//                if(nodeRegistrationResponse.isNeedHolePunching()) {
//                    connectivityState.gotoState(CONNECTIVITY_STATE_HOLE_PUNCHING, "Need hole punching to port " + nodeRegistrationResponse.getPublicPort() + " ip " + nodeRegistrationResponse.getPublicIp());
//                } else {
//                    connectivityState.gotoState(CONNECTIVITY_STATE_CONNECTED, "Connected to port " + nodeRegistrationResponse.getPublicPort() + " ip " + nodeRegistrationResponse.getPublicIp());
//                }
//            } else if(failedResponse != null) {
//                connectivityState.gotoState(CONNECTIVITY_STATE_DISCONNECTED, "Disconnected as timeout receiving NodeRegistrationResponse " + failedResponse);
//            }
//        }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
    }
    private void handleConnected(NodeRegistrationHandler nodeRegistrationHandler, StateMachine<Integer, NodeRegistrationHandler> stateMachine) {
        if(pingTask != null) {
            pingTask.cancel(true);
            pingTask = null;
        }
        serviceNodesManager.setNetworkCommunicator(networkCommunicator);
        pingTask = internalTools.getScheduledExecutorService().scheduleAtFixedRate(() -> {
            if(connectivityState.getCurrentState() == CONNECTIVITY_STATE_CONNECTED) {
                try {
                    networkCommunicator.ping(discoveryHostManager.getUsingAddress());
                } catch (Throwable e) {
                    e.printStackTrace();
                    LoggerEx.error(TAG, "Ping aquamanAddress " + discoveryHostManager.getUsingAddress() + " failed, " + e.getMessage());
                }
            } else {
                LoggerEx.warn(TAG, "Ping will be canceled as current state is not connected, expected " + CONNECTIVITY_STATE_CONNECTED + " actual " + connectivityState.getCurrentState());
                pingTask.cancel(true);
                pingTask = null;
            }
        },0, 3000L, TimeUnit.MILLISECONDS);

        networkCommunicator.addPingListener((serverIdCRC, address) -> {
            Long pingTime = aquamanPingTimeMap.get(serverIdCRC);
            long time = System.currentTimeMillis();
            if(pingTime == null || time > pingTime) {
                aquamanPingTimeMap.put(serverIdCRC, time);
            }
        });
        networkCommunicator.addContentPacketListener(LatencyCheckRequest.class, (contentPacket, serverIdCRC, address) -> {
            LatencyCheckRequest request = contentPacket.getContent();
            return request.generateResponse();
        });

        networkCommunicator.addPacketListener(NetworkCommunicator.PACKET_TYPE_ERROR, (packet, serverIdCRC, address) -> {
            errorPacket = (ErrorPacket) packet;
            switch (errorPacket.getCode()) {
                case DiscoveryErrorCodes.ERROR_UNKNOWN_NODE:
                    connectivityState.gotoState(CONNECTIVITY_STATE_DISCONNECTED, "Aquaman report ErrorPacket to current node, Unknown node. Need go to disconnect and reconnect...");
                    break;
                case DiscoveryErrorCodes.ERROR_NODE_TERMINATION:
                    connectivityState.gotoState(CONNECTIVITY_STATE_TERMINATED, "Aquaman report ErrorPacket to current node, Node termination. Will not reconnect...");
                    break;
                default:
                    LoggerEx.error(TAG, "Unknown error code " + errorPacket.getCode() + " message " + errorPacket.getMessage());
                    break;
            }
        });
        if(startNodeFuture != null) {
            startNodeFuture.complete(this);
            startNodeFuture = null;
        }
        retryTimes = 0;
        maxRetries = MAX_RETRIES_AFTER_CONNECTED;

        Collection<Service> services = serviceMap.values();
        for(Service service : services) {
            registerService(service).thenAccept(serviceRuntime -> {
                LoggerEx.info(TAG, "Register service " + service.generateServiceKey() + " after node connected");
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                LoggerEx.error(TAG, "Register service " + service.generateServiceKey() + " failed after node connected, " + throwable.getMessage());
                return null;
            });
        }
    }
    private void handleLeaveConnected(NodeRegistrationHandler nodeRegistrationHandler, StateMachine<Integer, NodeRegistrationHandler> stateMachine) {
        aquamanPingTimeMap.clear();
        networkCommunicator.close();
        networkCommunicator = null;
        if(pingTask != null) {
            pingTask.cancel(true);
            pingTask = null;
        }
        serviceNodesManager.setNetworkCommunicator(null);
    }
    private void handleDisconnected(NodeRegistrationHandler nodeRegistrationHandler, StateMachine<Integer, NodeRegistrationHandler> stateMachine) {
        if(retryTimes >= maxRetries) {
            connectivityState.gotoState(CONNECTIVITY_STATE_TERMINATED, "Will terminate, tried too many, " + retryTimes + " max " + maxRetries);
            return;
        }
        retryTimes++;
        long waitTime = (errorPacket != null && errorPacket.getWaitSeconds() != null && errorPacket.getWaitSeconds() > 0) ? errorPacket.getWaitSeconds() * 1000 : 5000L;
        LoggerEx.info(TAG, "Will wait " + waitTime / 1000 + " seconds to reconnect. retryTimes " + retryTimes + " maxRetries " + maxRetries);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connectivityState.gotoState(CONNECTIVITY_STATE_INITIALIZING, "Start reconnecting after " + waitTime / 1000 + " seconds sleep");
    }

    @Override
    public synchronized CompletableFuture<NodeRegistrationHandler> startNode(String discoveryHosts, String rpcIp, int rpcPort) {
        if(connectivityState == null)
            throw new IllegalStateException("NodeRegistrationHandler need init first before start node.");
        if(connectivityState.getCurrentState() != CONNECTIVITY_STATE_NONE)
            throw new IllegalStateException("NodeRegistrationHandler state is illegal, " + connectivityState.getCurrentState() + " expect " + CONNECTIVITY_STATE_NONE + ", can not start node");
        if(startNodeFuture != null)
            throw new IllegalStateException("NodeRegistrationHandler is starting, state " + connectivityState.getCurrentState());

        this.rpcPort = rpcPort;
        this.rpcIp = rpcIp;
        startNodeFuture = new CompletableFuture<>();

        //TODO later need implement retry multiple aquaman domain, like aquaman.starfish.com, aquaman1.starfish.com, aquaman2.starfish.com, aquaman3.starfish.com, ...
        discoveryHostManager = new DiscoveryHostManager(discoveryHosts);
        discoveryHostManager.init();
//        aquamanAddress = new InetSocketAddress(aquamanDomain, port);
        serviceNodesManager = new ServiceNodesManager(discoveryHostManager);
        serviceNodesManager.internalTools = internalTools;
        connectivityState.gotoState(CONNECTIVITY_STATE_INITIALIZING, "startNode at " + discoveryHosts);
        return startNodeFuture;

    }

    @Override
    public synchronized void stopNode() {
        connectivityState.gotoState(CONNECTIVITY_STATE_DISCONNECTED, "stopNode");
        if(startNodeFuture != null) {
            startNodeFuture.completeExceptionally(new IllegalStateException("Stopped"));
            startNodeFuture = null;
        }
    }

    @Override
    public CompletableFuture<ServiceRuntime> registerService(Service service) {
        if(connectivityState.getCurrentState() != CONNECTIVITY_STATE_CONNECTED)
            throw new IllegalStateException("Not connected while register service " + service);

        serviceMap.put(service.generateServiceKey(), service);
        CompletableFuture<ServiceRuntime> future = new CompletableFuture<>();
        ServiceRegistrationRequest serviceRegistrationRequest = new ServiceRegistrationRequest();
        serviceRegistrationRequest.setService(service);
        discoveryHostManager.sendRequestTransport(networkCommunicator, ContentPacket.buildWithContent(serviceRegistrationRequest),
                ServiceRegistrationResponse.class,
                (response, failedResponse, serverIdCRC, address) -> {
            boolean completed = false;
            if(response != null) {
                ServiceRuntime serviceRuntime = new ServiceRuntime(service);
                serviceRuntime.nodeRegistrationHandler = this;
                future.complete(serviceRuntime);
                completed = true;
            } else if(failedResponse != null) {
                FailedResponse theFailedResponse = failedResponse.getContent();
                if(theFailedResponse != null) {
                    future.completeExceptionally(new IOException(theFailedResponse.getMessage() + " code " + theFailedResponse.getCode()));
                    completed = true;
                }
            }
            if(!completed) {
                future.completeExceptionally(new IOException("Unknown error"));
            }
        }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
        return future;
    }

    @Override
    public NodeRegistrationHandler unregisterService(String service) {
        serviceMap.remove(service);
        return this;
    }

    @Override
    public <K extends RequestTransport<R>, R extends ResponseTransport> CompletableFuture<ContentPacket<R>> sendContentPacket(ContentPacket<K> packet, Class<R> responseClass, String serviceKey) {
        return serviceNodesManager.sendContentPacket(packet, responseClass, serviceKey);
    }

    @Override
    public CompletableFuture<ServiceNodeResult> getNodesWithServices(Collection<String> services, Collection<Long> checkNodesAvailability, boolean onlyNodeServerCRC) {
        CompletableFuture<ServiceNodeResult> future = new CompletableFuture<>();
        if(connectivityState.getCurrentState() != CONNECTIVITY_STATE_CONNECTED) {
            future.completeExceptionally(new IllegalStateException("Node not connect to discovery while getNodesWithPublicService services " + services));
            return future;
//            return CompletableFuture.failedFuture(new IllegalStateException("Node not connect to discovery while getNodesWithPublicService services " + services));
        }
//            throw new IllegalStateException("Node not connect to Aquaman");

        FindServiceRequest findServiceRequest = new FindServiceRequest();
//        findServiceRequest.setOwner(owner);
//        findServiceRequest.setProject(project);
        findServiceRequest.setServices(services);
        findServiceRequest.setCheckNodesAvailability(checkNodesAvailability);
        findServiceRequest.setOnlyNodeServerCRC(onlyNodeServerCRC);
//        findServiceRequest.setVersion(version);
        discoveryHostManager.sendRequestTransport(networkCommunicator, ContentPacket.buildWithContent(findServiceRequest), FindServiceResponse.class, (response, failedResponse, serverIdCRC, address) -> {
            boolean completed = false;
            if(response != null) {
                FindServiceResponse findServiceResponse = response.getContent();
                ServiceNodeResult result = findServiceResponse.getServiceNodeResult();
                future.complete(result);
                completed = true;
            } else if(failedResponse != null) {
                FailedResponse theFailedResponse = failedResponse.getContent();
                if(theFailedResponse != null) {
                    future.completeExceptionally(new IOException(theFailedResponse.getMessage() + " code " + theFailedResponse.getCode()));
                    completed = true;
                }
            }
            if(!completed) {
                future.completeExceptionally(new IOException("Unknown error"));
//                completed = true;
            }
        }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
        return future;
    }

    @Override
    public CompletableFuture<Node> getNodeByServerCRCId(Long serverCRCId) {
        if(connectivityState.getCurrentState() != CONNECTIVITY_STATE_CONNECTED)
            throw new IllegalStateException("Node not connect to Aquaman");

        CompletableFuture<Node> future = new CompletableFuture<>();
        GetNodeByServerCRCIdRequest getNodeByServerCRCIdRequest = new GetNodeByServerCRCIdRequest();
        getNodeByServerCRCIdRequest.setServerCRCId(serverCRCId);
        discoveryHostManager.sendRequestTransport(networkCommunicator, ContentPacket.buildWithContent(getNodeByServerCRCIdRequest), GetNodeByServerCRCIdResponse.class, (response, failedResponse, serverIdCRC, address) -> {
            GetNodeByServerCRCIdResponse findServiceResponse = response.getContent();
            boolean completed = false;
            if(findServiceResponse != null) {
                Node node = findServiceResponse.getNode();
                future.complete(node);
                completed = true;
            } else if(failedResponse != null) {
                FailedResponse theFailedResponse = failedResponse.getContent();
                if(theFailedResponse != null) {
                    future.completeExceptionally(new IOException(theFailedResponse.getMessage() + " code " + theFailedResponse.getCode()));
                    completed = true;
                }
            }
            if(!completed) {
                future.completeExceptionally(new IOException("Unknown error"));
//                completed = true;
            }
        }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
        return future;
    }

    @Override
    public NodeRegistrationHandler watchNodeEventsForPublicServices(List<String> services, NodeEventListener nodeEventListener) {
        return this;
    }

    @Override
    public NodeRegistrationHandler unwatchNodeEventsForPublicServices(List<String> services, NodeEventListener nodeEventListener) {
        return this;
    }

    @Override
    public NetworkCommunicator getConnectedNetworkCommunicator() {
        if(connectivityState.getCurrentState() == CONNECTIVITY_STATE_CONNECTED) {
            return networkCommunicator;
        }
        return null;
    }
}
