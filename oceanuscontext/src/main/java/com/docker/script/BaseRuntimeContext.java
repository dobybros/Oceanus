package com.docker.script;

import chat.config.Configuration;
import com.docker.context.config.ServerConfig;
import com.docker.server.remote.RuntimeServiceStubManagerFactory;
import oceanus.apis.CoreException;
import oceanus.sdk.rpc.remote.stub.ServiceStubManagerFactory;
import script.core.runtime.AbstractRuntimeContext;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public class BaseRuntimeContext extends AbstractRuntimeContext {
    public BaseRuntimeContext(Configuration configuration) throws CoreException {
        super(configuration);
        this.serviceStubManagerFactory = new RuntimeServiceStubManagerFactory(this);
        this.serverConfig = new ServerConfig(getConfiguration());
    }
    private ServiceStubManagerFactory serviceStubManagerFactory;

    private ServerConfig serverConfig;
    public ServiceStubManagerFactory getServiceStubManagerFactory() {
        return serviceStubManagerFactory;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }
}
