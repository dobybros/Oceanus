package oceanus.sdk.core.discovery.data;

public class TimeoutResponse extends FailedResponse {
    public TimeoutResponse(int code) {
        super(code);
    }

    public TimeoutResponse(int code, String message) {
        super(code, message);
    }
}
