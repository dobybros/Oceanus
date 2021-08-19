package oceanus.sdk.core.net.rudpex.impl;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import oceanus.sdk.core.common.InternalTools;
import oceanus.sdk.core.net.NetRuntime;
import oceanus.sdk.core.net.NetworkCommunicator;
import oceanus.sdk.core.net.rudpex.communicator.RUDPEXNetworkCommunicator;
import oceanus.sdk.core.utils.ByteUtils;
import oceanus.sdk.logger.LoggerEx;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Random;
import java.util.concurrent.*;

public class PacketTransmissionManager {
    private static final String TAG = PacketTransmissionManager.class.getSimpleName();
    DatagramSocket datagramSocket;
    private ReceivePacketThread receivePacketThread;
    private Random randomForId = new Random();
    //TODO use String for serverId is a little bit more reliable than Long.
    long serverIdCRC;
    // 上层监听
    private ReceivedListener receivedListener;

    // 正在发送的包
    ConcurrentHashMap<Integer, PacketSendingTransmission> idReliablePacketTransmissionMap = new ConcurrentHashMap<>();
    // 正在接收的server的包
    private ConcurrentHashMap<Long, ConcurrentHashMap<Integer, PacketReceivingTransmission>> serverIdRandIdReceivingTransmissionMap = new ConcurrentHashMap<>();
    // 通知上层处理接收的包
    private ExecutorService coreThreadPool;
    // 1.清除正在接收的包；2.管理发送包的补发逻辑
    private ScheduledExecutorService scheduledExecutorService;
    InternalTools internalTools;
    private NetworkCommunicator networkCommunicator;

    public PacketTransmissionManager(DatagramSocket datagramSocket, long serverIdCRC, InternalTools internalTools, NetworkCommunicator networkCommunicator) {
        this.datagramSocket = datagramSocket;
        this.serverIdCRC = serverIdCRC;
        this.internalTools = internalTools;
        this.networkCommunicator = networkCommunicator;
        initCoreThreadPool();
    }
    /**
     * Thread size is the same with cpu cores.
     * Should only use for network lower level.
     *
     * @return
     */
    private ExecutorService initCoreThreadPool() {
        if(coreThreadPool == null) {
            synchronized (NetRuntime.class) {
                if(coreThreadPool == null) {
                    ThreadFactory namedThreadFactory =
                            new ThreadFactoryBuilder().setNameFormat("Net-CoreThreadPool-%d").build();
                    coreThreadPool = Executors.newFixedThreadPool(NetRuntime.getCpuCores(), namedThreadFactory);
                }
            }
        }
        return coreThreadPool;
    }

    ScheduledExecutorService getScheduledExecutorService() {
        if(scheduledExecutorService == null) {
            synchronized (NetRuntime.class) {
                if(scheduledExecutorService == null) {
                    ThreadFactory namedThreadFactory =
                            new ThreadFactoryBuilder().setNameFormat(this.toString() + "-Scheduled-ThreadPool-%d").build();
                    scheduledExecutorService = Executors.newScheduledThreadPool(NetRuntime.getCpuCores(), namedThreadFactory);
                }
            }
        }
        return scheduledExecutorService;
    }

    public PacketTransmissionManager startReceivingPacket() {
        if(receivePacketThread == null) {
            synchronized (this) {
                if(receivePacketThread == null) {
                    receivePacketThread = new ReceivePacketThread();
                    receivePacketThread.start();
                }
            }
        }
        return this;
    }

    public PacketTransmissionManager setReceivedListener(ReceivedListener receivedListener) {
        this.receivedListener = receivedListener;
        return this;
    }

    public interface ReceivedListener {
        void received(byte type, long serverIdCRC, byte[] data, InetSocketAddress address);
    }

