package com.docker.rpc.impl;

import com.docker.rpc.RPCClientAdapter;

public interface DisconnectedAfterRetryListener {
    void disconected(RPCClientAdapter handler);
}
