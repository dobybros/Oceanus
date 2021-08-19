package oceanus.sdk.core.net.rudpex.impl;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import oceanus.sdk.core.net.errors.TransmissionFailedException;
import oceanus.sdk.core.net.rudpex.communicator.RUDPEXNetworkCommunicator;
import oceanus.sdk.core.utils.ByteUtils;
import oceanus.sdk.logger.LoggerEx;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketSendingTransmission extends PacketTransmission {
    static final int PARTIAL_AFTER_PACKETS = 30;
    static final int RECEIVE_DELAY_PACKETS = 6; //RECEIVE_DELAY_PACKETS must lesser than PARTIAL_AFTER_PACKETS
    private static final String TAG = PacketSendingTransmission.class.getSimpleName();

    private AtomicInteger requestCounter = new AtomicInteger(0);

    // 一个小包的真正内容缓存
    byte[] buffer = new byte[PacketTransmissionManager.SPLIT_PACKET_SIZE];
    int /* 本次读的size */readSize, /* 已经读了的size */totalReadSize = 0, startPos = 0, startSequence = 0;
    int len, packetLen;

    final int MAX_REQUEST_RETRY_COUNT = 10;
    int requestRetryCount = 0;
    // 重新发送partial_completed或者completed的任务
    ScheduledFuture<?> requestRetryScheduleTask;
    private int currentRequestCounter = -1;
//    private long[] retryDelays = new long[]{50L, 100L, 200L, 500L, 1000L, 2000L};
    private long[] retryDelays = new long[]{3000L, 4000L, 5000L};
    public static final int STATE_NONE = 0;
    public static final int STATE_SENDING_PACKETS = 1;
    public static final int STATE_WAITING_GATHER = 10;
    public static final int STATE_COMPLETED = 20;
    private int state = STATE_NONE;

    private SocketAddress socketAddress;

    private ConcurrentSkipListMap<Integer, byte[]> sequencePacketBytesMap = new ConcurrentSkipListMap<>();
    private InputStream inputStream;
    // 消息内容总长度，可指定可不指定，不指定时可写size = -1
    private int size;

    private CompletableFuture<Void> future;
    private Integer lastSequence;

    public PacketSendingTransmission(int id, PacketTransmissionManager packetTransmissionManager) {
        transmissionManager = packetTransmissionManager;
        this.id = id;
        sequence = 0;
    }

    // 发送包
    private void sendPacket(byte[] buffer, int offset, int packetLen, byte completeStatus) throws IOException {
        sendPacket(buffer, offset, packetLen, completeStatus, null);
    }

    /**
     * // 发送指定sequence的部分包
     * @param buffer 大包
     * @param offset from
     * @param packetLen 发送多少字节
     * @param completeStatus 包类型，1.sending；2.partial completed；4.completed
     * @param specifiedSequence 特定sequence
     */
    private void sendPacket(byte[] buffer, int offset, int packetLen, byte completeStatus, Integer specifiedSequence) throws IOException {
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
//        packetLen += PacketTransmissionManager.CLIENT_PACKET_HEAD_SIZE;
        int currentSequence = specifiedSequence != null ? specifiedSequence : sequence++;
        byte[] typeByteArray = new byte[]{TYPE_CLIENT_PACKET}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) (packetLen + PacketTransmissionManager.CLIENT_PACKET_HEAD_SIZE)); //2
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
        byte[] sequenceBytes = Ints.toByteArray(currentSequence); //4 bytes

        byte[] versionBytes = new byte[]{1}; //1 byte
        byte[] completeStatusBytes = new byte[] {completeStatus};
        byte[] startSequenceBytes = Ints.toByteArray(startSequence); //4 bytes
        currentRequestCounter = requestCounter.getAndIncrement();
        byte[] requestCounterBytes = Ints.toByteArray(currentRequestCounter);
        if(completeStatus == COMPLETE_STATUS_COMPLETED) {
            if(lastSequence == null) {
                lastSequence = currentSequence;
            } else if(lastSequence != currentSequence){
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Wrong last sequence while send packet, lastSequence " + lastSequence + " currentSequence " + currentSequence + " id " + id);
            }
        }

        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE + PacketTransmissionManager.CLIENT_PACKET_HEAD_SIZE + packetLen];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes);

        byte[] realData = new byte[packetLen];
        System.arraycopy(buffer, offset, realData, 0, packetLen);

        ByteUtils.copyBytes(PacketTransmissionManager.PACKET_HEADER_SIZE, packedByteArray, versionBytes, completeStatusBytes, requestCounterBytes, startSequenceBytes, realData);
