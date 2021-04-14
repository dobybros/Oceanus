package com.container.runtime.boot.thirdenv;

import chat.errors.CoreException;
import com.docker.context.RemoteContext;
import com.docker.rpc.remote.stub.ServiceStubManagerFactory;
import com.docker.server.remote.RuntimeServiceStubManagerFactory;

import java.rmi.Remote;

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
