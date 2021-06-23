package script.core.servlets;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;
import script.core.servlet.annotation.RequestPermission;
import script.core.servlets.GroovyServletManager.PermissionIntercepter;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

public class RequestPermissionHandler extends ClassAnnotationHandler {

    private static final String TAG = RequestPermissionHandler.class.getSimpleName();

    public RequestPermissionHandler() {
    }

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return RequestPermission.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        if (annotatedClassMap != null && !annotatedClassMap.isEmpty()) {
            StringBuilder uriLogs = new StringBuilder("\r\n---------------------------------------\r\n");

            Set<String> keys = annotatedClassMap.keySet();
            GroovyObjectEx<PermissionIntercepter> intercepter = null;

            GroovyServletManager servletManager = null;
            servletManager = (GroovyServletManager) runtimeContext.getClassAnnotationHandler(GroovyServletManager.class);
            if (servletManager == null) {
                uriLogs.append("GroovyServletManager not found, ignore...");
                uriLogs.append("---------------------------------------");
                return;
            }
            for (String key : keys) {
                if (intercepter == null) {
                    Class<?> groovyClass = annotatedClassMap.get(key);
                    intercepter = (GroovyObjectEx<PermissionIntercepter>) getObject(null, groovyClass, runtimeContext);
                    if (intercepter != null) {
                        uriLogs.append("Mapped " + key + " | " + groovyClass + " to request permission intercepter." + "\r\n");
                        servletManager.setPermissionIntercepter(intercepter);
                    }
                } else {
                    uriLogs.append("Ignored " + key + " to request permission intercepter." + "\r\n");
                }
            }
            uriLogs.append("---------------------------------------");
            LoggerEx.info(TAG, uriLogs.toString());
        }
    }

}