    public void close() {
        if(receivePacketThread != null) {
            synchronized (this) {
                if(receivePacketThread != null) {
                    receivePacketThread.close();
                    receivePacketThread = null;
                }
            }
        }
        if(coreThreadPool != null) {
            coreThreadPool.shutdown();
            coreThreadPool = null;
        }
        if(scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
    }

    public static final int MTU = 1460; //1472
    public static final int PACKET_HEADER_SIZE = 1/*type*/ + 8/*serverIdCRC*/ + 2/*length*/ + 4/*id*/ + 4/*sequence*/;
    public static final int UNRELIABLE_PACKET_HEADER_SIZE = 1/*type*/ + 8/*serverIdCRC*/ + 2/*length*/;
    public static final int CLIENT_PACKET_HEAD_SIZE = 1/*version*/ + 1/*completeStatus*/ + 4/*requestCounterBytes*/ + 4/*startSequence*/;
    public static int SPLIT_PACKET_SIZE = MTU - PACKET_HEADER_SIZE - CLIENT_PACKET_HEAD_SIZE;
    private class ReceivePacketThread extends Thread {
        private boolean isStarted = true;
        private byte[] buf = new byte[MTU];

        public ReceivePacketThread() {
            super("ReceiveUDPPacketThread");
        }

        @Override
        public void run() {
            while(isStarted) {
                DatagramPacket packet
                        = new DatagramPacket(buf, buf.length);
                try {
                    datagramSocket.receive(packet);
                } catch (Throwable t) {
                    t.printStackTrace();
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "DatagramSocket receive packet failed, " + t.getMessage());
                    continue;
                }

                InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());
                byte[] data = packet.getData();
                if(data != null && data.length > 0) {
                    byte type = data[0];
                    switch (type) {
                        case PacketTransmission.TYPE_CLIENT_PACKET_COMPLETED:
                        case PacketSendingTransmission.TYPE_CLIENT_PACKET:
                        case PacketSendingTransmission.TYPE_CLIENT_PACKET_PARTIAL_COMPLETED:
                        case PacketTransmission.TYPE_CLIENT_CLOSED:
                            this.handleReceiverPacket(type, data, address);
                            break;
                        case PacketTransmission.TYPE_SERVER_GATHER_PACKET:
                        case PacketTransmission.TYPE_SERVER_COMPLETED:
                            this.handleSenderPacket(type, data, address);
                            break;
                        case PacketSendingTransmission.TYPE_UNRELIABLE_PACKET:
                        case PacketTransmission.TYPE_UNRELIABLE_PING:
                            this.handleUnreliablePacketReceived(type, data, address);
                            break;
                        default:
                            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Unexpected packet type " + type + " received from address " + address + ". Ignored...");
                            break;
                    }
                }
            }
        }

