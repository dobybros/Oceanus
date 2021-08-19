package oceanus.sdk.core.net.rudpex.communicator;

import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.adapters.data.Packet;
import oceanus.sdk.core.net.rudpex.impl.PacketSendingTransmission;
import oceanus.sdk.core.net.rudpex.impl.PacketTransmissionManager;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.utils.state.StateMachine;
import oceanus.sdk.utils.state.StateOperateRetryHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * All packets from any remote will be received by one port only.
 * Use one port to send to any remote.
 *
 * NetworkCommunicator just for manage the remote ip and port.
 */
public class RUDPEXNetworkCommunicator extends NetworkCommunicator {
    public static final boolean LOG_ENABLED = false;
    private static final String TAG = RUDPEXNetworkCommunicator.class.getSimpleName();
    private DatagramSocket datagramSocket;
    private int port;
    private String id; //6 bytes

    private int totalReconnectTimes = 10, currentReconnectTimes = 0;
    private long reconnectPeriod = 5000L;
    private PacketTransmissionManager packetTransmissionManager;

    public RUDPEXNetworkCommunicator() {
    }

    @Override
    public void init() {
        super.init();
        connectStateMachine = new StateMachine<>("RUDPEXNetworkCommunicator#" + this.hashCode(), CONNECTIVITY_STATE_NONE, this);
        StateOperateRetryHandler retryHandler = StateOperateRetryHandler.build(connectStateMachine, internalTools.getScheduledExecutorService()).setMaxRetry(2).setRetryInterval(1000L)
                .setOperateListener(this::handleRetryConnecting)
                .setOperateFailedListener(this::handleRetryDisconnected);

        connectStateMachine
                .configState(CONNECTIVITY_STATE_NONE, connectStateMachine.execute()
                        .nextStates(CONNECTIVITY_STATE_CONNECTING))
                .configState(CONNECTIVITY_STATE_CONNECTING, connectStateMachine.execute(retryHandler::operate)
                        .nextStates(CONNECTIVITY_STATE_CONNECTED, CONNECTIVITY_STATE_DISCONNECTED))
                .configState(CONNECTIVITY_STATE_CONNECTED, connectStateMachine.execute(this::handleConnected).leaveState(this::handleLeaveConnected)
                        .nextStates(CONNECTIVITY_STATE_CONNECTING, CONNECTIVITY_STATE_DISCONNECTED))
                .configState(CONNECTIVITY_STATE_DISCONNECTED, connectStateMachine.execute(retryHandler::operateFailed)
                        .nextStates(CONNECTIVITY_STATE_CONNECTING, CONNECTIVITY_STATE_TERMINATED))
                .configState(CONNECTIVITY_STATE_TERMINATED, connectStateMachine.execute(this::handleTerminated)
                        .nextStates(CONNECTIVITY_STATE_CONNECTING))
                .errorOccurred(this::handleError);
    }
    private void handleTerminated(NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
    }

    private void handleRetryDisconnected(boolean willRetry, int retryCount, int maxRetry, NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
        if(datagramSocket != null) {
            datagramSocket.close();
            datagramSocket = null;
        }
        if(willRetry) {
            connectStateMachine.gotoState(CONNECTIVITY_STATE_CONNECTING, "Retry connecting at " + retryCount + " times");
        } else {
            connectStateMachine.gotoState(CONNECTIVITY_STATE_TERMINATED, "Terminated because retried " + retryCount + " times, max is " + maxRetry);
        }
    }

    // connecting与retry时走的逻辑
    private void handleRetryConnecting(NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
        if(datagramSocket != null) {
            try {
                datagramSocket.close();
            } catch (Throwable t) {}
        }
        try {
            if(port > 0) {
                datagramSocket = new DatagramSocket(port);
            } else {
                datagramSocket = new DatagramSocket();
//                datagramSocket.getOption()
            }
            connectStateMachine.gotoState(CONNECTIVITY_STATE_CONNECTED, "Connected at" + (port == -1 ? " any" : "") + " port " + (port == -1? datagramSocket.getLocalPort() : port));
        } catch (Throwable e) {
            e.printStackTrace();
            connectStateMachine.gotoState(CONNECTIVITY_STATE_DISCONNECTED, "Create DatagramSocket on port " + port + " failed, " + e.getMessage());
        }
    }

