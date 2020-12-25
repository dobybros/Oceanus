package com.proxy.im;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.proxy.annotation.ProxySessionListener;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationGlobalHandler;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author lick
 * @date 2019/11/12
 */
public class ProxyAnnotationHandler extends ClassAnnotationGlobalHandler {
    private final String TAG = ProxyAnnotationHandler.class.getSimpleName();
    private List<GroovyObjectEx<com.proxy.im.ProxySessionListener>> tcpListeners = new ArrayList<>();

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return ProxySessionListener.class;
    }

    @Override
    public void handleAnnotatedClassesInjectBean(AbstractRuntimeContext runtimeContext) {
        for (GroovyObjectEx<com.proxy.im.ProxySessionListener> groovyObjectEx : tcpListeners) {
            try {
                groovyObjectEx = (GroovyObjectEx<com.proxy.im.ProxySessionListener>) getObject(null, groovyObjectEx.getGroovyClass(), runtimeContext);
            }catch (CoreException e){
                LoggerEx.error(TAG, e.getMessage());
            }
        }
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap, AbstractRuntimeContext runtimeContext) throws CoreException {
        if (annotatedClassMap != null && !annotatedClassMap.isEmpty()) {
            StringBuilder uriLogs = new StringBuilder(
                    "\r\n---------------------------------------\r\n");

            List<GroovyObjectEx<com.proxy.im.ProxySessionListener>> newTcpList = new ArrayList<>();
            Set<String> keys = annotatedClassMap.keySet();
            for (String key : keys) {
                Class<?> groovyClass = annotatedClassMap.get(key);
                if (groovyClass != null) {
                    ProxySessionListener messageReceivedAnnotation = groovyClass.getAnnotation(ProxySessionListener.class);
                    if (messageReceivedAnnotation != null) {
                        GroovyObjectEx<com.proxy.im.ProxySessionListener> messageReceivedObj = (GroovyObjectEx<com.proxy.im.ProxySessionListener>) getObject(null, groovyClass, runtimeContext);
                        if (messageReceivedObj != null) {
                            uriLogs.append("TcpListener " + "#" + groovyClass + "\r\n");
                            newTcpList.add(messageReceivedObj);
                        }
                    }
                }
            }
            this.tcpListeners = newTcpList;
            uriLogs.append("---------------------------------------");
            LoggerEx.info(TAG, uriLogs.toString());
        }
    }

    public List<GroovyObjectEx<com.proxy.im.ProxySessionListener>> getTcpListeners() {
        return tcpListeners;
    }
}
