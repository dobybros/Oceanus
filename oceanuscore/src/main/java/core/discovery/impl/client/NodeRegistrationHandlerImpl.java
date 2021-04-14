package core.discovery.impl.client;

import core.common.CoreRuntime;
import core.discovery.NodeRegistrationHandler;
import core.discovery.data.FailedResponse;
import core.discovery.data.discovery.*;
import core.discovery.errors.DiscoveryErrorCodes;
import core.discovery.node.Node;
import core.discovery.node.Service;
import core.log.LoggerHelper;
import core.net.NetRuntime;
import core.net.NetworkCommunicator;
import core.net.adapters.data.ContentPacket;
import core.net.adapters.data.ErrorPacket;
import core.net.data.RequestTransport;
import core.net.data.ResponseTransport;
import oshi.hardware.NetworkIF;
import script.utils.state.StateMachine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NodeRegistrationHandlerImpl extends NodeRegistrationHandler {
//    private NetworkCommunicatorFactory networkCommunicatorFactory = NetRuntime.getNetworkCommunicatorFactory();

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
    private final int MAX_RETRIES_WHEN_START = 1, MAX_RETRIES_AFTER_CONNECTED = Integer.MAX_VALUE;

    private int maxRetries = MAX_RETRIES_WHEN_START;
    private int retryTimes = 0;

    private ServiceNodesManager serviceNodesManager;
    private int rpcPort;

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

        List<NetworkIF> networkIFS = CoreRuntime.getNetworkInterfaces();
        if(networkIFS == null || networkIFS.isEmpty()) {
            throw new IllegalStateException("No available network interfaces to start node.");
        }
        LinkedHashSet<String> ipSet = new LinkedHashSet<>();
        for(NetworkIF networkIF : networkIFS) {
            LoggerHelper.logger.info("Find network interface " + networkIF);
            String[] ipv4s = networkIF.getIPv4addr();
            if(ipv4s != null && ipv4s.length > 0) {
                for(String ipv4 : ipv4s) {
                    ipSet.add(ipv4);
                }
            }
            String[] ipv6s = networkIF.getIPv6addr();
            if(ipv6s != null && ipv6s.length > 0) {
                for(String ipv6 : ipv6s) {
                    ipSet.add(ipv6);
                }
            }
        }
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
                    LoggerHelper.logger.error("Ping aquamanAddress " + discoveryHostManager.getUsingAddress() + " failed, " + e.getMessage());
                }
            } else {
                LoggerHelper.logger.warn("Ping will be ignored as current state is not connected, expected " + CONNECTIVITY_STATE_CONNECTED + " actual " + connectivityState.getCurrentState());
            }
        },0, 3000L, TimeUnit.MILLISECONDS);

        networkCommunicator.addPingListener((serverIdCRC, address) -> {
            Long pingTime = aquamanPingTimeMap.get(serverIdCRC);
            long time = System.currentTimeMillis();
            if(pingTime == null || time > pingTime) {
                aquamanPingTimeMap.put(serverIdCRC, pingTime);
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
                    LoggerHelper.logger.error("Unknown error code " + errorPacket.getCode() + " message " + errorPacket.getMessage());
                    break;
            }
        });
        if(startNodeFuture != null) {
            startNodeFuture.complete(this);
            startNodeFuture = null;
        }
        retryTimes = 0;
        maxRetries = MAX_RETRIES_AFTER_CONNECTED;
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
        LoggerHelper.logger.info("Will wait " + waitTime / 1000 + " to reconnect. retryTimes " + retryTimes + " maxRetries " + maxRetries);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connectivityState.gotoState(CONNECTIVITY_STATE_INITIALIZING, "Start reconnecting after " + waitTime / 1000 + " seconds sleep");
    }

    @Override
    public synchronized CompletableFuture<NodeRegistrationHandler> startNode(String discoveryHosts, int rpcPort) {
        if(connectivityState == null)
            throw new IllegalStateException("NodeRegistrationHandler need init first before start node.");
        if(connectivityState.getCurrentState() != CONNECTIVITY_STATE_NONE)
            throw new IllegalStateException("NodeRegistrationHandler state is illegal, " + connectivityState.getCurrentState() + " expect " + CONNECTIVITY_STATE_NONE + ", can not start node");
        if(startNodeFuture != null)
            throw new IllegalStateException("NodeRegistrationHandler is starting, state " + connectivityState.getCurrentState());

        this.rpcPort = rpcPort;
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
        return this;
    }

    @Override
    public <K extends RequestTransport<R>, R extends ResponseTransport> CompletableFuture<ContentPacket<R>> sendContentPacket(ContentPacket<K> packet, Class<R> responseClass, String serviceKey) {
        return serviceNodesManager.sendContentPacket(packet, responseClass, serviceKey);
    }

    private CompletableFuture<List<Long>> getNodesWithPublicService(String owner, String project, String service, Integer version) throws IOException {
        if(connectivityState.getCurrentState() != CONNECTIVITY_STATE_CONNECTED)
            throw new IllegalStateException("Node not connect to Aquaman");

        CompletableFuture<List<Long>> future = new CompletableFuture<>();
        FindServiceRequest findServiceRequest = new FindServiceRequest();
        findServiceRequest.setOwner(owner);
        findServiceRequest.setProject(project);
        findServiceRequest.setService(service);
        findServiceRequest.setVersion(version);
        discoveryHostManager.sendRequestTransport(networkCommunicator, ContentPacket.buildWithContent(findServiceRequest), FindServiceResponse.class, (response, failedResponse, serverIdCRC, address) -> {
            FindServiceResponse findServiceResponse = response.getContent();
            boolean completed = false;
            if(findServiceResponse != null) {
                List<Long> nodes = findServiceResponse.getNodeServers();
                future.complete(nodes);
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

    private CompletableFuture<Node> getNodeByServerCRCId(Long serverCRCId) throws IOException {
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
