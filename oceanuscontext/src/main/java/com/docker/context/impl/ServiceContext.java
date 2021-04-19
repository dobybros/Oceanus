package com.docker.context.impl;

import chat.errors.CoreException;
import com.docker.context.Context;
import com.docker.context.RPCCaller;
import com.docker.context.ServiceGenerator;
import com.docker.context.config.ServerConfig;
import com.docker.oceansbean.BeanFactory;
import com.docker.rpc.remote.stub.RemoteServersManager;
import com.docker.script.BaseRuntimeContext;
import com.docker.storage.adapters.DockerStatusService;
import com.docker.storage.adapters.impl.DockerStatusServiceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public class ServiceContext implements Context {
    public static final String LAN_ID_DEFAULT = "default";

    private BaseRuntimeContext runtimeContext;

    private ConcurrentHashMap<String, RPCCaller> lanRpcCallCacheMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServiceGenerator> lanServiceGeneratorCacheMap = new ConcurrentHashMap<>();
//    private DockerStatusService dockerStatusService = (DockerStatusService) BeanFactory.getBean(DockerStatusServiceImpl.class.getName());

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
    public RPCCaller getRPCCaller() {
        return getRPCCaller(null);
    }

    @Override
    public RPCCaller getRPCCaller(String lanId) {
        if(lanId == null) {
            lanId = LAN_ID_DEFAULT;
        }
        RPCCaller rpcCaller = lanRpcCallCacheMap.get(lanId);
        if(rpcCaller == null) {
            rpcCaller = new RPCCaller(lanId) {
                @Override
                public Object call(String service, String className, String method, Object... args) throws CoreException {
                    return call(service, className, method, null, args);
                }

                @Override
                public Object call(String service, String className, String method, String onlyCallOneServer, Object... args) throws CoreException {
                    return runtimeContext.getServiceStubManagerFactory().get(this.lanId).call(service, className, method, onlyCallOneServer, args);
                }

                @Override
                public CompletableFuture<?> callAsync(String service, String className, String method, Object... args) throws CoreException {
                    return callAsync(service, className, method, args);
                }

                @Override
                public CompletableFuture<?> callAsync(String service, String className, String method, String onlyCallOneServer, Object... args) throws CoreException {
                    return runtimeContext.getServiceStubManagerFactory().get(this.lanId).callAsync(service, className, method, onlyCallOneServer, args);
                }
            };
            lanRpcCallCacheMap.putIfAbsent(lanId, rpcCaller);
        }
        return lanRpcCallCacheMap.get(lanId);
    }

    public ServiceGenerator getServiceGenerator() {
        return getServiceGenerator(null);
    }

    public ServiceGenerator getServiceGenerator(String lanId) {
        if(lanId == null) {
            lanId = LAN_ID_DEFAULT;
        }
        ServiceGenerator serviceGenerator = lanServiceGeneratorCacheMap.get(lanId);
        if(serviceGenerator == null) {
            serviceGenerator = new ServiceGenerator(lanId) {
                @Override
                public <T> T getService(String service, Class<T> clazz) throws CoreException {
                    return getService(service, clazz, null);
                }

                @Override
                public <T> T getService(String service, Class<T> clazz, String onlyCallOneServer) throws CoreException {
                    return runtimeContext.getServiceStubManagerFactory().get(this.lanId).getService(service, clazz, onlyCallOneServer);
                }

                @Override
                public Object call(String service, String className, String method, Object... args) throws CoreException {
                    return callOneServer(service, className, method, null, args);
                }

                @Override
                public Object callOneServer(String service, String className, String method, String onlyCallOneServer, Object... args) throws CoreException {
                    return runtimeContext.getServiceStubManagerFactory().get(this.lanId).call(service, className, method, onlyCallOneServer, args);
                }
            };
            lanServiceGeneratorCacheMap.putIfAbsent(lanId, serviceGenerator);
        }
        return lanServiceGeneratorCacheMap.get(lanId);
    }

    @Override
    public Object getAndCreateBean(Class<?> clazz) {
        try {
            return this.runtimeContext.getRuntimeBeanFactory().get(null, runtimeContext.getRuntime().path(clazz));
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object getAndCreateBean(String beanName, Class<?> clazz) {
        try {
            return this.runtimeContext.getRuntimeBeanFactory().get(beanName, runtimeContext.getRuntime().path(clazz));
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void injectBean(Object obj) throws CoreException {
        this.runtimeContext.getRuntimeBeanFactory().fillObject(obj);
    }

    @Override
    public Collection<Class<?>> getClasses() {
        return this.runtimeContext.getAllClasses().values();
    }

    @Override
    public List<String> getServersByService(String service) throws CoreException {
//        return dockerStatusService.getServersByService(service);
        RemoteServersManager remoteServersManager = RemoteServersManager.getInstance();
        RemoteServersManager.ServiceNodesMonitor serviceNodesMonitor = RemoteServersManager.getInstance().getServers(service);
        if(serviceNodesMonitor == null) {
            remoteServersManager.initService(service);
            serviceNodesMonitor = RemoteServersManager.getInstance().getServers(service);
        }
        if(serviceNodesMonitor != null) {
            List<Long> serverCRCIds = serviceNodesMonitor.getNodeServerCRCIds();
            if(serverCRCIds != null) {
                List<String> servers = new ArrayList<>();
                for(Long serverCRCId : serverCRCIds) {
                    servers.add(String.valueOf(serverCRCId));
                }
                return servers;
            }
        }
        return null;
    }
}
