package oceanus.sdk.core.discovery.impl.client;

import oceanus.sdk.core.common.CoreRuntime;
import oceanus.sdk.core.discovery.data.FailedResponse;
import oceanus.sdk.core.discovery.data.discovery.FindServiceRequest;
import oceanus.sdk.core.discovery.data.discovery.FindServiceResponse;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.adapters.data.ContentPacket;
import oceanus.sdk.core.net.data.RequestTransport;
import oceanus.sdk.core.net.data.ResponseTransport;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.utils.SingleThreadQueueEx;
import oceanus.sdk.utils.state.StateListener;
import oceanus.sdk.utils.state.StateMachine;
import oceanus.sdk.utils.state.StateOperateRetryHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ServiceNodesHandler {
    private static final String TAG = ServiceNodesHandler.class.getSimpleName();
    private final String serviceKey;
    private final SingleThreadQueueEx<ContentPacketContainer<? extends ResponseTransport, ? extends RequestTransport<?>>> sendingQueue;

    private static final int STATE_NONE = 1;
    private static final int STATE_INITIALIZING = 2;
    private static final int STATE_GET_SERVICE_NODES = 3;
    private static final int STATE_GET_SERVICE_NODES_FAILED = 6;
    private static final int STATE_READY = 9;
    private static final int STATE_TERMINATED = 100;
    private final StateMachine<Integer, ServiceNodesHandler> stateMachine;
    private final ServiceNodesManager serviceNodesManager;
    private final int PREPARE_NODE_CONNECTIVITY_HANDLER_COUNT = 100;
//    private int retryCount = 0;
//    private final int MAX_RETRY_COUNT = 5;
//    private ScheduleTask retryTask;
    private StateOperateRetryHandler<Integer, ServiceNodesHandler> getServiceNodesHandler;
    private List<Long> serviceNodes;
    List<NodeConnectivityHandler> connectedConnectivityHandlers = new CopyOnWriteArrayList<>();

    public ServiceNodesHandler(ServiceNodesManager serviceNodesManager, String serviceKey) {
        this.serviceKey = serviceKey;
        this.serviceNodesManager = serviceNodesManager;
        sendingQueue = new SingleThreadQueueEx<>(this.serviceNodesManager.getSendingThreadPool());
        sendingQueue.setHandler(new SingleThreadQueueEx.Handler<ContentPacketContainer<? extends ResponseTransport, ? extends RequestTransport<?>>>() {
            @Override
            public void execute(ContentPacketContainer<? extends ResponseTransport, ? extends RequestTransport<?>> contentPacketContainer) throws Throwable {
                contentPacketContainer.done();
                ServiceInvocation<? extends ResponseTransport, ? extends RequestTransport<?>> serviceInvocation = new ServiceInvocation(serviceNodesManager, ServiceNodesHandler.this, contentPacketContainer.packet, contentPacketContainer.responseClass, contentPacketContainer.future);
                serviceInvocation.sendContentPacket(contentPacketContainer.serviceKey);
            }

            @Override
            public void error(ContentPacketContainer<? extends ResponseTransport, ? extends RequestTransport<?>> contentPacketContainer, Throwable e) {
                if(contentPacketContainer.future != null) {
                    contentPacketContainer.future.completeExceptionally(e);
                }
            }
        });
        stateMachine = new StateMachine<>("ServiceNodesHandler#" + serviceKey, STATE_NONE, this);
        getServiceNodesHandler = StateOperateRetryHandler.build(stateMachine, serviceNodesManager.internalTools.getScheduledExecutorService()).setMaxRetry(5).setRetryInterval(2000L)
                .setOperateListener(this::handleGetServicesNodes)
                .setOperateFailedListener(this::handleGetServicesNodesFailed);
        stateMachine.configState(STATE_NONE, stateMachine.execute().nextStates(STATE_INITIALIZING, STATE_TERMINATED))
                .configState(STATE_INITIALIZING, stateMachine.execute(this::handleInitializing).nextStates(STATE_GET_SERVICE_NODES, STATE_TERMINATED))
                .configState(STATE_GET_SERVICE_NODES, stateMachine.execute(getServiceNodesHandler::operate).nextStates(STATE_GET_SERVICE_NODES_FAILED, STATE_READY))
                .configState(STATE_GET_SERVICE_NODES_FAILED, stateMachine.execute(getServiceNodesHandler::operateFailed).nextStates(STATE_GET_SERVICE_NODES, STATE_TERMINATED))
                .configState(STATE_READY, stateMachine.execute(this::handleReady).nextStates(STATE_TERMINATED, STATE_INITIALIZING))
                .configState(STATE_TERMINATED, stateMachine.execute(this::handleTerminated).nextStates(STATE_INITIALIZING));
    }

    private void handleTerminated(ServiceNodesHandler serviceNodesHandler, StateMachine<Integer, ServiceNodesHandler> integerServiceNodesHandlerStateMachine) {
    }

    private void handleReady(ServiceNodesHandler serviceNodesHandler, StateMachine<Integer, ServiceNodesHandler> integerServiceNodesHandlerStateMachine) {
    }

    public ServiceNodesHandler addStateListener(StateListener<Integer, ServiceNodesHandler> stateListener) {
        stateMachine.addStateListener(stateListener);
        return this;
    }

    public void removeStateListener(StateListener<Integer, ServiceNodesHandler> stateListener) {
        stateMachine.removeStateListener(stateListener);
    }

    private void handleGetServicesNodesFailed(boolean willRetry, int retryCount, int maxRetry, ServiceNodesHandler serviceNodesHandler, StateMachine<Integer, ServiceNodesHandler> integerServiceNodesHandlerStateMachine) {
        if(willRetry) {
            stateMachine.gotoState(STATE_GET_SERVICE_NODES, "Retry get service " + serviceKey + " nodes at " + retryCount + " times");
        } else {
            stateMachine.gotoState(STATE_TERMINATED, "Terminated because retried " + retryCount + " times, max is " + maxRetry);
        }
    }

    private void handleInitializing(ServiceNodesHandler serviceNodesHandler, StateMachine<Integer, ServiceNodesHandler> integerServiceNodesHandlerStateMachine) throws Throwable {
        getServiceNodesHandler.initializing(serviceNodesHandler, integerServiceNodesHandlerStateMachine);
        stateMachine.gotoState(STATE_GET_SERVICE_NODES, "initializing");
    }

    private void handleGetServicesNodes(ServiceNodesHandler serviceNodesHandler, StateMachine<Integer, ServiceNodesHandler> integerServiceConnectivityHandlerHelperStateMachine) throws IOException {
//        if(connectivityState.getCurrentState() != CONNECTIVITY_STATE_CONNECTED)
//            throw new IllegalStateException("Node not connect to Aquaman");

        FindServiceRequest findServiceRequest = new FindServiceRequest();
//        findServiceRequest.setServiceKey(serviceKey);
        findServiceRequest.setServices(Arrays.asList(serviceKey));
//        findServiceRequest.setCheckNodesAvailability(checkNodesAvailability);
        findServiceRequest.setOnlyNodeServerCRC(true);
        NetworkCommunicator networkCommunicator = serviceNodesManager.getNetworkCommunicator();

        if(networkCommunicator != null) {
            serviceNodesManager.discoveryHostManager.sendRequestTransport(networkCommunicator, ContentPacket.buildWithContent(findServiceRequest), FindServiceResponse.class, (response, failedResponse, serverIdCRC, address) -> {
                FindServiceResponse findServiceResponse = null;
                if(response != null) {
                    findServiceResponse = response.getContent();
                }
                String errorMessage = null;
                if(findServiceResponse != null) {
                    int count = 0;
                    serviceNodes = findServiceResponse.getServiceNodeResult().getServiceNodeCRCIds().get(serviceKey);
                    if(serviceNodes != null) {
                        for(Long node : serviceNodes) {
                            if(count > PREPARE_NODE_CONNECTIVITY_HANDLER_COUNT) {
                                break;
                            }
                            NodeConnectivityHandler connectivityHandler = serviceNodesManager.serverCRCIdNodeMap.get(node);
                            if(connectivityHandler != null) {
                                Integer state = connectivityHandler.getCurrentState();
                                if(state != null) {
                                    switch (state) {
                                        case NodeConnectivityHandler.STATE_CONNECTED:
                                            if(!connectedConnectivityHandlers.contains(connectivityHandler)) {
                                                connectedConnectivityHandlers.add(connectivityHandler);
                                            }
                                        case NodeConnectivityHandler.STATE_CONNECTING:
                                            count++;
                                            break;
                                        case NodeConnectivityHandler.STATE_DISCONNECTED:
                                        case NodeConnectivityHandler.STATE_TERMINATED:
                                            connectedConnectivityHandlers.remove(connectivityHandler);
                                            break;
                                    }
                                }
                            } else {
                                connectivityHandler = new NodeConnectivityHandler(node, serviceNodesManager);
                                NodeConnectivityHandler oldHandler = serviceNodesManager.serverCRCIdNodeMap.putIfAbsent(node, connectivityHandler);
                                if(oldHandler == null) {
                                    connectivityHandler.start();
                                }
                                count++;
                                connectivityHandler.addStateListener(this::watchNodeConnectivityState);
                            }
                        }
                    }
                    stateMachine.gotoState(STATE_READY, "Found nodes " + (serviceNodes != null ? Arrays.toString(serviceNodes.toArray()) : "[]") + " for serviceKey " + serviceKey);
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
                stateMachine.gotoState(STATE_GET_SERVICE_NODES_FAILED, errorMessage);
            }, CoreRuntime.CONTENT_PACKET_TIMEOUT);
        }
    }

    private void watchNodeConnectivityState(Integer from, Integer to, NodeConnectivityHandler nodeConnectivityHandler) {
        if(to != null) {
            switch (to) {
                case NodeConnectivityHandler.STATE_CONNECTED:
                    if(!connectedConnectivityHandlers.contains(nodeConnectivityHandler)) {
                        connectedConnectivityHandlers.add(nodeConnectivityHandler);
                    }
                    synchronized (this) {
                        if(!sendingQueue.getQueue().isEmpty()) {
                            sendingQueue.start();
                        }
                    }
                    break;
                case NodeConnectivityHandler.STATE_DISCONNECTED:
                case NodeConnectivityHandler.STATE_TERMINATED:
                    if(connectedConnectivityHandlers.contains(nodeConnectivityHandler)) {
                        connectedConnectivityHandlers.remove(nodeConnectivityHandler);
                    }
                    break;
            }
        }
    }

    public ServiceNodesHandler start() {
        stateMachine.gotoState(STATE_INITIALIZING, "start");
        return this;
    }

    public void stop() {
        stateMachine.gotoState(STATE_TERMINATED, "stop");
    }

    public class ContentPacketContainer<R extends ResponseTransport, K extends RequestTransport<?>> {
        private final ContentPacket<K> packet;
        private final Class<R> responseClass;
        private final String serviceKey;
        private final CompletableFuture<ContentPacket<R>> future;
        private ScheduledFuture<?> timeoutTask;

        private ContentPacketContainer(ContentPacket<K> packet, Class<R> responseClass, String serviceKey, CompletableFuture<ContentPacket<R>> future) {
            this.packet = packet;
            this.responseClass = responseClass;
            this.serviceKey = serviceKey;
            this.future = future;
        }

        public ContentPacketContainer<R, K> start() {
            timeoutTask = serviceNodesManager.internalTools.getScheduledExecutorService().schedule(() -> {
                boolean removed = sendingQueue.getQueue().remove(ContentPacketContainer.this);
                if(removed) {
                    this.future.completeExceptionally(new IOException("Timeout"));
                } else {
                    LoggerEx.warn(TAG, "ContentPacketContainer didn't be removed when timeout, " + ContentPacketContainer.this);
                }
            }, CoreRuntime.SEND_PACKET_TIMEOUT, TimeUnit.MILLISECONDS);
            return this;
        }

        public void done() {
            if(timeoutTask != null) {
                timeoutTask.cancel(true);
                timeoutTask = null;
            }
        }
    }

    public <R extends ResponseTransport, K extends RequestTransport<R>> CompletableFuture<ContentPacket<R>> sendContentPacket(ContentPacket<K> packet, Class<R> responseClass, String serviceKey) {
        CompletableFuture<ContentPacket<R>> completableFuture = new CompletableFuture<>();
        synchronized (this) {
            if(connectedConnectivityHandlers.isEmpty()) {
                sendingQueue.offer(new ContentPacketContainer<>(packet, responseClass, serviceKey, completableFuture).start());
            } else {
                sendingQueue.offerAndStart(new ContentPacketContainer<>(packet, responseClass, serviceKey, completableFuture).start());
            }
        }
        return completableFuture;
    }
}
