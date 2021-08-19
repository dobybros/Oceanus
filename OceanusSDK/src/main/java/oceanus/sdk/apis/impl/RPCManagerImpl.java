package oceanus.sdk.apis.impl;

import oceanus.apis.CoreException;
import oceanus.apis.Oceanus;
import oceanus.apis.RPCManager;
import oceanus.sdk.rpc.remote.stub.ServiceStubManager;
import oceanus.sdk.server.OnlineServer;

public class RPCManagerImpl implements RPCManager {
    @Override
    public <R> R callOneServer(String service, String clazz, String method, String onlyCallOneServer, Class<R> returnClass, Object... args) throws CoreException {
        ServiceStubManager serviceStubManager = OnlineServer.getInstance().getServiceStubManagerFactory().get();
        return serviceStubManager.call(service, clazz, method, onlyCallOneServer, returnClass, args);
    }
    @Override
    public <R> R call(String service, String clazz, String method, Class<R> returnClass, Object... args) throws CoreException {
        ServiceStubManager serviceStubManager = OnlineServer.getInstance().getServiceStubManagerFactory().get();
        return serviceStubManager.call(service, clazz, method, null, returnClass, args);
    }

    @Override
    public <S> S getService(String service, Class<S> sClass) {
        ServiceStubManager serviceStubManager = OnlineServer.getInstance().getServiceStubManagerFactory().get();
        return serviceStubManager.getService(service, sClass);
    }
    @Override
    public <S> S getService(String service, Class<S> sClass, String onlyCallOneServer) {
        ServiceStubManager serviceStubManager = OnlineServer.getInstance().getServiceStubManagerFactory().get();
        return serviceStubManager.getService(service, sClass, onlyCallOneServer);
    }
}
