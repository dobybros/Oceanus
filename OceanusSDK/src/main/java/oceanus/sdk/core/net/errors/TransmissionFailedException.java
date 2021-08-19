package oceanus.sdk.core.net.errors;

import java.io.IOException;

public class TransmissionFailedException extends IOException {
    private int reason;
    public TransmissionFailedException(int reason) {
        this(reason, "Reason is " + reason);
    }
    public TransmissionFailedException(int reason, String message) {
        super(message);
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }
}