//        System.arraycopy(buffer, offset, packedByteArray, PacketTransmissionManager.PACKET_HEADER_SIZE + PacketTransmissionManager.CLIENT_PACKET_HEAD_SIZE, packetLen);

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);
//        if(specifiedSequence != null)
        sendPacketWithRetries(currentSequence, packedByteArray.length, packet);
        sequencePacketBytesMap.putIfAbsent(currentSequence, realData);
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendPacket id " + id + " len " + packetLen + " sequence " + currentSequence + " address " + socketAddress + " serverIdCRC " + transmissionManager.serverIdCRC + new String(realData));
//        if(sequence % PARTIAL_AFTER_PACKETS == 0) {
//            sendPartialCompleted(address);
//        }
    }

    // 发送单独的completed包，用于重发completed逻辑
    private void sendCompleted() throws IOException {
        int length = 1/*version*/ + 4/*requestCounterBytes*/;
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
//        isCompleted = true;
        byte[] typeByteArray = new byte[]{TYPE_CLIENT_PACKET_COMPLETED}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) length); //2
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
        byte[] sequenceBytes = Ints.toByteArray(sequence); //4 bytes

        currentRequestCounter = requestCounter.getAndIncrement();
        byte[] version = new byte[]{1}; //1 byte
        byte[] requestCounterBytes = Ints.toByteArray(currentRequestCounter);

        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE + length];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes, version, requestCounterBytes);

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);
        sendPacketWithRetries(sequence, packedByteArray.length, packet);
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendClientCompleted id " + id + " len " + packedByteArray.length + " sequence " + sequence + " address " + socketAddress + " serverIdcCRC " + transmissionManager.serverIdCRC + " requestCounterValue " + currentRequestCounter);
//        for(int i = 0; i < RECEIVE_DELAY_PACKETS; i++) {
//        }
//        return requestCounterValue;
    }

    // 发送单独的partial completed包，用于重发partial completed逻辑
    private void sendPartialCompleted() throws IOException {
        int length = 1/*version*/ + 4/*requestCounterBytes*/ + 4/*startSequenceBytes*/;
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
//        isCompleted = true;
        byte[] typeByteArray = new byte[]{TYPE_CLIENT_PACKET_PARTIAL_COMPLETED}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) length); //2
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
//        partialSequence = sequence - RECEIVE_DELAY_PACKETS;
        byte[] sequenceBytes = Ints.toByteArray(sequence); //4 bytes

        byte[] version = new byte[]{1}; //1 byte
        byte[] startSequenceBytes = Ints.toByteArray(startSequence); //4 bytes
        currentRequestCounter = requestCounter.getAndIncrement();
        byte[] requestCounterBytes = Ints.toByteArray(currentRequestCounter);

        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE + length];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes, version, requestCounterBytes, startSequenceBytes);

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);
        sendPacketWithRetries(sequence, packedByteArray.length, packet);
//        for(int i = 0; i < RECEIVE_DELAY_PACKETS; i++) {
//        }
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendPartialCompleted id " + id + " len " + len + " sequence " + sequence + " address " + socketAddress + " serverIdcCRC " + transmissionManager.serverIdCRC + " requestCounterValue " + currentRequestCounter);
//        return requestCounterValue;
    }

    // 没有发完小包并且close transmission时发送关闭消息
    private void sendClosed(short reason) throws IOException {
        int length = 1/*version*/ + 2/*reason*/;
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
//        isCompleted = true;
        byte[] typeByteArray = new byte[]{TYPE_CLIENT_CLOSED}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) length); //2 bytes
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
        byte[] sequenceBytes = Ints.toByteArray(sequence); //4 bytes

        byte[] version = new byte[]{1}; //1 byte
        byte[] reasonBytes = Shorts.toByteArray(reason); //4 bytes

        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE + length];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes, version, reasonBytes);

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);
        sendPacketWithRetries(sequence, packedByteArray.length, packet);
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendClosed id " + id + " len " + len + " sequence " + sequence + " address " + socketAddress + " serverIdcCRC " + transmissionManager.serverIdCRC);
    }

    // close transmission的静态方法
    public static void clientClosed(PacketTransmissionManager transmissionManager, int id, short reason, SocketAddress socketAddress) {
        int length = 1/*version*/ + 2/*reason*/;
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
//        isCompleted = true;
        byte[] typeByteArray = new byte[]{TYPE_CLIENT_CLOSED}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) length); //2 bytes
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
        byte[] sequenceBytes = Ints.toByteArray(-1); //4 bytes，-1是随便取的值

        byte[] version = new byte[]{1}; //1 byte
        byte[] reasonBytes = Shorts.toByteArray(reason); //4 bytes

        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE + length];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes, version, reasonBytes);

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);
        try {
            transmissionManager.datagramSocket.send(packet);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendClientClosed id " + id + " len " + packedByteArray.length + " sequence " + -1 + " address " + socketAddress + " serverIdcCRC " + transmissionManager.serverIdCRC);
        } catch (IOException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Send client closed packet id " + id + " length " + packedByteArray.length + " address " + socketAddress + " failed, IOException " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            LoggerEx.error(TAG, "Send client closed packet id " + id +  " length " + packedByteArray.length + " address " + socketAddress + " failed, Throwable " + t.getMessage());
        }
    }

