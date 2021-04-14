package core.consensus.model;

import core.consensus.model.data.ClusterActionRequest;
import core.net.data.ResponseTransport;

public interface NodeStorageListener<ActionRequest extends ClusterActionRequest> {
    ResponseTransport dataReceived(ActionRequest actionRequest);
}
