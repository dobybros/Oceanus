package com.docker.server.remote;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.data.Lan;
import com.docker.errors.CoreErrorCodes;
import com.docker.script.BaseRuntimeContext;
import com.docker.storage.adapters.LansService;
import com.docker.storage.adapters.impl.LansServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import com.docker.rpc.remote.stub.ServiceStubManager;
import com.docker.oceansbean.BeanFactory;
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
    private LansService lansService = (LansService) BeanFactory.getBean(LansServiceImpl.class.getName());
    private Map<String, ServiceStubManager> serviceStubManagerMap = new ConcurrentHashMap<>();
    private BaseRuntimeContext runtimeContext;

    public RuntimeServiceStubManagerFactory(BaseRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Override
    public ServiceStubManager get(String lanId) throws CoreException {
        if(StringUtils.isBlank(lanId)){
            lanId = runtimeContext.getConfiguration().getBaseConfiguration().getLanId();
        }
        ServiceStubManager serviceStubManager = serviceStubManagerMap.get(lanId);
        if(serviceStubManager == null){
            synchronized (RuntimeServiceStubManagerFactory.class){
                serviceStubManager = serviceStubManagerMap.get(lanId);
                if(serviceStubManager == null){
                    if(lanId.equals(runtimeContext.getConfiguration().getBaseConfiguration().getLanId())){
                        serviceStubManager = new ServiceStubManager(runtimeContext.getConfiguration().getService());
                    }else {
                        Lan lan = null;
                        try {
                            lan = lansService.getLan(lanId);
                        } catch (CoreException e) {
                            e.printStackTrace();
                            LoggerEx.error(TAG, "Read lan " + lanId + " information failed, " + ExceptionUtils.getFullStackTrace(e));
                        }
                        if (lan == null)
                            throw new CoreException(CoreErrorCodes.ERROR_LAN_FAILED, "Lan is null for lanId " + lanId);
                        if (lan.getDomain() == null || lan.getPort() == null || lan.getProtocol() == null)
                            throw new CoreException(CoreErrorCodes.ERROR_LAN_FAILED, "Lan " + lan + " is illegal for lanId " + lanId + " domain " + lan.getDomain() + " port " + lan.getPort() + " protocol " + lan.getProtocol());
                        String host = lan.getProtocol() + "://" + lan.getDomain() + ":" + lan.getPort();
                        serviceStubManager = new ServiceStubManager(host, runtimeContext.getConfiguration().getService());
                        if (lan.getType() == null) {
                            serviceStubManager.setLanType(Lan.TYPE_http);
                        } else {
                            serviceStubManager.setLanType(Integer.valueOf(lan.getType()));
                        }
                        serviceStubManager.setUsePublicDomain(true);
                    }
                    if(runtimeContext.getCurrentClassLoader() instanceof MyGroovyClassLoader){
                        ClassHolder classHolder = ((MyGroovyClassLoader)runtimeContext.getCurrentClassLoader()).getClass("script.core.runtime.ServiceStubProxy");
                        if(classHolder == null){
                            throw new CoreException(CoreErrorCodes.ERROR_CANT_FIND_CLASS, "Cant find class script.core.runtime.ServiceStubProxy");
                        }
                        serviceStubManager.setServiceStubProxyClass(classHolder.getParsedClass());
                    }
                    serviceStubManager.init();
                    serviceStubManagerMap.put(lanId, serviceStubManager);
                }
            }
        }
        return serviceStubManager;
    }
}