//    private void sendPing(SocketAddress address) throws IOException {
//        //type 1 byte
//        //serverIdCRC 8 bytes
//        //length 2 bytes
//        //id 4 bytes by random
//        //sequence 4 bytes
//        //centent 1472 bytes - above (17) = 1455
//        isCompleted = true;
//        byte[] typeByteArray = new byte[]{TYPE_CLIENT_PING}; //1 byte
//        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
//        byte[] lengthBytes = Shorts.toByteArray((short) 0); //2
//        byte[] idBytes = Ints.toByteArray(id); //4 bytes
//        byte[] version = new byte[]{1}; //1 byte
//        byte[] maxPacketsBufferBytes = Shorts.toByteArray(MAX_PACKETS_BUFFER); //2 bytes
//        byte[]
//
//        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE];
//        //Below coding is all for performance.
//        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes);
//
//        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, address);
//        sendPacketWithRetries(sequence, packedByteArray.length, packet);
//    }

    @Override
    public boolean close() {
        return close(CLOSED_REASON_ACTIVE);
    }

    private boolean close(short reason) {
        boolean bool = false;
        if(state != STATE_COMPLETED) {
            synchronized (this) {
                if(state != STATE_COMPLETED) {
                    state = STATE_COMPLETED;
                    bool = true;
                }
            }
        }
        if(bool) {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "close change state to STATE_COMPLETED id " + id + " sequence " + sequence);
//            boolean removed = transmissionManager.idReliablePacketTransmissionMap.remove(id, this);
            boolean removed = transmissionManager.transmissionCompleted(id, this);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "close reason " + reason + " removed " + removed + " id " + id + " address " + socketAddress);
            if(removed && reason != CLOSED_REASON_COMPLETED) {
                try {
                    sendClosed(reason);
                } catch (IOException e) {
                    e.printStackTrace();
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "PacketSendingTransmission close failed, " + e.getMessage() + " socketAddress " + socketAddress);
                }
            }
            resetRequestRetry();
            if(!future.isDone()) {
                if(reason == CLOSED_REASON_COMPLETED) {
                    future.complete(null);
                } else {
                    String str = null;
                    switch (reason) {
                        case CLOSED_REASON_IO_ERROR:
                            str = "IO Error";
                            break;
                        case CLOSED_REASON_ACTIVE:
                            str = "Closed manually";
                            break;
                        case CLOSED_REASON_TIMEOUT:
                            str = "Timeout";
                            break;
                        default:
                            str = "Unknown error";
                            break;
                    }
                    future.completeExceptionally(new TransmissionFailedException(reason, str));
                }
            }
            return removed;
        }
        return false;
    }

    // 清除重新发送partial_completed或者completed的任务
    private void resetRequestRetry() {
        requestRetryCount = 0;
        if(requestRetryScheduleTask != null) {
            requestRetryScheduleTask.cancel(true);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "requestRetryScheduleTask " + requestRetryScheduleTask + " shall be cancelled, id " + id + " sequence " + sequence + " startSequence " + startSequence);
            requestRetryScheduleTask = null;
        }
    }

    // 开始发送inputStream
    public CompletableFuture<Void> sendPacket(InputStream is, int size, SocketAddress address) {
        boolean bool = false;
        if(state == STATE_NONE) {
            synchronized (this) {
                if(state == STATE_NONE) {
                    state = STATE_SENDING_PACKETS;
                    bool = true;
                }
            }
        }
        if(bool) {
            future = new CompletableFuture<>();
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendPacket state change be STATE_SENDING_PACKETS id " + id + " sequence " + sequence);

            socketAddress = address;
            if(size < 0) {
                len = PacketTransmissionManager.SPLIT_PACKET_SIZE;
            } else {
                len = Math.min(PacketTransmissionManager.SPLIT_PACKET_SIZE, size - totalReadSize);
            }
            packetLen = len;
            inputStream = is;
            this.size = size;

            if(handleSendPackets(PARTIAL_AFTER_PACKETS)) {
                waitGatherPacket();
            }
            return future;
        } else {
            future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("sendPacket is already sending, size " + size + " address " + address));
            return future;
//            return CompletableFuture.failedFuture(new IllegalStateException("sendPacket is already sending, size " + size + " address " + address));
        }
    }

    // 继续发送inputStream中的流
    boolean completed = false;
    private boolean handleSendPackets(int count) {
        try {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "handleSendPackets count " + count + " id " + id + " address " + socketAddress);
            if(count >= 0) {
                boolean result = sendPackets(count);
                if(result && !completed) {
                    completed = true;
                }
            }
            return true;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            boolean deleted = close(CLOSED_REASON_IO_ERROR);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send reliable packet failed, " + throwable.getMessage() + " id " + id + " address " + socketAddress + " deleted " + deleted);
            return false;
        }
    }

    // 开启gather timer，等待gather，若一段时间后此timer没有销毁，就重新补发partial_completed或者completed包，若一直收不到gather，就按超时处理
    private void startGatherTimer() {
        try {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendPacketResult currentRequestCounter " + currentRequestCounter + " id " + id + " address " + socketAddress + " requestRetryScheduleTask " + requestRetryScheduleTask);
            if(requestRetryScheduleTask == null) {
                long delay;
                if(requestRetryCount >= retryDelays.length) {
                    delay = retryDelays[retryDelays.length - 1];
                } else {
                    delay = retryDelays[requestRetryCount];
                }
                requestRetryScheduleTask = transmissionManager.getScheduledExecutorService().schedule(() -> {
                    if(state == STATE_COMPLETED) {
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "requestRetryScheduleTask " + requestRetryScheduleTask + " should not be running as transmission is closed, id " + id + " sequence " + sequence + " startSequence " + startSequence);
                        return;
                    }

                    if(requestRetryCount++ < MAX_REQUEST_RETRY_COUNT) {
                        requestRetryScheduleTask = null;
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendPacketResult timeout resend completed "  + completed + " id " + id + " address " + socketAddress);
//                            sendPacketResult(completed);
                        try {
                            if(completed) {
                                sendCompleted();
                            } else {
                                sendPartialCompleted();
                            }
                        } catch(Throwable throwable) {
                            throwable.printStackTrace();
                            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "send " + (completed ? "Completed" : "Partial completed") + " packet failed, " + throwable.getMessage() + " id " + id + " sequence " + sequence + " startSequence " + startSequence + " requestCounter " + requestCounter.get());
                        }
                        startGatherTimer();
                    } else {
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendPacketResult timeout close completed "  + completed + " id " + id + " address " + socketAddress);
                        close(CLOSED_REASON_TIMEOUT);
                    }
                }, delay, TimeUnit.MILLISECONDS);
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "requestRetryScheduleTask " + requestRetryScheduleTask + " started delay " + delay + " id " + id + " sequence " + sequence);
            } else {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendPacketResult requestRetryScheduleTask exists already, do nothing, currentRequestCounter " + currentRequestCounter + " id " + id + " address " + socketAddress);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            boolean deleted = close(CLOSED_REASON_IO_ERROR);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send reliable packet failed, " + throwable.getMessage() + " id " + id + " address " + socketAddress + " deleted " + deleted);
        }
    }

    // 发送指定sequence的包
    private void sendPacketsOnSequences(List<Integer> sequences) throws IOException {
        if(sequences != null) {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "Resend missing sequences " + sequences + " keys " + sequencePacketBytesMap.keySet() + " id " + id + " address " + socketAddress);
            for (int i = 0; i < sequences.size(); i++) {
                Integer sequence = sequences.get(i);
                byte[] data = sequencePacketBytesMap.get(sequence);
                if(data != null) {
                    if(i == sequences.size() - 1) {
                        sendPacket(data, 0, data.length, sequence.equals(lastSequence) ? COMPLETE_STATUS_COMPLETED : COMPLETE_STATUS_PARTIAL_COMPLETED, sequence);
                    } else {
                        sendPacket(data, 0, data.length, COMPLETE_STATUS_SENDING, sequence);
                    }
                } else {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "No data for sequence " + sequence + " id " + id + " startSequence " + startSequence + " address " + socketAddress);
                }
            }
        }
    }

    /**
     * 将要发送的数据读number个包并发送，发到第number个包带上partial_completed标识
     * @param numOfPackets 读包个数
     * @return 是否已经解包完成
     */
    private boolean sendPackets(int numOfPackets) throws IOException {
        int counter = 0;
        Throwable throwable = null;
        boolean breakByCounter = false;
        //inputStream读到末尾了， 或者totalReadSize读到指定的size总数时， 结束发送Packet。
        try {
            while((size < 0 || size > totalReadSize) && ((readSize = inputStream.read(buffer, startPos, len)) != -1)) {
                totalReadSize += readSize;
                counter++;
                if(counter >= numOfPackets) {
                    breakByCounter = true;
                }
                if((len -= readSize) == 0) {
                    //send
                    if(breakByCounter) {
                        sendPacket(buffer, 0, packetLen, COMPLETE_STATUS_PARTIAL_COMPLETED);
                    } else {
                        sendPacket(buffer, 0, packetLen, COMPLETE_STATUS_SENDING);
                    }
                    if(size < 0) {
                        startPos = 0;
                        len = PacketTransmissionManager.SPLIT_PACKET_SIZE;
                    } else if(size == totalReadSize) {
                        //all packets send
                        break;
                    } else {
                        startPos = 0;
                        len = Math.min(PacketTransmissionManager.SPLIT_PACKET_SIZE, size - totalReadSize);
                    }
                    packetLen = len;
                } else {
                    startPos += readSize;
//                    len -= readSize;
                }
                if(breakByCounter)
                    break;
            }
        } catch(Throwable t) {
            throw t;
        }
        if(!breakByCounter) {
//            if(startPos > 0) {//怀疑有bug， 如果startPos刚好是0的时候， 会不会发不出completed包
            if(readSize == -1) {
                //TODO startPos == 0的时候应该测试一下
                if(totalReadSize != 0) {
                    sendPacket(buffer, 0, startPos, COMPLETE_STATUS_COMPLETED);
                } else {
                    LoggerEx.error(TAG, "Unexpected totalReadSize == 0 while sending packet for id " + id  + " sequence " + sequence);
                }
            }
            return true;
        }
        return false;
    }

    // 处理收到的gather和server返回的completed
    public void packetReceived(byte[] theData, byte type, int sequence, long serverIdCRC) {
        switch (type) {
            case PacketTransmission.TYPE_SERVER_GATHER_PACKET:
                if(handleReceivedRequestCounter(theData)) {
                    try {
                        gatherPacketReceived(theData, type, sequence, serverIdCRC);
                    } catch (IOException e) {
                        e.printStackTrace();
                        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "gatherPacketReceived failed, " + e.getMessage() + " type " + type + " sequence " + sequence + " serverIdCRC " + serverIdCRC);
                    }
                } else {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "gatherPacketReceived requestCounter is not matching current " + currentRequestCounter + " type " + type + " sequence " + sequence + " serverIdCRC " + serverIdCRC);
                }
                break;
            case PacketTransmission.TYPE_SERVER_COMPLETED:
                try {
                    completedPacketReceived(theData, type, sequence, serverIdCRC);
                } catch (IOException e) {
                    e.printStackTrace();
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "gatherPacketReceived failed, " + e.getMessage() + " type " + type + " sequence " + sequence + " serverIdCRC " + serverIdCRC);
                }
