package com.docker.rpc;

import chat.config.BaseConfiguration;
import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import com.docker.rpc.remote.skeleton.ServiceSkeletonAnnotationHandler;
import com.docker.script.BaseRuntimeContext;
import com.docker.oceansbean.BeanFactory;

/**
 * Created by aplomb on 17-5-16.
 */
public class RPCServerMethodInvocation extends RPCServerAdapter<MethodRequest, MethodResponse> {

    private static final String TAG = RPCServerMethodInvocation.class.getSimpleName();
    protected BaseConfiguration baseConfiguration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());

    @Override
    public MethodResponse onCall(MethodRequest request) throws CoreException {
        ServiceSkeletonAnnotationHandler.SkelectonMethodMapping methodMapping = getMethodMapping(request);
        MethodResponse response = methodMapping.invoke(request);
        return response;
    }

    @Override
    public Object oncallAsync(MethodRequest request,  String callbackFutureId) throws CoreException {
        ServiceSkeletonAnnotationHandler.SkelectonMethodMapping methodMapping = getMethodMapping(request);
        return methodMapping.invokeAsync(request, callbackFutureId);
    }

    public ServiceSkeletonAnnotationHandler.SkelectonMethodMapping getMethodMapping(MethodRequest request) throws CoreException{
        Long crc = request.getCrc();
//        if(crc == 0 || crc == -1)
//            throw new CoreException(CoreErrorCodes.ERROR_METHODREQUEST_CRC_ILLEGAL, "CRC is illegal for MethodRequest");
        String service = request.getService();
//        if(service == null)
//            throw new CoreException(ChatErrorCodes.ERROR_METHODREQUEST_SERVICE_NULL, "Service is null for service_class_method " + ServerCacheManager.getInstance().getCrcMethodMap().get(crc));

        BaseRuntimeContext runtimeContext = (BaseRuntimeContext) baseConfiguration.getRuntimeContext(service);
        if(runtimeContext == null)
            throw new CoreException(ChatErrorCodes.ERROR_METHODREQUEST_SERVICE_NOTFOUND, "Service " + service + " not found for service_class_method ");
        ServiceSkeletonAnnotationHandler serviceSkeletonAnnotationHandler = (ServiceSkeletonAnnotationHandler) runtimeContext.getClassAnnotationHandler(ServiceSkeletonAnnotationHandler.class);
        if(serviceSkeletonAnnotationHandler == null)
            throw new CoreException(ChatErrorCodes.ERROR_METHODREQUEST_SKELETON_NULL, "Skeleton handler is not for service " + service + " on service_class_method ");
        ServiceSkeletonAnnotationHandler.SkelectonMethodMapping methodMapping = serviceSkeletonAnnotationHandler.getMethodMapping(crc);
        if(methodMapping == null)
            throw new CoreException(ChatErrorCodes.ERROR_METHODREQUEST_METHODNOTFOUND, "Method doesn't be found by service_class_method ");
        return methodMapping;
    }
}
