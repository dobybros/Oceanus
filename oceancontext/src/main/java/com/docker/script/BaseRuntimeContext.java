package com.docker.script;

import chat.config.Configuration;
import chat.errors.CoreException;
import com.docker.rpc.remote.stub.ServiceStubManagerFactory;
import com.docker.server.remote.RuntimeServiceStubManagerFactory;
import lombok.Getter;
import script.core.runtime.AbstractRuntimeContext;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
@Getter
public class BaseRuntimeContext extends AbstractRuntimeContext {
    public BaseRuntimeContext(Configuration configuration) throws CoreException {
        super(configuration);
        this.serviceStubManagerFactory = new RuntimeServiceStubManagerFactory(this);
    }
    private ServiceStubManagerFactory serviceStubManagerFactory;
}
