package com.container.runtime.boot.thirdenv;

import com.docker.context.RemoteContext;
import com.docker.server.remote.RuntimeServiceStubManagerFactory;
import oceanus.apis.CoreException;
import oceanus.sdk.rpc.remote.stub.ServiceStubManagerFactory;

/**
 * Created by lick on 2021/1/6.
 * Description：
 */
public class DefaultRemoteContext implements RemoteContext {
    private ServiceStubManagerFactory serviceStubManagerFactory = new RuntimeServiceStubManagerFactory();
    @Override
    public <T> T getService(String service, Class<T> clazz) throws CoreException {
        return getService(null, service, clazz);
    }

    @Override
    public <T> T getService(String lanId, String service, Class<T> clazz) throws CoreException {
        return serviceStubManagerFactory.get(lanId).getService(service, clazz);
    }
}
