package oceanus.sdk.rpc;

import oceanus.apis.CoreException;

public abstract class RPCServerAdapter<Request extends RPCRequest, Response extends RPCResponse> {
	public abstract Response onCall(Request request) throws CoreException;
}
