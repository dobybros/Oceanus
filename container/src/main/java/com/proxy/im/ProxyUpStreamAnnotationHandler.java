package com.proxy.im;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.proxy.annotation.ProxyMessageReceived;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationGlobalHandler;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProxyUpStreamAnnotationHandler extends ClassAnnotationGlobalHandler {
    private static final String TAG = ProxyUpStreamAnnotationHandler.class.getSimpleName();
    private List<GroovyObjectEx<ProxyMessageReceivedListener>> proxyMessageReceivedListeners = new ArrayList<>();

    public List<GroovyObjectEx<ProxyMessageReceivedListener>> getProxyMessageReceivedListeners() {
        return proxyMessageReceivedListeners;
    }

    public void setProxyMessageReceivedListeners(List<GroovyObjectEx<ProxyMessageReceivedListener>> proxyMessageReceivedListeners) {
        this.proxyMessageReceivedListeners = proxyMessageReceivedListeners;
    }

    @Override
    public void handleAnnotatedClassesInjectBean(AbstractRuntimeContext runtimeContext) {
        for (GroovyObjectEx<ProxyMessageReceivedListener> groovyObjectEx : proxyMessageReceivedListeners) {
            try {
                groovyObjectEx = (GroovyObjectEx<com.proxy.im.ProxyMessageReceivedListener>) getObject(null, groovyObjectEx.getGroovyClass(), runtimeContext);
            }catch (CoreException e){
                LoggerEx.error(TAG, e.getMessage());
            }
        }
    }

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return ProxyMessageReceived.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap, AbstractRuntimeContext runtimeContext) throws CoreException {
        if (annotatedClassMap != null && !annotatedClassMap.isEmpty()) {
            StringBuilder uriLogs = new StringBuilder(
                    "\r\n---------------------------------------\r\n");

            Set<String> keys = annotatedClassMap.keySet();
            List<GroovyObjectEx<ProxyMessageReceivedListener>> newProxyMessageReceivedListeners = new ArrayList<>();
            for (String key : keys) {
                Class<?> groovyClass = annotatedClassMap.get(key);
                if (groovyClass != null) {
                    ProxyMessageReceived messageReceivedAnnotation = groovyClass.getAnnotation(ProxyMessageReceived.class);
                    if (messageReceivedAnnotation != null) {
                        GroovyObjectEx<ProxyMessageReceivedListener> messageReceivedObj = (GroovyObjectEx<com.proxy.im.ProxyMessageReceivedListener>) getObject(null, groovyClass, runtimeContext);
                        if (messageReceivedObj != null) {
                            uriLogs.append("MessageReceivedListener " + groovyClass + "\r\n");
                            newProxyMessageReceivedListeners.add(messageReceivedObj);
                        }
                    }
                }
            }
            this.proxyMessageReceivedListeners = newProxyMessageReceivedListeners;
            uriLogs.append("---------------------------------------");
            LoggerEx.info(TAG, uriLogs.toString());
        }
    }


}
