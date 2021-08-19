package oceanus.sdk.core.net.adapters.listeners;//package core.net.adapters.listeners;
//
//import org.apache.commons.lang.exception.ExceptionUtils;
//import core.log.LoggerHelper;
//import core.net.NetRuntime;
//import core.net.NetworkCommunicator;
//import core.net.NetworkCommunicatorFactory;
//import core.net.adapters.data.Packet;
//import core.net.adapters.data.ServerNamePacket;
//
//import java.io.IOException;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
//
//public class ServerNamePacketServerListener implements NetworkCommunicator.PacketListener {
//    private NetworkCommunicatorFactory networkCommunicatorFactory;
//    private ScheduledFuture scheduledFuture;
//    private NetworkCommunicator networkCommunicator;
//    private NetworkCommunicatorFactory.NetworkCommunicatorCreatedListener createdListener;
//    public ServerNamePacketServerListener(NetworkCommunicator networkCommunicator, NetworkCommunicatorFactory networkCommunicatorFactory, NetworkCommunicatorFactory.NetworkCommunicatorCreatedListener createdListener) {
//        this.networkCommunicatorFactory = networkCommunicatorFactory;
//        this.networkCommunicator = networkCommunicator;
//        this.createdListener = createdListener;
//        scheduledFuture = NetRuntime.getScheduledExecutorService().schedule(() -> {
//            networkCommunicatorFactory.removeNetworkCommunicator(networkCommunicator.getServerName(), networkCommunicator);
//        }, NetRuntime.serverNameTimeoutForServerAndClient(), TimeUnit.MILLISECONDS);
//    }
//
//    @Override
//    public void packetReceived(Packet packet) {
//        switch (packet.getType()) {
//            case Packet.TYPE_SERVER_NAME:
//                ServerNamePacket serverNamePacket = (ServerNamePacket) packet;
//                String serverName = serverNamePacket.getServerName();
//                stopTimer();
//                if(!serverName.isBlank()) {
//                    networkCommunicator = networkCommunicatorFactory.addUpdateNetworkCommunicator(serverName, networkCommunicator);
//                    if(createdListener != null) {
//                        try {
//                            createdListener.created(networkCommunicator);
//                        } catch (Throwable throwable) {
//                            LoggerEx.error(TAG, "networkCommunicator " + networkCommunicator + " created failed, " + ExceptionUtils.getFullStackTrace(throwable));
//                        }
//                    }
//                    try {
//                        networkCommunicator.sendPacket(new ServerNamePacket(networkCommunicatorFactory.getServerName()));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        LoggerEx.error(TAG, "Send back server name failed, " + e.getMessage());
//                    }
//                }
//
//                break;
//        }
//    }
//
//    private void stopTimer() {
//        if(!scheduledFuture.isCancelled())
//            scheduledFuture.cancel(true);
//    }
//}
