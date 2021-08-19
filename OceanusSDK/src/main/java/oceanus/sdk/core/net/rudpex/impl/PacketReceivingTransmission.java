package oceanus.sdk.core.net.rudpex.impl;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import oceanus.sdk.core.net.rudpex.communicator.RUDPEXNetworkCommunicator;
import oceanus.sdk.core.utils.ByteUtils;
import oceanus.sdk.logger.LoggerEx;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class PacketReceivingTransmission extends PacketTransmission {

    private static final String TAG = PacketReceivingTransmission.class.getSimpleName();
    private boolean completed = false;

    // 接收到的包的sequences
    private ConcurrentSkipListSet<Integer> sequencePacketSet = new ConcurrentSkipListSet<>();
    private ConcurrentHashMap<Integer, PacketReceivingTransmission> receivingTransmissionMap;
    // 接收到的小包的集合
    private SequenceBytesCollector sequenceBytesCollector;

    private int startSequence = 0;

    public PacketReceivingTransmission(int id, PacketTransmissionManager packetTransmissionManager, ConcurrentHashMap<Integer, PacketReceivingTransmission> receivingTransmissionMap, InetSocketAddress address) {
        transmissionManager = packetTransmissionManager;
        this.receivingTransmissionMap = receivingTransmissionMap;
        this.id = id;
        this.socketAddress = address;
    }

    private byte readPacketVersion(byte[] data) {
        return data[0];
    }

    // 获取partial_completed或者completed包的counter
    private int readRequestCounter(byte[] data) {
        int offset = 1/*version*/;
        return Ints.fromBytes(data[offset], data[offset + 1], data[offset + 2], data[offset + 3]);
    }

    private short readCloseReason(byte[] data) {
        int offset = 1/*version*/;
        return Shorts.fromBytes(data[offset], data[offset + 1]);
    }

    // 获取partial_completed的startSequence
    private int readPartialPacketStartSequence(byte[] data) {
//            byte[] theData = new byte[4];
//            System.arraycopy(data, 15, theData, 0, 4);
//            return Ints.fromByteArray(theData);
        int offset = 1/*version*/ + 4/*requestCounterBytes*/;
        return Ints.fromBytes(data[offset], data[offset + 1], data[offset + 2], data[offset + 3]);
    }

    // 获取正常包的startSequence
    private int readClientPacketStartSequence(byte[] data) {
        int offset =  1/*version*/ + 1/*completeStatus*/ + 4/*requestCounterBytes*/;
        return Ints.fromBytes(data[offset], data[offset + 1], data[offset + 2], data[offset + 3]);
    }

    // 获取正常包的counter
    private int readClientPacketRequestCounter(byte[] data) {
        int offset = 1/*version*/ + 1/*completeStatus*/;
        return Ints.fromBytes(data[offset], data[offset + 1], data[offset + 2], data[offset + 3]);
    }

    // 获取正常包的完成状态
    private byte readClientPacketCompleteStatus(byte[] data) {
//            byte[] theData = new byte[4];
//            System.arraycopy(data, 15, theData, 0, 4);
//            return Ints.fromByteArray(theData);
        int offset =  1/*version*/;
        return data[offset];
    }

    // 处理接收到的包
    public void receivePacket(byte[] theData, byte typePacket, int sequence, long serverIdCRC) {
        if(completed) {
            try {
//                int requestCounter = readRequestCounter(theData);
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.warn(TAG, "receivePacket has completed, will ignore for id " + id + " type " + typePacket + " sequence " + sequence);
                if(typePacket != PacketTransmission.TYPE_CLIENT_CLOSED) {
                    sendCompleted();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send completed packet to sender failed, " + e.getMessage() + " because the transmission has completed, id " + id + " sequence " + sequence);
            }
            return;
        }
        if(sequenceBytesCollector == null) {
            synchronized (this) {
                if(sequenceBytesCollector ==  null) {
                    sequenceBytesCollector = new MemorySequenceBytesCollector();
                }
            }
        }
        switch (typePacket) {
            case PacketTransmission.TYPE_CLIENT_PACKET:
                byte completeStatus = readClientPacketCompleteStatus(theData);
                int startSequenceClientPacket = readClientPacketStartSequence(theData);
                int requestCounterClientPacket = readClientPacketRequestCounter(theData);
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "receivePacket TYPE_CLIENT_PACKET id " + id + " type " + typePacket + " sequence " + sequence + " completeStatus " + completeStatus + " startSequenceClientPacket " + startSequenceClientPacket + " requestCounterClientPacket " + requestCounterClientPacket);
                if(sequence > this.sequence) {
                    this.sequence = sequence;
                }
                sequencePacketSet.add(sequence);
                byte[] realData = new byte[theData.length - PacketTransmissionManager.CLIENT_PACKET_HEAD_SIZE];
                System.arraycopy(theData, PacketTransmissionManager.CLIENT_PACKET_HEAD_SIZE, realData, 0, realData.length);
                sequenceBytesCollector.receivedSequenceBytes(sequence, realData);

                switch (completeStatus) {
                    case COMPLETE_STATUS_COMPLETED:
                        handleSendCompletePacket(typePacket, serverIdCRC, requestCounterClientPacket);
                        break;
                    case COMPLETE_STATUS_PARTIAL_COMPLETED:
                        handleSendGatherPacket(typePacket, startSequenceClientPacket, requestCounterClientPacket);
                        break;
//                    case COMPLETE_STATUS_SENDING:
//                        break;
//                    default:
//                        break;
                }

                break;
            case PacketTransmission.TYPE_CLIENT_PACKET_PARTIAL_COMPLETED:
//                sendGatherPacket(findMissSequences(sequence, sequencePacketBytesMap.keySet()), address);
                int startSequencePartial = readPartialPacketStartSequence(theData);
                int requestCounterForPartial = readRequestCounter(theData);
                handleSendGatherPacket(typePacket, startSequencePartial, requestCounterForPartial);
                break;
            case PacketTransmission.TYPE_CLIENT_PACKET_COMPLETED:
                int requestCounterForCompleted = readRequestCounter(theData);
                handleSendCompletePacket(typePacket, serverIdCRC, requestCounterForCompleted);
                break;
            case PacketTransmission.TYPE_CLIENT_CLOSED:
                short reason = readCloseReason(theData);
                close(reason);
                break;
        }
    }

    // 处理收到的completed包：如果有没收到的包就发送gather，否则发送completed，
    private void handleSendCompletePacket(byte typePacket, long serverIdCRC, int requestCounterForCompleted) {
        List<Integer> missingSequences = findMissSequences(startSequence, sequence, sequencePacketSet);
        if(missingSequences == null || missingSequences.isEmpty()) {
            sequenceBytesCollector.completed();
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "receivePacket TYPE_CLIENT_PACKET_COMPLETED sendCompleted id " + id + " type " + typePacket + " sequence " + sequence + " startSequence " + startSequence + " requestCounterForCompleted " + requestCounterForCompleted);
            try {
                sendCompleted();
            } catch (IOException e) {
                e.printStackTrace();
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "sendGatherPacket#sendCompleted failed, " + e.getMessage() + " to host " + socketAddress + " type " + typePacket);
            }
            //TODO use single thread queue against every remote server.
            transmissionManager.invokeReceivedListener(typePacket, serverIdCRC, sequenceBytesCollector.collectAllBytes(), socketAddress, id);
            close();
        } else {
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "receivePacket TYPE_CLIENT_PACKET_COMPLETED sendGatherPacket id " + id + " type " + typePacket + " sequence " + sequence + " startSequence " + startSequence + " missingSequences " + missingSequences + " requestCounterForCompleted " + requestCounterForCompleted);
            try {
                sendGatherPacket(missingSequences, requestCounterForCompleted);
            } catch (Throwable e) {
                e.printStackTrace();
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "sendGatherPacket failed, " + e.getMessage() + " to host " + socketAddress + " type " + typePacket);
            }
        }
    }

    // 收到partial_completed的处理：删除之前的包，发送gather
    private void handleSendGatherPacket(byte typePacket, int theStartSequence, int theRequestCounter) {
        if(theStartSequence > startSequence) {
            startSequence = theStartSequence;
        }

        if(startSequence > 0) {
            for(int i = startSequence - 1; i >= 0; i--) {
                boolean removed = sequencePacketSet.remove(i);
                if(!removed)
                    break;
            }
        }
        List<Integer> missSequences = findMissSequences(startSequence, sequence, sequencePacketSet);
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "receivePacket TYPE_CLIENT_PACKET_PARTIAL_COMPLETED id " + id + " type " + typePacket + " sequence " + sequence + " startSequence " + startSequence + " partialMissingSequences " + missSequences + " requestCounterForPartial " + theRequestCounter);
        try {
            sendGatherPacket(missSequences, theRequestCounter);
        } catch (Throwable e) {
            e.printStackTrace();
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "sendGatherPacket failed, " + e.getMessage() + " to host " + socketAddress);
        }
    }

    // 查找没有收到的sequence
    private List<Integer> findMissSequences(int startSequence, int maxSequence, Collection<Integer> keys) {
//        Collection<Integer> keys = map.keySet();
        List<Integer> list = null;
        maxSequence += 1;
        int start = startSequence;
        Integer lastKey = 0;
        for(Integer key : keys) {
            lastKey = key;
            if (start > key) {
                continue;
            } else if(start == key) {
                start++;
                continue;
            } else {
                int offset = start;
                int distance = key - start;
                if(list == null) list = new ArrayList<>();
                for(;start < offset + distance; start++) {
                    list.add(start);
                }
                start = key + 1;
            }
        }
        lastKey += 1;
        if(lastKey <= maxSequence) {
            int distance = maxSequence - lastKey;
            if(list == null) list = new ArrayList<>();
            for(;start < lastKey + distance; start++) {
                list.add(start);
            }
        }
        return list;
    }

    // 发送gather包
    public void sendGatherPacket(List<Integer> missSequences, int requestCounter) throws IOException {
        int missLength = (missSequences == null ? 0 : missSequences.size()) * 4;
        int missSize = (missSequences == null ? 0 : missSequences.size());
        int length = 1/*version*/ + 4/*requestCounterBytes*/ + 2/*missSequenceLength*/ + missLength;
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
        byte[] typeByteArray = new byte[]{TYPE_SERVER_GATHER_PACKET}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) length); //2 bytes
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
        byte[] sequenceBytes = Ints.toByteArray(sequence); //4 bytes

        byte[] version = new byte[]{1}; //1 byte
        byte[] requestCounterBytes = Ints.toByteArray(requestCounter);
        byte[] missSequenceLength = Shorts.toByteArray((short) missSize); //2 bytes

        byte[] packedByteArray = new byte[length + PacketTransmissionManager.PACKET_HEADER_SIZE];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes, version, requestCounterBytes, missSequenceLength);
        if(missLength > 0) {
            ByteUtils.copyBytes(PacketTransmissionManager.PACKET_HEADER_SIZE + 1/*version*/ + 4/*requestCounterBytes*/ + 2/*missSequenceLength*/, packedByteArray, missSequences);
        }

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);
        sendPacketWithRetries(-1, packedByteArray.length, packet);
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendGatherPacket id " + id + " len " + length + " sequence " + sequence + " address " + socketAddress + " serverIdCRC " + transmissionManager.serverIdCRC + " missLength " + missLength + " requestCounter " + requestCounter);
    }

    // 发送completed的静态方法
    public static void serverCompleted(PacketTransmissionManager transmissionManager, int id, SocketAddress socketAddress) {
        int length = 1/*version*/;
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
        byte[] typeByteArray = new byte[]{TYPE_SERVER_COMPLETED}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) length); //2 bytes
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
        byte[] sequenceBytes = Ints.toByteArray(-1); //4 bytes

        byte[] version = new byte[]{1}; //1 byte
