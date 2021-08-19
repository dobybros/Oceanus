package oceanus.sdk.core.net.adapters.data;


import oceanus.sdk.core.net.NetworkCommunicator;

public class ErrorPacket extends Packet{
    private String message;
    private int code;

    private Integer waitSeconds;

    public ErrorPacket(int code, String message) {
        super(NetworkCommunicator.PACKET_TYPE_ERROR);
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Integer getWaitSeconds() {
        return waitSeconds;
    }

    public void setWaitSeconds(Integer waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public ErrorPacket waitSeconds(Integer waitSeconds) {
        this.waitSeconds = waitSeconds;
        return this;
    }
}
