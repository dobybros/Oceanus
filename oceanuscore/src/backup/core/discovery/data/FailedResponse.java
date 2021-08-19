package core.discovery.data;

import core.net.data.ResponseTransport;

public class FailedResponse extends ResponseTransport {
    public FailedResponse() {}
    public FailedResponse(int code) {
        this(code, null);
    }
    public FailedResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