        // 处理服务器返回的response的包
        private void handleSenderPacket(byte type, byte[] data, InetSocketAddress address) {
            if(data != null && data.length > PACKET_HEADER_SIZE) {
                //type 1 byte
                //id 4 bytes by random
                //sequence 4 bytes
                //serverIdCRC 8 bytes
                long serverIdCRC = readPacketServerIdCRC(data);
                short length = readPacketLength(data);
                int id = readPacketId(data);
                int sequence = readPacketSequence(data);

                byte[] theData = null;
                if(length > 0) {
                    theData = new byte[length];
                    System.arraycopy(data, PACKET_HEADER_SIZE, theData, 0, theData.length);
                } else if(length == 0) {
                    theData = new byte[length];
                } else {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "handleSenderPacket length is minus " + length + " for type " + type + " serverCRCId " + serverIdCRC + " id " + id + ". Ignored...");
                    return;
                }

                PacketSendingTransmission sendingTransmission = idReliablePacketTransmissionMap.get(id);
                if(sendingTransmission != null) {
                    sendingTransmission.packetReceived(theData, type, sequence, serverIdCRC);
                } else {
                    PacketSendingTransmission.clientClosed(PacketTransmissionManager.this, id, PacketTransmission.CLOSED_REASON_COMPLETED, address);
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Unexpected gather packet received for id " + id + " serverIdCRC " + serverIdCRC + " send back client closed packet");
                }
            } else {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Illegal bytes from handleSenderPacket length " + (data != null ? data.length : 0) + " type " + type + " from address " + address);
            }
        }

        private long readPacketServerIdCRC(byte[] data) {
//            byte[] theData = new byte[8];
//            System.arraycopy(data, 1, theData, 0, 8);
//            return Longs.fromByteArray(theData);
            return Longs.fromBytes(data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8]);
        }

        private short readPacketLength(byte[] data) {
//            byte[] theData = new byte[2];
//            System.arraycopy(data, 9, theData, 0, 2);
//            return Shorts.fromByteArray(theData);
            return Shorts.fromBytes(data[9], data[10]);
        }

        private int readPacketId(byte[] data) {
//            byte[] theData = new byte[4];
//            System.arraycopy(data, 11, theData, 0, 4);
//            return Ints.fromByteArray(theData);
            return Ints.fromBytes(data[11], data[12], data[13], data[14]);
        }

        private int readPacketSequence(byte[] data) {
//            byte[] theData = new byte[4];
//            System.arraycopy(data, 15, theData, 0, 4);
//            return Ints.fromByteArray(theData);
            return Ints.fromBytes(data[15], data[16], data[17], data[18]);
        }

        // 处理客户端发来的包
        private void handleReceiverPacket(byte type, byte[] data, InetSocketAddress address) {
            if(data != null && data.length > PACKET_HEADER_SIZE) {
                //type 1 byte
                //id 4 bytes by random
                //sequence 4 bytes
                //serverIdCRC 8 bytes
                long serverIdCRC = readPacketServerIdCRC(data);
                short length = readPacketLength(data);
                int id = readPacketId(data);
                int sequence = readPacketSequence(data);

                byte[] theData = null;
                if(length > 0) {
                    theData = new byte[length];
                    try {
                        System.arraycopy(data, PACKET_HEADER_SIZE, theData, 0, theData.length);
                    } catch(Throwable t) {
                        t.printStackTrace();
                    }
                } else if(length == 0) {
                    theData = new byte[length];
                } else {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "handleReceiverPacket length is minus " + length + " for type " + type + " serverCRCId " + serverIdCRC + " id " + id + ". Ignored...");
                    return;
                }

                ConcurrentHashMap<Integer, PacketReceivingTransmission> receivingTransmissionMap = serverIdRandIdReceivingTransmissionMap.get(serverIdCRC);
                if(receivingTransmissionMap == null) {
                    receivingTransmissionMap = new ConcurrentHashMap<>();
                    ConcurrentHashMap<Integer, PacketReceivingTransmission> old = serverIdRandIdReceivingTransmissionMap.putIfAbsent(serverIdCRC, receivingTransmissionMap);
                    if(old != null) {
                        receivingTransmissionMap = old;
                    }
                }
                PacketReceivingTransmission receivingTransmission = receivingTransmissionMap.get(id);
                if(receivingTransmission == null) {
                    switch (type) {
                        case PacketTransmission.TYPE_CLIENT_PACKET:
                        case PacketTransmission.TYPE_CLIENT_PACKET_COMPLETED:
                        case PacketTransmission.TYPE_CLIENT_PACKET_PARTIAL_COMPLETED:
                            if(sequence > (PacketSendingTransmission.PARTIAL_AFTER_PACKETS)) {
                                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "Unexpected packet received (sequence too large compare to " + (PacketSendingTransmission.PARTIAL_AFTER_PACKETS) + ") for create PacketReceivingTransmission, type " + type + " serverCRCId " + serverIdCRC + " id " + id + " sequence " + sequence);
                                break;
                            }

                        case PacketTransmission.TYPE_UNRELIABLE_PACKET:
                            receivingTransmission = new PacketReceivingTransmission(id, PacketTransmissionManager.this, receivingTransmissionMap, address);
                            PacketReceivingTransmission old = receivingTransmissionMap.putIfAbsent(id, receivingTransmission);
                            if(old != null) {
                                receivingTransmission = old;
                                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "Create PacketReceivingTransmission from another thread " + receivingTransmission + " type " + type + " serverCRCId " + serverIdCRC + " id " + id);
                            } else {
                                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "Create PacketReceivingTransmission " + receivingTransmission + " type " + type + " serverCRCId " + serverIdCRC + " id " + id);
                            }
                            break;
                        default:
                            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "Unexpected packet received for create PacketReceivingTransmission, type " + type + " serverCRCId " + serverIdCRC + " id " + id + " sequence " + sequence);
                            break;
                    }
                }
                if(receivingTransmission != null)
                    receivingTransmission.receivePacket(theData, type, sequence, serverIdCRC);
            } else {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Illegal bytes from handlePacketReceived length " + (data != null ? data.length : 0) + " type " + type + " from address " + address);
            }
        }

        private void handleUnreliablePacketReceived(byte type, byte[] data, InetSocketAddress address) {
            if(data != null && data.length > UNRELIABLE_PACKET_HEADER_SIZE) {
                long serverIdCRC = readPacketServerIdCRC(data);
                short length = readPacketLength(data);
                byte[] realData = new byte[length];
                System.arraycopy(data, UNRELIABLE_PACKET_HEADER_SIZE, realData, 0, realData.length);

                invokeReceivedListener(type, serverIdCRC, realData, address, null);
            } else {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Illegal bytes from handleUnreliablePacketReceived length " + (data != null ? data.length : 0) + " from address " + address);
            }
        }

        public void close() {
            isStarted = false;
            try {
                datagramSocket.close();
            } catch (Throwable t) {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Close datagramSocket failed, " + t.getMessage());
            }
        }
    }

    // 将完整的包发给上层listener
    void invokeReceivedListener(byte type, long serverIdCRC, byte[] realData, InetSocketAddress address, Integer transmissionId) {
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "invokeReceivedListener will send type " + type + " serverIdCRC " + serverIdCRC + " id " + transmissionId/* + " counter " + counter.incrementAndGet()*/ + " coreThreadPool " + coreThreadPool);
        coreThreadPool.execute(() -> {
            try {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "invokeReceivedListener type " + type + " serverIdCRC " + serverIdCRC + " id " + transmissionId/* + " counter " + counter.incrementAndGet()*/ + " coreThreadPool " + coreThreadPool);
                receivedListener.received(type, serverIdCRC, realData, address);
            } catch(Throwable t) {
                t.printStackTrace();
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "receivedListener received failed, " + t.getMessage() + " for data length " + realData.length + " address " + address + " transmissionId " + transmissionId);
            }
        });
    }

    boolean transmissionCompleted(int id, PacketSendingTransmission packetSendingTransmission) {
        return idReliablePacketTransmissionMap.remove(id, packetSendingTransmission);
    }

    void clearPendingTransmissions() {
        idReliablePacketTransmissionMap.clear();
    }

    // 指定size发包
    public CompletableFuture<Void> sendPacket(InputStream is, int size, SocketAddress address) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if(address == null) {
            future.completeExceptionally(new IllegalArgumentException("Address is null while sendPacket"));
            return future;
//            return CompletableFuture.failedFuture(new IllegalArgumentException("Address is null while sendPacket"));
        }

        try {
            int counter = 0;
            int randId = randomForId.nextInt();
            PacketSendingTransmission transmission = new PacketSendingTransmission(randId, this);
            while(idReliablePacketTransmissionMap.putIfAbsent(randId, transmission) != null) {
                randId = randomForId.nextInt();
                transmission.setId(randId);
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "Regenerate random id for sending " + size + " bytes to address " + address + " for " + ++counter + " times");
            }

            return transmission.sendPacket(is, size, address);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            future.completeExceptionally(throwable);
            return future;
