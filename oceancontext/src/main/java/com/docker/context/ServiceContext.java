package com.docker.context;

import chat.errors.CoreException;
import com.docker.script.BaseRuntimeContext;

import java.util.Properties;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public class ServiceContext implements Context {
    private BaseRuntimeContext runtimeContext;

    public ServiceContext(BaseRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Override
    public Properties getConfig() {
        return this.runtimeContext.getConfiguration().getConfig();
    }

    @Override
    public <T> T getService(String service, Class<T> clazz) throws CoreException {
        return getService(null, service, clazz);
    }

    @Override
    public <T> T getService(String lanId, String service, Class<T> clazz) throws CoreException {
        return this.runtimeContext.getServiceStubManagerFactory().get(lanId).getService(service, clazz);
    }

    @Override
    public void injectBean(Object obj) throws CoreException {
        this.runtimeContext.getRuntimeBeanFactory().fillObject(obj);
    }
}