//                if(handleReceivedRequestCounter(theData)) {
//                } else {
//                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "completedPacketReceived requestCounter is not matching current " + currentRequestCounter + " type " + type + " sequence " + sequence + " serverIdCRC " + serverIdCRC);
//                }
                break;
            default:
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Unexpected packet type " + type + " received from handleSenderPacket address " + socketAddress);
                break;
        }
    }

    private void completedPacketReceived(byte[] theData, byte type, int sequence, long serverIdCRC) throws IOException {
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "completedPacketReceived " + " id " + id + " address " + socketAddress);
        close(CLOSED_REASON_COMPLETED);
    }

    // 判断收到的包是不是正在等待的gather
    private boolean handleReceivedRequestCounter(byte[] theData) {
        int requestCounter = readRequestCounter(theData);
        if(currentRequestCounter == requestCounter) {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "handleReceivedRequestCounter true currentRequestCounter " + currentRequestCounter + " id " + id + " address " + socketAddress);
            boolean bool = false;
            if(state == STATE_WAITING_GATHER) {
                synchronized (this) {
                    if(state == STATE_WAITING_GATHER) {
                        state = STATE_SENDING_PACKETS;
                        bool = true;
                    }
                }
            }
            if(!bool) {
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "handleReceivedRequestCounter change state to STATE_SENDING_PACKETS failed, expecting state to be STATE_WAITING_GATHER, but actual is " + state + " id " + id + " sequence " + sequence);
                return false;
            }
            return true;
        } else {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "handleReceivedRequestCounter false currentRequestCounter " + currentRequestCounter + " requestCounter " + requestCounter + " id " + id + " address " + socketAddress);
        }
        return false;
    }

    // 处理收到的gather包，如果有丢失的，就补发丢失的包，否则继续发送，等下一个gather
    public void gatherPacketReceived(byte[] theData, byte type, int sequence, long serverIdCRC) throws IOException {
        List<Integer> missSequences = readGatherPacketMissSequences(theData);
        int theSequence;
        if(missSequences != null && !missSequences.isEmpty()) {
            Integer minSequence = missSequences.get(0);
            theSequence = minSequence;
//            for(int i = startSequence; i < sequence; i++) {
//                if(!missSequences.contains(i)) {
//                    sequencePacketBytesMap.remove(i);
//                }
//            }
        } else {
            theSequence = sequence;
        }
        if(theSequence > startSequence) {
            startSequence = theSequence;
            resetRequestRetry();
        } else {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "gatherPacketReceived gather packet received startSequence " + startSequence + " but change to " + theSequence + " from serverIdCRC " + serverIdCRC + " id " + id + " sequence " + sequence + " missing " + missSequences);
        }
        for(int i = startSequence - 1; i >= 0; i--) {
            byte[] removed = sequencePacketBytesMap.remove(i);
            if(removed == null)
                break;
        }
        boolean needGather = true;
        if(missSequences != null && !missSequences.isEmpty()) {
            sendPacketsOnSequences(missSequences);
        } else {
            int count = PARTIAL_AFTER_PACKETS;
            needGather = handleSendPackets(count);
        }
        if(needGather)
            waitGatherPacket();
