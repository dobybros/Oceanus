package oceanus.sdk.rpc.remote.stub;

import oceanus.apis.CoreException;
import oceanus.sdk.rpc.MethodRequest;
import oceanus.sdk.rpc.interceptor.MethodInterceptor;
import oceanus.sdk.rpc.interceptor.MethodInvocation;
import oceanus.sdk.rpc.method.RPCMethodInvocation;
import oceanus.sdk.rpc.remote.MethodMapping;

import java.util.List;
import java.util.Map;

public class RemoteInvocationHandlerImpl implements RemoteInvocationHandler {
    private final String TAG = RemoteInvocationHandlerImpl.class.getSimpleName();
    private RemoteServerHandler remoteServerHandler;

    protected RemoteInvocationHandlerImpl(RemoteServerHandler remoteServerHandler) {
        this.remoteServerHandler = remoteServerHandler;
    }

    @Override
    public Object invoke(MethodMapping methodMapping, MethodRequest methodRequest) throws CoreException {
        String methodKey = String.valueOf(methodRequest.getCrc());
        List<MethodInterceptor> methodInterceptors = null;
        if (methodRequest.getFromService() != null) {
            Map<String, List<MethodInterceptor>> methodInterceptorMap = RPCInterceptorFactory.getInstance().getAllMethodInterceptorMap().get(methodRequest.getFromService());
            if (methodInterceptorMap != null) {
                methodInterceptors = methodInterceptorMap.get(methodKey);
            }
        }
//        handleAsyncWithHandler(methodMapping, methodRequest);
        MethodInvocation methodInvocation = null;
        if(methodRequest.getServiceStubManager() != null){
            methodInvocation = new RPCMethodInvocation(methodRequest, methodMapping, methodInterceptors, remoteServerHandler, methodKey);
//            if(methodRequest.getServiceStubManager().getLanType() == null || methodRequest.getServiceStubManager().getLanType().equals(Lan.TYPE_RPC)){
//                methodInvocation = new RPCMethodInvocation(methodRequest, methodMapping, methodInterceptors, remoteServerHandler, methodKey);
//            } else {
                //TODO Aplomb should not enter here. No longer use http for cross lan invocation.
//                methodInvocation = new HttpInvocation(methodRequest, methodMapping, methodInterceptors, remoteServerHandler, methodKey);
//            }
            return methodInvocation.proceed();
        }
        return null;
    }

}
