package oceanus.sdk.rpc.impl;

import oceanus.sdk.rpc.RPCClientAdapter;

public interface DisconnectedAfterRetryListener {
    void disconected(RPCClientAdapter handler);
}