    private void handleLeaveConnected(NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
        if(packetTransmissionManager != null) {
            packetTransmissionManager.close();
        }
        currentReconnectTimes = 0;
    }

//    private void handleReconnecting(NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
//    }

//    private void handleDisconnected(NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
//        if(datagramSocket != null) {
//            datagramSocket.close();
//            datagramSocket = null;
//        }
//        //TODO need retry logic
//    }

    private void handleError(Throwable throwable, Integer integer, Integer integer1, NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
    }

    private void handleConnected(NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> integerNetworkCommunicatorStateMachine) {
        if(datagramSocket == null) {
            connectStateMachine.gotoState(CONNECTIVITY_STATE_CONNECTING, "datagramSocket is null when connected");
            return;
        }

//        java.util.zip.CRC32 x = new java.util.zip.CRC32();
//        x.update(serverName.getBytes());
        packetTransmissionManager = new PacketTransmissionManager(datagramSocket, serverNameCRC, internalTools, this);
        packetTransmissionManager.startReceivingPacket().setReceivedListener(this::rawDataReceived);
    }

    private void rawDataReceived(byte type, long serverIdCRC, byte[] bytes, InetSocketAddress inetAddress) {
        switch (type) {
            case PacketSendingTransmission.TYPE_UNRELIABLE_PING:
                executePingListener(serverIdCRC, inetAddress);
                break;
            default:
                Packet packet = null;
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                try {
                    packet = resurrectPacket(inputStream);
                    if(type == PacketSendingTransmission.TYPE_UNRELIABLE_PACKET) {
                        packet.setNeedReliable(false);
                    }
                    packetReceived(packet, serverIdCRC, inetAddress);
                } catch (Throwable e) {
                    e.printStackTrace();
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "rawDataReceived received failed, " + e.getMessage() + " for data length " + bytes.length + " address " + inetAddress + " port " + port + " serverIdCRC " + serverIdCRC);
                } finally {
                    try {
                        inputStream.close();
                    } catch (Throwable ignored) { }
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "rawDataReceived received Packet " + packet + " type " + type + " serverIdCRC " + serverIdCRC + " address " + inetAddress + " port " + port);
                }
                break;
        }

    }

//    private void handleConnecting(NetworkCommunicator networkCommunicator, StateMachine<Integer, NetworkCommunicator> stateMachine) {
//        if(datagramSocket != null) {
//            try {
//                datagramSocket.close();
//            } catch (Throwable t) {}
//        }
//        try {
//            if(port > 0) {
//                datagramSocket = new DatagramSocket(port);
//            } else {
//                datagramSocket = new DatagramSocket();
//            }
//            connectivityState.gotoState(CONNECTIVITY_STATE_CONNECTED, "Connected at" + (port == -1 ? " any" : "") + " port " + (port == -1? datagramSocket.getLocalPort() : port));
//        } catch (Throwable e) {
//            e.printStackTrace();
//            connectivityState.gotoState(CONNECTIVITY_STATE_DISCONNECTED, "Create DatagramSocket on port " + port + " failed, " + e.getMessage());
//        }
//    }

    @Override
    public NetworkCommunicator startAtAnyPort() {
        port = -1;
        connectStateMachine.gotoState(CONNECTIVITY_STATE_CONNECTING, "openAtAnyPort");
        return this;
    }

    @Override
    public NetworkCommunicator startAtFixedPort(int port) {
        this.port = port;
        connectStateMachine.gotoState(CONNECTIVITY_STATE_CONNECTING, "openAtFixedPort " + port);
        return this;
    }

    @Override
    protected void stop() {
        connectStateMachine.gotoState(CONNECTIVITY_STATE_DISCONNECTED, "Disconnected at " + (port == -1 ? " any" : "") + " port " + (port == -1? datagramSocket.getLocalPort() : port));
    }

    @Override
    public CompletableFuture<Void> sendPacket(Packet packet, SocketAddress address) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            super.persistentPacket(packet, os);
        } catch (Throwable e) {
            e.printStackTrace();
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
//            return CompletableFuture.failedFuture(e);
        }
        if(packet.isNeedReliable()) {
            return packetTransmissionManager.sendPacket(new ByteArrayInputStream(os.toByteArray()), address);
        } else {
            return packetTransmissionManager.sendUnreliablePacket(os.toByteArray(), address);
        }
    }

    @Override
    public void ping(SocketAddress address) throws IOException {
        packetTransmissionManager.sendUnreliablePing(address);
    }
}
