package oceanus.sdk.rpc.impl;

import oceanus.sdk.rpc.RPCRequest;
import oceanus.sdk.rpc.RPCResponse;

public class RPCEntity {
    public Class<? extends RPCRequest> requestClass;
    public Class<? extends RPCResponse> responseClass;
}