//        int count = PARTIAL_AFTER_PACKETS - (missSequences == null ? 0 : missSequences.size());
//        if(missSequences == null || missSequences.isEmpty())
//            count = PARTIAL_AFTER_PACKETS;
//        else
//            count = -1;
//        handleSendPackets(count);
    }

    // 发送一部分包后等待gather
    private void waitGatherPacket() {
        boolean bool = false;
        if(state == STATE_SENDING_PACKETS) {
            synchronized (this) {
                if(state == STATE_SENDING_PACKETS) {
                    state = STATE_WAITING_GATHER;
                    bool = true;
//                LoggerEx.info(TAG, "handleSendPackets change state to STATE_WAITING_GATHER id " + id + " sequence " + sequence);
                }
            }
        }
        if(bool) {
            startGatherTimer();
        } else {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "handleSendPackets sendPacketResult failed, state expecting to be STATE_SENDING_PACKETS but actual is " + state + " id " + id + " sequence " + sequence);
        }
    }

    // 获取包的counter
    private int readRequestCounter(byte[] data) {
        int offset = 1/*version*/;
        return Ints.fromBytes(data[offset], data[offset + 1], data[offset + 2], data[offset + 3]);
    }

    // 获取丢失的sequence
    private List<Integer> readGatherPacketMissSequences(byte[] theData) {
        int offset = 1/*version*/ + 4/*requestCounterBytes*/;
        short length = Shorts.fromBytes(theData[offset], theData[offset + 1]);
        if(length > 0)
            return ByteUtils.readIntList(offset + 2/*length*/, theData, length);
        return null;
    }
}
