package com.docker.server.remote;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.context.impl.ServiceContext;
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
    private Map<String, ServiceStubManager> serviceStubManagerMap = new ConcurrentHashMap<>();
    private BaseRuntimeContext runtimeContext;
    private final String OUTSIDE_LOCAL_IDC_LANID = "outsideLocalIdcLan";

    public RuntimeServiceStubManagerFactory(BaseRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }
    public RuntimeServiceStubManagerFactory() {}

    @Override
    public ServiceStubManager get(String lanId) throws CoreException {
        String baseLanId = null;
        String fromService = null;
        Class parseClass = null;
        if(runtimeContext != null){
            baseLanId = runtimeContext.getConfiguration().getBaseConfiguration().getLanId();
            fromService = runtimeContext.getConfiguration().getService();
            if(runtimeContext.getCurrentClassLoader() instanceof MyGroovyClassLoader){
                ClassHolder classHolder = ((MyGroovyClassLoader)runtimeContext.getCurrentClassLoader()).getClass("script.core.runtime.ServiceStubProxy");
                if(classHolder == null){
                    throw new CoreException(CoreErrorCodes.ERROR_CANT_FIND_CLASS, "Cant find class script.core.runtime.ServiceStubProxy");
                }
                parseClass = classHolder.getParsedClass();
            }
        }
        if(StringUtils.isBlank(lanId) || lanId.equals(ServiceContext.LAN_ID_DEFAULT)){
            lanId = baseLanId;
            if(StringUtils.isBlank(lanId)){
                lanId = OUTSIDE_LOCAL_IDC_LANID;
            }
        }
        ServiceStubManager serviceStubManager = serviceStubManagerMap.get(lanId);
        if(serviceStubManager == null){
            synchronized (RuntimeServiceStubManagerFactory.class){
                serviceStubManager = serviceStubManagerMap.get(lanId);
                if(serviceStubManager == null){
                    if(lanId.equals(OUTSIDE_LOCAL_IDC_LANID) || lanId.equals(baseLanId)){
                        serviceStubManager = new ServiceStubManager(fromService);
                    } else {
                        Lan lan = null;
                        try {
                            LansService lansService = (LansService) BeanFactory.getBean(LansServiceImpl.class.getName());
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
                        serviceStubManager = new ServiceStubManager(host, fromService);
                        if (lan.getType() == null) {
                            serviceStubManager.setLanType(Lan.TYPE_http);
                        } else {
                            serviceStubManager.setLanType(Integer.valueOf(lan.getType()));
                        }
                        serviceStubManager.setUsePublicDomain(true);
                    }
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

