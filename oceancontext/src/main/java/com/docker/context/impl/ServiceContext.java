package com.docker.context.impl;

import chat.errors.CoreException;
import com.docker.context.Context;
import com.docker.context.config.ServerConfig;
import com.docker.script.BaseRuntimeContext;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

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
    public ServerConfig getServerConfig() {
        return this.runtimeContext.getServerConfig();
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
    public Object call(String service, String className, String method, Object... args) throws CoreException {
        return this.call(null, service, className, method, args);
    }

    @Override
    public CompletableFuture<?> callAsync(String service, String className, String method, Object... args) throws CoreException {
        return this.callAsync(null, service, className, method, args);
    }

    @Override
    public Object call(String lanId, String service, String className, String method, Object... args) throws CoreException {
        return this.runtimeContext.getServiceStubManagerFactory().get(lanId).call(service, className, method, args);
    }

    @Override
    public CompletableFuture<?> callAsync(String lanId, String service, String className, String method, Object... args) throws CoreException {
        return this.runtimeContext.getServiceStubManagerFactory().get(lanId).callAsync(service, className, method, args);
    }

    @Override
    public void injectBean(Object obj) throws CoreException {
        this.runtimeContext.getRuntimeBeanFactory().fillObject(obj);
    }

    @Override
    public Collection<Class<?>> getClasses() {
        return this.runtimeContext.getAllClasses().values();
    }
}
