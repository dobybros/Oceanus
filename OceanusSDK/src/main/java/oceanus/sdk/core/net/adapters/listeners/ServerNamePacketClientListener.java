package oceanus.sdk.core.net.adapters.listeners;//package core.net.adapters.listeners;
//
//import org.apache.commons.lang.RandomStringUtils;
//import core.log.LoggerHelper;
//import core.net.NetRuntime;
//import core.net.NetworkCommunicator;
//import core.net.NetworkCommunicatorFactory;
//import core.net.adapters.data.Packet;
//import core.net.adapters.data.ServerNamePacket;
//import org.apache.commons.lang.exception.ExceptionUtils;
//
//import java.io.IOException;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
//
//public class ServerNamePacketClientListener implements NetworkCommunicator.PacketListener {
//    private NetworkCommunicator networkCommunicator;
//    private ScheduledFuture scheduledFuture;
//    private CompletableFuture<NetworkCommunicator> future;
//    private NetworkCommunicatorFactory networkCommunicatorFactory;
//    private String clientName;
//    public ServerNamePacketClientListener(NetworkCommunicator networkCommunicator, CompletableFuture<NetworkCommunicator> future, NetworkCommunicatorFactory networkCommunicatorFactory) {
//        this.networkCommunicator = networkCommunicator;
//        this.future = future;
//        this.clientName = networkCommunicatorFactory.getServerName() + "#" + RandomStringUtils.randomAlphanumeric(8);
//        this.networkCommunicatorFactory = networkCommunicatorFactory;
//
//        this.networkCommunicatorFactory.registerClientNetworkCommunicator(this.clientName, networkCommunicator);
//        LoggerEx.info(TAG, "Start counting " + NetRuntime.serverNameTimeoutForServerAndClient() / 1000 + " seconds, will destroy if no server name received. " + networkCommunicator);
//        scheduledFuture = NetRuntime.getScheduledExecutorService().schedule(() -> {
//            future.completeExceptionally(new IOException("serverName timeout for client, " + this.clientName));
//            destroy();
//        }, NetRuntime.serverNameTimeoutForServerAndClient(), TimeUnit.MILLISECONDS);
//        try {
//            networkCommunicator.sendPacket(new ServerNamePacket(this.clientName));
//        } catch (IOException e) {
//            e.printStackTrace();
//            LoggerEx.error(TAG, "Client send server name " + this.clientName + " failed, " + ExceptionUtils.getFullStackTrace(e));
//            if(!scheduledFuture.isCancelled())
//                scheduledFuture.cancel(true);
//            future.completeExceptionally(new IOException("Send serverName " + this.clientName + " failed, " + e.getMessage()));
//        }
//    }
//
//    @Override
//    public void packetReceived(Packet packet) {
//        switch (packet.getType()) {
//            case Packet.TYPE_SERVER_NAME:
//                ServerNamePacket serverNamePacket = (ServerNamePacket) packet;
//                String serverName = serverNamePacket.getServerName();
//                LoggerEx.info(TAG, "Counter will be canceled as received ServerNamePacket " + serverName);
//                if(!scheduledFuture.isCancelled())
//                    scheduledFuture.cancel(true);
//                if(!serverName.isBlank()) {
////                    NetworkCommunicator networkCommunicator = networkCommunicatorFactory.addUpdateNetworkCommunicator(serverName, this.networkCommunicator);
//                    future.complete(networkCommunicator);
//                    destroy();
//                } else {
//                    future.completeExceptionally(new IllegalStateException("No serverName received"));
//                }
//                break;
//        }
//    }
//
//    private void destroy() {
//        if(networkCommunicator != null) {
//            boolean bool = networkCommunicator.removePacketListener(Packet.TYPE_SERVER_NAME, this);
//            if(!bool) {
//                LoggerEx.error(TAG, "ServerNamePacketListener was not been removed as expected");
//            }
//        }
//    }
//}
