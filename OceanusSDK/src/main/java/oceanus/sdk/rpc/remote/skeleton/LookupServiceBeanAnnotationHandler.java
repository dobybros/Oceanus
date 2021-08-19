package oceanus.sdk.rpc.remote.skeleton;

import oceanus.apis.CoreException;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.rpc.remote.annotations.LookupServiceBean;
import oceanus.sdk.rpc.remote.annotations.RemoteService;
import oceanus.sdk.rpc.remote.annotations.ServiceBean;
import oceanus.sdk.rpc.remote.stub.ServiceStubManager;
import oceanus.sdk.server.OnlineServer;
import oceanus.sdk.utils.ReflectionUtil;
import oceanus.sdk.utils.annotation.ClassAnnotationHandler;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LookupServiceBeanAnnotationHandler extends ClassAnnotationHandler {
    private static final String TAG = LookupServiceBeanAnnotationHandler.class.getSimpleName();

    @Override
    public void handle() throws CoreException {
        Set<Class<?>> remoteServiceClasses = reflections.getTypesAnnotatedWith(LookupServiceBean.class, true);
        if(remoteServiceClasses != null && !remoteServiceClasses.isEmpty()) {
            StringBuilder uriLogs = new StringBuilder(
                    "\r\n--------------LookupServiceBeanAnnotationHandler-------------\r\n");

            ConcurrentHashMap<Long, ServiceSkeletonAnnotationHandler.SkelectonMethodMapping> newMethodMap = new ConcurrentHashMap<>();
            for(Class<?> remoteServiceClass : remoteServiceClasses) {
                Object bean = OnlineServer.getInstance().getOrCreateObject(remoteServiceClass);
                Field[] fields = ReflectionUtil.getFields(remoteServiceClass);
                for(Field field : fields) {
                    ServiceBean serviceBean = field.getAnnotation(ServiceBean.class);
                    if(serviceBean != null) {
                        field.setAccessible(true);
                        try {
                            Object value = field.get(bean);
                            if(value == null) {
                                ServiceStubManager serviceStubManager = OnlineServer.getInstance().getServiceStubManagerFactory().get();
                                value = serviceStubManager.getService(serviceBean.name(), field.getType());
                                field.set(bean, value);
                                uriLogs.append("Assigned " + field.getName() + " in class " + field.getDeclaringClass() + " with " + value);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            LoggerEx.error(TAG, "Assign ServiceBean " + field.getName() + " in class " + field.getDeclaringClass() + " failed, " + e.getMessage());
                        }
                    }
                }
            }

            uriLogs.append("---------------------------------------\r\n");
            LoggerEx.info(TAG, uriLogs.toString());
        }
    }
}
