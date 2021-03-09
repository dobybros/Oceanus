package com.docker.rpc;

import chat.errors.CoreException;
import com.docker.rpc.RPCRequest;
import com.docker.rpc.RPCResponse;

public abstract class RPCServerAdapter<Request extends RPCRequest, Response extends RPCResponse> {
	public abstract Response onCall(Request request) throws CoreException;
	public Object oncallAsync(Request request, String callbackFutureId) throws CoreException {
		return null;
	}
}
