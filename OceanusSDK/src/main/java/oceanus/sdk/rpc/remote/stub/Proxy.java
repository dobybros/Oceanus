package oceanus.sdk.rpc.remote.stub;

import oceanus.sdk.errors.ChatErrorCodes;
import oceanus.apis.CoreException;
import oceanus.sdk.rpc.MethodRequest;
import oceanus.sdk.rpc.MethodResponse;
import oceanus.sdk.rpc.remote.MethodMapping;
import oceanus.sdk.utils.Tracker;

public class Proxy {
    private ServiceStubManager serviceStubManager;
    private RemoteInvocationHandler invocationHandler;
    protected RemoteServerHandler remoteServerHandler;
    protected String onlyCallOneServer;

    public Proxy(ServiceStubManager serviceStubManager, RemoteServerHandler remoteServerHandler) {
        this.serviceStubManager = serviceStubManager;
        this.remoteServerHandler = remoteServerHandler;
        invocationHandler = new RemoteInvocationHandlerImpl(this.remoteServerHandler);
    }

    //远程service调用
    public Object invoke(Long crc, Object[] args) throws Throwable {
        // TODO Auto-generated method stub
        MethodRequest request = new MethodRequest();
        request.setEncode(MethodRequest.ENCODE_JAVABINARY);
        request.setArgs(args);
        //TODO should consider how to optimize get CRC too often.
        Tracker tracker = Tracker.trackerThreadLocal.get();
        request.setTrackId(tracker == null ? null : tracker.getTrackId());
        request.setCrc(crc);
        request.setServiceStubManager(serviceStubManager);
        request.setFromService(serviceStubManager.getFromService());
        MethodMapping methodMapping = serviceStubManager.getMethodMapping(crc);
        if (methodMapping != null) {
            return invocationHandler.invoke(methodMapping, request);
        } else {
            MethodResponse response = new MethodResponse();
            response.setException(new CoreException(ChatErrorCodes.ERROR_METHODREQUEST_METHODNOTFOUND, "Method doesn't be found by service_class_method " + RpcCacheManager.getInstance().getMethodByCrc(crc) + ",crc: " + crc));
            return response;
        }
    }

    public static Object getReturnObject(MethodRequest request, MethodResponse response) throws CoreException {
        if (response != null) {
            CoreException e = response.getException();
            if (e != null) {
                throw e;
            }
            Object returnObject = response.getReturnObject();
            return returnObject;
        }
        throw new CoreException(ChatErrorCodes.ERROR_METHODRESPONSE_NULL, "Method response is null for request " + request);
    }
}