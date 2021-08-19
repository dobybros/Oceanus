package oceanus.sdk.core.net.data;


import oceanus.sdk.core.common.ErrorCodes;

public class ResponseTransport extends Transport {
    protected int code = ErrorCodes.OKAY;
    protected String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " code " + code + " message " + message;
    }
}
