package oceanus.sdk.core.net.rudpex.impl;

import oceanus.sdk.core.net.rudpex.communicator.RUDPEXNetworkCommunicator;
import oceanus.sdk.logger.LoggerEx;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public abstract class PacketTransmission {
    public static final byte TYPE_UNRELIABLE_PING = 111;
    public static final byte TYPE_UNRELIABLE_PACKET = 100;

    static final byte TYPE_CLIENT_PACKET = 1;
    static final byte TYPE_CLIENT_PING = 2;
    static final byte TYPE_CLIENT_PACKET_PARTIAL_COMPLETED = 6;
    static final byte TYPE_CLIENT_PACKET_COMPLETED = 10;
    static final byte TYPE_CLIENT_CLOSED = 20;

    static final byte TYPE_SERVER_PONG = 100;
    static final byte TYPE_SERVER_GATHER_PACKET = 105;
    static final byte TYPE_SERVER_COMPLETED = 115;

    public static final short CLOSED_REASON_TIMEOUT = 1;
    public static final short CLOSED_REASON_IO_ERROR = 5;
    public static final short CLOSED_REASON_ACTIVE = 10;
    public static final short CLOSED_REASON_COMPLETED = 100;

    public static final short MAX_PACKETS_BUFFER = 10;

    public static final byte COMPLETE_STATUS_SENDING = 1;
    public static final byte COMPLETE_STATUS_PARTIAL_COMPLETED = 2;
    public static final byte COMPLETE_STATUS_COMPLETED = 4;
    private static final String TAG = PacketTransmission.class.getSimpleName();

    protected InetSocketAddress socketAddress;
    protected int id;
    protected int sequence;
    protected PacketTransmissionManager transmissionManager;

    // 发送数据，若出现io错误，重试3次
    protected void sendPacketWithRetries(int currentSequence, int length, DatagramPacket packet) throws IOException {
        Throwable exception = null;
        int times = PacketTransmissionManager.RETRY_TIMES; //retry times when IO error occurred.
        for(int i = 0; i < times; i++) {
            try {
                transmissionManager.datagramSocket.send(packet);
                if(exception != null) {
                    if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.info(TAG, "Send packet " + id + " at sequence " + currentSequence + " length " + length + " address " + socketAddress + " successfully after retries, IOException was " + exception.getMessage() + " have retried to i " + i);
                    exception = null;
                }
                break;
            } catch (IOException e) {
                e.printStackTrace();
                exception = e;
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send packet " + id + " at sequence " + currentSequence + " length " + length + " address " + socketAddress + " failed, IOException " + e.getMessage() + " will continue retry from i " + i + " to " + times);
            } catch (Throwable t) {
                t.printStackTrace();
                exception = t;
                if(RUDPEXNetworkCommunicator.LOG_ENABLED) LoggerEx.error(TAG, "Send packet " + id + " at sequence " + currentSequence + " length " + length + " address " + socketAddress + " failed, Throwable " + t.getMessage() + " will NOT retry from i " + i);
                break;
            }
        }

        if(exception != null) {
            if(exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new IOException("Unexpected unknown error while send packet id " + id + " at sequence " + currentSequence + " length " + length + " address " + socketAddress, exception);
            }
        }
    }

    public abstract boolean close();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
}
