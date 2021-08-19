package oceanus.sdk.rpc.remote;

import oceanus.apis.CoreException;
import oceanus.sdk.rpc.RPCRequest;
import oceanus.sdk.rpc.impl.RMIServer;

public interface RpcServerInterceptor {
    public Object invoke(RPCRequest rpcRequest, RMIServer rmiServer) throws CoreException;

    public Object afterInvoke();
}
