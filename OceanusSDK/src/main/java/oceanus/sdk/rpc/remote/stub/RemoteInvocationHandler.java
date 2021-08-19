package oceanus.sdk.rpc.remote.stub;

import oceanus.apis.CoreException;
import oceanus.sdk.rpc.MethodRequest;
import oceanus.sdk.rpc.remote.MethodMapping;

interface RemoteInvocationHandler {

    public Object invoke(MethodMapping methodMapping, MethodRequest methodRequest) throws CoreException;
}