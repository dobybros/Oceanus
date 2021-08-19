package oceanus.sdk.core.discovery.impl.client;


import oceanus.sdk.core.common.CoreRuntime;
import oceanus.sdk.core.discovery.data.FailedResponse;
import oceanus.sdk.core.discovery.data.discovery.GetNodeByServerCRCIdRequest;
import oceanus.sdk.core.discovery.data.discovery.GetNodeByServerCRCIdResponse;
import oceanus.sdk.core.discovery.data.discovery.LatencyCheckRequest;
import oceanus.sdk.core.discovery.data.discovery.LatencyCheckResponse;
import oceanus.sdk.core.discovery.node.Node;
import oceanus.sdk.core.discovery.node.NodeConnectivity;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.utils.state.StateListener;
import oceanus.sdk.utils.state.StateMachine;
import oceanus.sdk.utils.state.StateOperateRetryHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NodeConnectivityHandler {
    private static final String TAG = NodeConnectivityHandler.class.getSimpleName();
    private final ServiceNodesManager serviceNodesManager;
    private NodeConnectivity nodeConnectivity;
    private final Long serverCRCId;

    public static final int STATE_NONE = 1;
    public static final int STATE_INITIALIZING = 2;
    public static final int STATE_GET_NODE = 4;
    public static final int STATE_GET_NODE_FAILED = 6;
    public static final int STATE_FIND_BEST_IP = 7;
    public static final int STATE_FIND_BEST_IP_FAILED = 8;
    public static final int STATE_CONNECTING = 9;
    public static final int STATE_CONNECTED = 10;
    public static final int STATE_DISCONNECTED = 100;
    public static final int STATE_TERMINATED = 200;
    private StateMachine<Integer, NodeConnectivityHandler> stateMachine;
    private StateOperateRetryHandler<Integer, NodeConnectivityHandler> getNodeHandler, findBestIPHandler;

    public NodeConnectivityHandler(Long serverCRCId, ServiceNodesManager serviceNodesManager) {
        this.serviceNodesManager = serviceNodesManager;
        this.serverCRCId = serverCRCId;
        stateMachine = new StateMachine<>("NodeConnectivityHandler#" + serverCRCId, STATE_NONE, this);
        getNodeHandler = StateOperateRetryHandler.build(stateMachine, serviceNodesManager.internalTools.getScheduledExecutorService()).setMaxRetry(5).setRetryInterval(2000L)
                .setOperateListener(this::handleGetNode)
                .setOperateFailedListener(this::handleGetNodeFailed);
        findBestIPHandler = StateOperateRetryHandler.build(stateMachine, serviceNodesManager.internalTools.getScheduledExecutorService()).setMaxRetry(5).setRetryInterval(2000L)
                .setOperateListener(this::handleFindBestIP).setOperateFailedListener(this::handleFindBestIPFailed);
        stateMachine
                .configState(STATE_NONE, stateMachine.execute().nextStates(STATE_INITIALIZING))
                .configState(STATE_INITIALIZING, stateMachine.execute(this::handleInitializing).nextStates(STATE_GET_NODE))
                .configState(STATE_GET_NODE, stateMachine.execute(getNodeHandler::operate).nextStates(STATE_GET_NODE_FAILED, STATE_FIND_BEST_IP))
                .configState(STATE_GET_NODE_FAILED, stateMachine.execute(getNodeHandler::operateFailed).nextStates(STATE_INITIALIZING, STATE_TERMINATED))
                .configState(STATE_FIND_BEST_IP, stateMachine.execute(findBestIPHandler::operate).nextStates(STATE_FIND_BEST_IP_FAILED, STATE_CONNECTING, STATE_CONNECTED).leaveState(this::handleLeaveFindBestIp))
                .configState(STATE_FIND_BEST_IP_FAILED, stateMachine.execute(findBestIPHandler::operateFailed).nextStates(STATE_FIND_BEST_IP, STATE_TERMINATED))
//                .configState(STATE_CONNECTING, stateMachine.newState(this::handleConnecting).nextStates(STATE_CONNECTED, STATE_DISCONNECTED))
                .configState(STATE_CONNECTED, stateMachine.execute(this::handleConnected).nextStates(STATE_DISCONNECTED))
                .configState(STATE_DISCONNECTED, stateMachine.execute(this::handleDisconnected).nextStates(STATE_CONNECTING, STATE_TERMINATED))
                .configState(STATE_TERMINATED, stateMachine.execute(this::handleTerminated).nextStates(STATE_INITIALIZING));
    }

    private void handleLeaveFindBestIp(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) {
        if(findBestIPTimeoutTask != null) {
            findBestIPTimeoutTask.cancel(true);
            findBestIPTimeoutTask = null;
        }
    }

    private void handleFindBestIPFailed(boolean willRetry, int retryCount, int maxRetry, NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) {
        if(willRetry) {
            stateMachine.gotoState(STATE_FIND_BEST_IP, "Retry find best ip " + serverCRCId + " at " + retryCount + " times");
        } else {
            stateMachine.gotoState(STATE_TERMINATED, "Terminated because retried " + retryCount + " times, max is " + maxRetry);
        }
    }

    private ScheduledFuture<?> findBestIPTimeoutTask;
    private void handleFindBestIP(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) throws IOException {
        if(nodeConnectivity == null) {
            stateMachine.gotoState(STATE_INITIALIZING, "Node connectivity is null while find best IP");
            return;
        }
        Node node = nodeConnectivity.getNode();
        if(node == null || node.getIps() == null || node.getIps().isEmpty()) {
            stateMachine.gotoState(STATE_INITIALIZING, "Node in NodeConnectivity is null or ips is empty while find best IP");
            return;
        }
        if(findBestIPTimeoutTask != null) {
            findBestIPTimeoutTask.cancel(true);
            findBestIPTimeoutTask = null;
        }
        findBestIPTimeoutTask = serviceNodesManager.internalTools.getScheduledExecutorService().schedule(() -> {
             stateMachine.gotoState(STATE_FIND_BEST_IP_FAILED, "Find best ip timeout after " + CoreRuntime.CONTENT_PACKET_TIMEOUT / 1000 + " seconds");
        }, CoreRuntime.CONTENT_PACKET_TIMEOUT, TimeUnit.MILLISECONDS);
        List<String> ips = node.getIps();
//        Map<String, Long> ipLatency = new HashMap<>();
        nodeConnectivity.reset();
        for(final String ip : ips) {
            NetworkCommunicator networkCommunicator = serviceNodesManager.getNetworkCommunicator();
            if(networkCommunicator != null) {
                final long time = System.currentTimeMillis();
                //TODO Maybe need send multiple times for hole punching
                networkCommunicator.sendRequestTransport(ContentPacket.buildWithContent(new LatencyCheckRequest(), false), new InetSocketAddress(ip, node.getPort()), LatencyCheckResponse.class, (response, failedResponse, serverIdCRC, address) -> {
                    if(failedResponse == null) {
                        synchronized (nodeConnectivity) {
                            long takes = (System.currentTimeMillis() - time);
                            if(nodeConnectivity.getIp() == null) {
                                nodeConnectivity.registerIp(ip, takes);
                                stateMachine.gotoState(STATE_CONNECTED, "Found first IP " + ip + " which takes " + takes);
                                LoggerEx.info(TAG, "Found major IP " + ip + " which takes " + takes + " to serverCRCId " + serverCRCId);
                            } else {
                                nodeConnectivity.registerIp(ip, takes);
                                LoggerEx.info(TAG, "Found backup IP " + ip + " which takes " + takes + " to serverCRCId " + serverCRCId);
                            }
                        }
                    }
                }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
            }
        }

    }

    private void handleGetNodeFailed(boolean willRetry, int retryCount, int maxRetry, NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) {
        if(willRetry) {
            stateMachine.gotoState(STATE_GET_NODE, "Retry get node " + serverCRCId + " at " + retryCount + " times");
        } else {
            stateMachine.gotoState(STATE_TERMINATED, "Terminated because retried " + retryCount + " times, max is " + maxRetry);
        }
    }

    private void handleGetNode(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) {
        GetNodeByServerCRCIdRequest getNodeByServerCRCIdRequest = new GetNodeByServerCRCIdRequest();
        getNodeByServerCRCIdRequest.setServerCRCId(serverCRCId);
        NetworkCommunicator networkCommunicator = serviceNodesManager.getNetworkCommunicator();

        if(networkCommunicator != null) {
            serviceNodesManager.discoveryHostManager.sendRequestTransport(networkCommunicator, ContentPacket.buildWithContent(getNodeByServerCRCIdRequest), GetNodeByServerCRCIdResponse.class, (response, failedResponse, fromServerCRCId, address) -> {
                GetNodeByServerCRCIdResponse getNodeByServerCRCIdResponse = response.getContent();
                boolean completed = false;
                String errorMessage = null;
                if(getNodeByServerCRCIdResponse != null) {
                    int count = 0;
                    Node node = getNodeByServerCRCIdResponse.getNode();
                    if(node != null) {
                        nodeConnectivity = new NodeConnectivity(node);
                        stateMachine.gotoState(STATE_FIND_BEST_IP, "Found node " + node + " for serverCRCId " + serverCRCId);
                    } else {
                        stateMachine.gotoState(STATE_GET_NODE_FAILED, "Node is null from response for serverCRCId " + serverCRCId);
                    }
                    return;
                } else if(failedResponse != null) {
                    FailedResponse theFailedResponse = failedResponse.getContent();
                    if(theFailedResponse != null) {
//                        future.completeExceptionally(new IOException(theFailedResponse.getMessage() + " code " + theFailedResponse.getCode()));
                        errorMessage = "FailedResponse " + theFailedResponse.getMessage() + " code " + theFailedResponse.getCode();
                    }
                }
                if(errorMessage == null) {
                    errorMessage = "Unknown error";
                }
                stateMachine.gotoState(STATE_GET_NODE_FAILED, errorMessage);
            }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
        }
    }

    private void handleTerminated(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) {
    }

    private void handleDisconnected(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) {

    }

    private void handleConnected(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) throws Throwable {
        getNodeHandler.initializing(nodeConnectivityHandler, integerNodeConnectivityHandlerStateMachine);
        findBestIPHandler.initializing(nodeConnectivityHandler, integerNodeConnectivityHandlerStateMachine);
    }

//    private void handleConnecting(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) {
//    }

    private void handleInitializing(NodeConnectivityHandler nodeConnectivityHandler, StateMachine<Integer, NodeConnectivityHandler> integerNodeConnectivityHandlerStateMachine) throws Throwable {
        getNodeHandler.initializing(nodeConnectivityHandler, integerNodeConnectivityHandlerStateMachine);
        findBestIPHandler.initializing(nodeConnectivityHandler, integerNodeConnectivityHandlerStateMachine);
        stateMachine.gotoState(STATE_GET_NODE, "Initializing");
    }

    public Integer getCurrentState() {
        return stateMachine.getCurrentState();
    }

    public void start() {
        stateMachine.gotoState(STATE_INITIALIZING, "Start");
    }

    public void stop() {
        stateMachine.gotoState(STATE_TERMINATED, "Stop");
    }

    public <R extends ResponseTransport, K extends RequestTransport<R>> CompletableFuture<ContentPacket<R>> sendContentPacket(ContentPacket<K> packet, Class<R> responseClass) {
        return sendContentPacket(packet, responseClass, null);
    }

    public <R extends ResponseTransport, K extends RequestTransport<R>> CompletableFuture<ContentPacket<R>> sendContentPacket(ContentPacket<K> packet, Class<R> responseClass, String serviceKey) {
        final CompletableFuture<ContentPacket<R>> future = new CompletableFuture<>();
        NetworkCommunicator networkCommunicator = serviceNodesManager.getNetworkCommunicator();
        if(networkCommunicator != null) {
            if(serviceKey != null)
                packet.setServiceKey(ContentPacket.getServiceKeyCRC(serviceKey));
            networkCommunicator.sendRequestTransport(packet, nodeConnectivity.getIp().getSocketAddress(), responseClass, (response, failedResponse, serverIdCRC, address) -> {
                if(response != null) {
                    future.complete(response);
                } else if(failedResponse != null) {
                    FailedResponse actualFailedResponse = failedResponse.getContent();
                    future.completeExceptionally(new IOException(actualFailedResponse.getMessage() + " at code " + actualFailedResponse.getCode()));
                } else {
                    future.completeExceptionally(new IOException("Unknown error"));
                }
            }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
        } else {
            future.completeExceptionally(new IOException("NetworkCommunicator is not ready"));
        }
        return future;
    }

    public NodeConnectivityHandler addStateListener(StateListener<Integer, NodeConnectivityHandler> stateListener) {
        if(stateMachine != null) {
            stateMachine.addStateListener(stateListener);
        }
        return this;
    }

    public void removeStateListener(StateListener<Integer, NodeConnectivityHandler> stateListener) {
        if(stateMachine != null) {
            stateMachine.removeStateListener(stateListener);
        }
    }
}
