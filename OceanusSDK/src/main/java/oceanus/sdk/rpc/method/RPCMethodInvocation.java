package oceanus.sdk.rpc.method;

import oceanus.apis.CoreException;
import oceanus.sdk.rpc.MethodRequest;
import oceanus.sdk.rpc.MethodResponse;
import oceanus.sdk.rpc.interceptor.MethodInterceptor;
import oceanus.sdk.rpc.interceptor.MethodInvocation;
import oceanus.sdk.rpc.remote.MethodMapping;
import oceanus.sdk.rpc.remote.stub.Proxy;
import oceanus.sdk.rpc.remote.stub.RemoteServerHandler;

import java.util.List;

public class RPCMethodInvocation extends MethodInvocation {
    private MethodRequest methodRequest;
    private MethodMapping methodMapping;
    private RemoteServerHandler remoteServerHandler;
    private Boolean isAsync;

    public RPCMethodInvocation(MethodRequest methodRequest, MethodMapping methodMapping, List<MethodInterceptor> methodInterceptors, RemoteServerHandler remoteServerHandler, String methodKey) {
        super(null, methodMapping.getMethod().getDeclaringClass(), methodMapping.getMethod(), methodRequest.getArgs(), methodInterceptors, methodKey);
        this.methodRequest = methodRequest;
        this.methodMapping = methodMapping;
        this.isAsync = methodMapping.getAsync();
        this.remoteServerHandler = remoteServerHandler;
    }

    @Override
    public Object invoke() throws CoreException {
        return this.handleSync();
    }


    public Object handleSync() throws CoreException {
        MethodResponse response = remoteServerHandler.call(methodRequest);
        return Proxy.getReturnObject(methodRequest, response);
    }

    public MethodRequest getMethodRequest() {
        return methodRequest;
    }

    public MethodMapping getMethodMapping() {
        return methodMapping;
    }

    public RemoteServerHandler getRemoteServerHandler() {
        return remoteServerHandler;
    }

    public int getCurrentInterceptorIndex() {
        return currentInterceptorIndex;
    }

    public Boolean getAsync() {
        return isAsync;
    }
}