//        byte[] requestCounterBytes = Ints.toByteArray(-1);

        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE + length];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes, version);

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);

        try {
            transmissionManager.datagramSocket.send(packet);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "send ServerCompleted id " + id + " len " + packedByteArray.length + " sequence " + -1 + " address " + socketAddress + " serverIdcCRC " + transmissionManager.serverIdCRC);
        } catch (IOException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Send ServerCompleted id " + id + " length " + packedByteArray.length + " address " + socketAddress + " failed, IOException " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            LoggerEx.error(TAG, "Send ServerCompleted id " + id +  " length " + packedByteArray.length + " address " + socketAddress + " failed, Throwable " + t.getMessage());
        }
    }

    // 发送completed包
    public void sendCompleted() throws IOException {
        int length = 1/*version*/;
        //type 1 byte
        //serverIdCRC 8 bytes
        //length 2 bytes
        //id 4 bytes by random
        //sequence 4 bytes
        //centent 1472 bytes - above (17) = 1455
        byte[] typeByteArray = new byte[]{TYPE_SERVER_COMPLETED}; //1 byte
        byte[] serverIdCrcBytes = Longs.toByteArray(transmissionManager.serverIdCRC); //8 bytes
        byte[] lengthBytes = Shorts.toByteArray((short) length); //2 bytes
        byte[] idBytes = Ints.toByteArray(id); //4 bytes
        byte[] sequenceBytes = Ints.toByteArray(sequence); //4 bytes

        byte[] version = new byte[]{1}; //1 byte
//        byte[] requestCounterBytes = Ints.toByteArray(-1);

        byte[] packedByteArray = new byte[PacketTransmissionManager.PACKET_HEADER_SIZE + length];
        //Below coding is all for performance.
        ByteUtils.copyBytes(packedByteArray, typeByteArray, serverIdCrcBytes, lengthBytes, idBytes, sequenceBytes, version);

        DatagramPacket packet = new DatagramPacket(packedByteArray, 0, packedByteArray.length, socketAddress);
        sendPacketWithRetries(sequence, packedByteArray.length, packet);
        if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "sendServerCompleted id " + id + " len " + length + " sequence " + sequence + " address " + socketAddress + " serverIdCRC " + transmissionManager.serverIdCRC);
    }

    @Override
    public boolean close() {
        return close(CLOSED_REASON_COMPLETED);
    }

    public boolean close(short reason) {
        PacketReceivingTransmission transmission = receivingTransmissionMap.get(id);
        boolean bool = false;
        if (!completed && transmission != null && transmission.equals(this)) {
            synchronized (this) {
                if(!completed && transmission != null && transmission.equals(this)) {
                    completed = true;
                    bool = true;
                }
            }
        }
        if(bool) {
            transmissionManager.getScheduledExecutorService().schedule(() -> {
                receivingTransmissionMap.remove(id, this);
//                LoggerEx.info(TAG, "receivingTransmissionMap remove id " + id + " after 10 seconds reason " + reason);
            }, 120000, TimeUnit.MILLISECONDS);
            if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "closed id " + id + " sequence " + sequence + " address " + socketAddress + " serverIdCRC " + transmissionManager.serverIdCRC + " reason " + reason + " id will be release after 120s");
            if(sequenceBytesCollector != null) {
                sequenceBytesCollector.clear();
            }
            sequencePacketSet.clear();
            return true;
        }
        return false;
    }

    public SequenceBytesCollector getSequenceBytesCollector() {
        return sequenceBytesCollector;
    }

    public void setSequenceBytesCollector(SequenceBytesCollector sequenceBytesCollector) {
        this.sequenceBytesCollector = sequenceBytesCollector;
    }
}
