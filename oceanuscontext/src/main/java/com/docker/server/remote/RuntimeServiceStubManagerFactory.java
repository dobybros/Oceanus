package com.docker.server.remote;

import chat.errors.CoreException;
import com.docker.context.impl.ServiceContext;
import com.docker.errors.CoreErrorCodes;
import com.docker.script.BaseRuntimeContext;
import com.docker.rpc.remote.stub.ServiceStubManager;
import com.docker.rpc.remote.stub.ServiceStubManagerFactory;
import script.core.runtime.classloader.ClassHolder;
import script.core.runtime.classloader.MyGroovyClassLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public class RuntimeServiceStubManagerFactory implements ServiceStubManagerFactory {
    private final String TAG = RuntimeServiceStubManagerFactory.class.getSimpleName();
    private Map<String, ServiceStubManager> serviceStubManagerMap = new ConcurrentHashMap<>();
    private BaseRuntimeContext runtimeContext;
    private final String OUTSIDE_LOCAL_IDC_LANID = "outsideLocalIdcLan";

    public RuntimeServiceStubManagerFactory(BaseRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }
    public RuntimeServiceStubManagerFactory() {}

    @Override
    public ServiceStubManager get(String lanId) throws CoreException {
        String fromService = null;
        Class parseClass = null;
        if(runtimeContext != null){
            fromService = runtimeContext.getConfiguration().getService();
            if(runtimeContext.getCurrentClassLoader() instanceof MyGroovyClassLoader){
                ClassHolder classHolder = ((MyGroovyClassLoader)runtimeContext.getCurrentClassLoader()).getClass("script.core.runtime.ServiceStubProxy");
                if(classHolder == null){
                    throw new CoreException(CoreErrorCodes.ERROR_CANT_FIND_CLASS, "Cant find class script.core.runtime.ServiceStubProxy");
                }
                parseClass = classHolder.getParsedClass();
            }
        }
        lanId = ServiceContext.LAN_ID_DEFAULT;
        ServiceStubManager serviceStubManager = serviceStubManagerMap.get(lanId);
        if(serviceStubManager == null){
            synchronized (RuntimeServiceStubManagerFactory.class){
                serviceStubManager = serviceStubManagerMap.get(lanId);
                if(serviceStubManager == null){
                    serviceStubManager = new ServiceStubManager(fromService);
                    serviceStubManager.setServiceStubProxyClass(parseClass);
                    serviceStubManager.init();
                    serviceStubManagerMap.put(lanId, serviceStubManager);
                }
            }
        }
        return serviceStubManager;
    }

    @Override
    public ServiceStubManager get() throws CoreException {
        return get(null);
    }
}