//            return CompletableFuture.failedFuture(throwable);
        }
    }

    // 不指定size发包
    public CompletableFuture<Void> sendPacket(InputStream is, SocketAddress address) {
        return sendPacket(is, -1, address);
    }

    // 发送不可靠的包
    public CompletableFuture<Void> sendUnreliablePacket(byte[] dataLessThanMTU, SocketAddress address) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if(address == null) {
            future.completeExceptionally(new IllegalArgumentException("Address is null while sendUnReliablePacket"));
            return future;
//            return CompletableFuture.failedFuture(new IllegalArgumentException("Address is null while sendUnReliablePacket"));
        }
        if(dataLessThanMTU == null) {
            future.completeExceptionally(new IllegalArgumentException("Data is null while sendUnReliablePacket to " + address));
            return future;
//            return CompletableFuture.failedFuture(new IllegalArgumentException("Data is null while sendUnReliablePacket to " + address));
        }
        if(dataLessThanMTU.length >= (MTU - UNRELIABLE_PACKET_HEADER_SIZE))  {
            future.completeExceptionally(new IllegalArgumentException("Unreliable packet can not exceed MTU " + (MTU - UNRELIABLE_PACKET_HEADER_SIZE) + " but actual is " + dataLessThanMTU.length));
            return future;
//            return CompletableFuture.failedFuture(new IllegalArgumentException("Unreliable packet can not exceed MTU " + (MTU - UNRELIABLE_PACKET_HEADER_SIZE) + " but actual is " + dataLessThanMTU.length));
        }
        if(datagramSocket == null || datagramSocket.isClosed()) {
            future.completeExceptionally(new IllegalStateException("DatagramSocket is not ready for sending, " + datagramSocket + " to address " + address));
            return future;
//            return CompletableFuture.failedFuture(new IllegalStateException("DatagramSocket is not ready for sending, " + datagramSocket + " to address " + address));
        }
        try {
            //type 1 byte
            //serverIdCRC 8 bytes
            //length 2 bytes
            byte[] sendData = new byte[dataLessThanMTU.length + UNRELIABLE_PACKET_HEADER_SIZE];
            byte[] serverIdCrcBytes = Longs.toByteArray(serverIdCRC); //8 bytes
            byte[] lengthBytes = Shorts.toByteArray((short) dataLessThanMTU.length); //2

            ByteUtils.copyBytes(sendData, new byte[]{PacketSendingTransmission.TYPE_UNRELIABLE_PACKET}, serverIdCrcBytes, lengthBytes, dataLessThanMTU);

            DatagramPacket datagramPacket = new DatagramPacket(sendData, 0, sendData.length, address);
//        datagramSocket.send(datagramPacket);
            sendPacketWithRetries(datagramSocket, datagramPacket);
            return CompletableFuture.completedFuture(null);
        } catch(Throwable throwable) {
            throwable.printStackTrace();
            future.completeExceptionally(throwable);
            return future;
//            return CompletableFuture.failedFuture(throwable);
        }
    }

    // 发送ping
    public void sendUnreliablePing(SocketAddress address) throws IOException {
        if(address == null) {
            throw new IllegalArgumentException("Address is null while sendUnReliablePacket");
        }
        if(datagramSocket == null || datagramSocket.isClosed()) {
            throw new IllegalStateException("DatagramSocket is not ready for sending, " + datagramSocket + " to address " + address);
        }
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        byte[] sendData = new byte[UNRELIABLE_PACKET_HEADER_SIZE];
        byte[] serverIdCrcBytes = Longs.toByteArray(serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) 0); //2

        ByteUtils.copyBytes(sendData, new byte[]{PacketSendingTransmission.TYPE_UNRELIABLE_PING}, serverIdCrcBytes, lengthBytes);

        DatagramPacket datagramPacket = new DatagramPacket(sendData, 0, sendData.length, address);
//        datagramSocket.send(datagramPacket);
        sendPacketWithRetries(datagramSocket, datagramPacket);
    }

    public static final int RETRY_TIMES = 3;
    protected void sendPacketWithRetries(DatagramSocket datagramSocket, DatagramPacket packet) throws IOException {
        Throwable exception = null;
        int times = RETRY_TIMES; //retry times when IO error occurred.
        for(int i = 0; i < times; i++) {
            try {
                datagramSocket.send(packet);
                if(exception != null) {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "Send packet to address " + packet.getAddress() + " successfully after retries, IOException was " + exception.getMessage() + " have retried to i " + i);
                    exception = null;
                }
                break;
            } catch (IOException e) {
                e.printStackTrace();
                exception = e;
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send packet to address " + packet.getAddress() + " failed, IOException " + e.getMessage() + " will continue retry from i " + i + " to " + times);
            } catch (Throwable t) {
                t.printStackTrace();
                exception = t;
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send packet to address " + packet.getAddress() + " failed, Throwable " + t.getMessage() + " will NOT retry from i " + i);
                break;
            }
        }

        if(exception != null) {
            if(exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new IOException("Unexpected unknown error while send packet to address " + packet.getAddress(), exception);
            }
        }
    }
}
