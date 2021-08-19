package oceanus.sdk.rpc.remote.skeleton;

import com.alibaba.fastjson.JSON;
import oceanus.sdk.errors.ChatErrorCodes;
import oceanus.apis.CoreException;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.rpc.MethodRequest;
import oceanus.sdk.rpc.MethodResponse;
import oceanus.sdk.rpc.remote.MethodMapping;
import oceanus.sdk.rpc.remote.RpcServerInterceptor;
import oceanus.sdk.rpc.remote.annotations.RemoteService;
import oceanus.sdk.rpc.remote.stub.RpcCacheManager;
import oceanus.sdk.server.OnlineServer;
import oceanus.sdk.utils.ObjectId;
import oceanus.sdk.utils.OceanusProperties;
import oceanus.sdk.utils.ReflectionUtil;
import oceanus.sdk.utils.Tracker;
import oceanus.sdk.utils.annotation.ClassAnnotationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceSkeletonAnnotationHandler extends ClassAnnotationHandler {
    private static final String TAG = ServiceSkeletonAnnotationHandler.class.getSimpleName();
    private ConcurrentHashMap<Long, SkelectonMethodMapping> methodMap = new ConcurrentHashMap<>();

    private Integer serviceVersion;
    private String service;
//    private List<Class<? extends Annotation>> extraAnnotations;
//    private List<ServiceAnnotation> annotationList = new ArrayList<>();


    public ServiceSkeletonAnnotationHandler() {
//        extraAnnotations = new ArrayList<>();
        service = OceanusProperties.getInstance().getService();
    }

    @Override
    public void handle() {
        Set<Class<?>> remoteServiceClasses = reflections.getTypesAnnotatedWith(RemoteService.class, true);
        if(remoteServiceClasses != null && !remoteServiceClasses.isEmpty()) {
            StringBuilder uriLogs = new StringBuilder(
                    "\r\n-------------ServiceSkeletonAnnotationHandler------------\r\n");

            ConcurrentHashMap<Long, SkelectonMethodMapping> newMethodMap = new ConcurrentHashMap<>();
            for(Class<?> remoteServiceClass : remoteServiceClasses) {
                scanClass(remoteServiceClass, OnlineServer.getInstance().getOrCreateObject(remoteServiceClass), newMethodMap, uriLogs);
            }
            this.methodMap = newMethodMap;
            uriLogs.append("---------------------------------------\r\n");
            LoggerEx.info(TAG, uriLogs.toString());
        }
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Integer getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(Integer serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public class SkelectonMethodMapping extends MethodMapping {
        private Object remoteService;
        private List<RpcServerInterceptor> rpcServerInterceptors;

        public SkelectonMethodMapping(Method method) {
            super(method);
        }

        private Object[] prepareMethodArgs(MethodRequest request) throws CoreException {
            Object[] rawArgs = request.getArgs();
            if (method == null)
                throw new CoreException(ChatErrorCodes.ERROR_METHODMAPPING_METHOD_NULL, "Invoke method is null");
            int argLength = rawArgs != null ? rawArgs.length : 0;
            Object[] args = null;
            if (parameterTypes.length == argLength) {
                args = rawArgs;
            } else if (parameterTypes.length < argLength) {
                args = new Object[parameterTypes.length];
                System.arraycopy(rawArgs, 0, args, 0, parameterTypes.length);
            } else {
                args = new Object[parameterTypes.length];
                if (rawArgs != null)
                    System.arraycopy(rawArgs, 0, args, 0, rawArgs.length);
            }
            return args;
        }

        public MethodResponse invoke(MethodRequest request) throws CoreException {
            Object[] args = prepareMethodArgs(request);
            Long crc = request.getCrc();
            Object returnObj = null;
            CoreException exception = null;
            String parentTrackId = request.getTrackId();
            String currentTrackId = null;
            if (parentTrackId != null) {
                currentTrackId = ObjectId.get().toString();
                Tracker tracker = new Tracker(currentTrackId, parentTrackId);
                Tracker.trackerThreadLocal.set(tracker);
            }
            StringBuilder builder = new StringBuilder();
            boolean error = false;
            long time = System.currentTimeMillis();
            try {
                builder.append("$$methodrequest:: " + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + " $$service:: " + service + " $$serviceversion:: " + serviceVersion + " $$parenttrackid:: " + parentTrackId + " $$currenttrackid:: " + currentTrackId + " $$args:: " + request.getArgsTmpStr());
                returnObj = method.invoke(remoteService, args);
//                returnObj = remoteService.invokeRootMethod(method.getName(), args);
            } catch (Throwable t) {
                error = true;
                builder.append(" $$error" +
                        ":: " + t.getClass() + " $$errormsg:: " + t.getMessage());
//                if (t instanceof InvokerInvocationException) {
//                    Throwable theT = ((InvokerInvocationException) t).getCause();
//                    if (theT != null) {
//                        t = theT;
//                    }
//                }
                if (t instanceof CoreException) {
                    exception = (CoreException) t;
                } else {
                    exception = new CoreException(ChatErrorCodes.ERROR_METHODMAPPING_INVOKE_UNKNOWNERROR, t.getMessage());
                }
                exception.log(TAG, "invoke MethodRequest " + request.toString() + " error, " + t.getMessage());
            } finally {
                String ip = OnlineServer.getInstance().getIp();
                Tracker.trackerThreadLocal.remove();
                long invokeTokes = System.currentTimeMillis() - time;
                builder.append(" $$takes:: " + invokeTokes);
                builder.append(" $$sdockerip:: " + ip);
            }
            MethodResponse response = new MethodResponse(returnObj, exception);
            response.setRequest(request);
            response.setEncode(MethodResponse.ENCODE_JAVABINARY);
            response.setCrc(crc);
            if (returnObj != null)
                response.setReturnTmpStr(JSON.toJSONString(returnObj));
//            builder.append(" $$returnobj:: " + response.getReturnTmpStr());
//            if (error)
//                AnalyticsLogger.error(TAG, builder.toString());
//            else
//                AnalyticsLogger.info(TAG, builder.toString());
            return response;
        }

        public Object getRemoteService() {
            return remoteService;
        }

        public void setRemoteService(Object remoteService) {
            this.remoteService = remoteService;
        }

        public List<RpcServerInterceptor> getRpcServerInterceptors() {
            return rpcServerInterceptors;
        }

        public void setRpcServerInterceptors(List<RpcServerInterceptor> rpcServerInterceptors) {
            this.rpcServerInterceptors = rpcServerInterceptors;
        }
    }

    public SkelectonMethodMapping getMethodMapping(Long crc) {
        return methodMap.get(crc);
    }

    public void scanClass(Class<?> clazz, Object serverAdapter, ConcurrentHashMap<Long, SkelectonMethodMapping> methodMap, StringBuilder uriLogs) {
        if (clazz == null)
            return;
        RemoteService remoteService = clazz.getAnnotation(RemoteService.class);
        Method[] methods = ReflectionUtil.getMethods(clazz);
        for (Method method : methods) {
            if (method.isSynthetic() || method.getModifiers() == Modifier.PRIVATE)
                continue;
            SkelectonMethodMapping mm = new SkelectonMethodMapping(method);
            mm.setRemoteService(serverAdapter);
            long value = ReflectionUtil.getCrc(method, service);
            if (methodMap.contains(value)) {
                LoggerEx.warn(TAG, "Don't support override methods, please rename your method " + method + " for crc " + value + " and existing method " + methodMap.get(value).getMethod());
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Type[] genericParamterTypes = method.getGenericParameterTypes();
            if (parameterTypes != null) {
                boolean failed = false;
                for (int i = 0; i < parameterTypes.length; i++) {
                    parameterTypes[i] = ReflectionUtil.getInitiatableClass(parameterTypes[i]);
                    Class<?> parameterType = parameterTypes[i];
                    if (!ReflectionUtil.canBeInitiated(parameterType)) {
                        failed = true;
                        LoggerEx.warn(TAG, "Parameter " + parameterType + " in method " + method + " couldn't be initialized. ");
                        break;
                    }
                }
                if (failed)
                    continue;
            }
            mm.setParameterTypes(parameterTypes);
            mm.setGenericParameterTypes(genericParamterTypes);
            Class<?> returnType = method.getReturnType();
            returnType = ReflectionUtil.getInitiatableClass(returnType);
            mm.setReturnClass(returnType);
            mm.setAsync(false);
            methodMap.put(value, mm);
            RpcCacheManager.getInstance().putCrcMethodMap(value, service + "_" + clazz.getSimpleName() + "_" + method.getName());

            uriLogs.append("Mapping crc " + value + " for class " + clazz.getName() + " method " + method.getName() + " for service " + service + "\r\n");
        }
    }

    public ConcurrentHashMap<Long, SkelectonMethodMapping> getMethodMap() {
        return methodMap;
    }

}
