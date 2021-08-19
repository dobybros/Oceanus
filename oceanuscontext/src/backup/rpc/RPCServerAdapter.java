package com.docker.rpc;

public abstract class RPCServerAdapter<Request extends RPCRequest, Response extends RPCResponse> {
	public abstract Response onCall(Request request) throws CoreException;
	public Object oncallAsync(Request request, String callbackFutureId) throws CoreException {
		return null;
	}
}
