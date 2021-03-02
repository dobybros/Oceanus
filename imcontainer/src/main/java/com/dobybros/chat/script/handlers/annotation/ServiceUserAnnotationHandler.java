package com.dobybros.chat.script.handlers.annotation;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.dobybros.chat.script.listeners.ServiceUserListener;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hzj on 2021/1/3 下午6:34 
 */
public abstract class ServiceUserAnnotationHandler extends ClassAnnotationHandler {

    public static final String TAG = ServiceUserAnnotationHandler.class.getSimpleName();

    private Map<String, Class<?>> annotatedClassMap;

    private ConcurrentHashMap<String, ServiceUserListener> listenerMap = new ConcurrentHashMap<>();

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        this.setAnnotatedClassMap(annotatedClassMap);
    }

    @Override
    public void handlerShutdown() {
        super.handlerShutdown();
        for (String serverUser : listenerMap.keySet()) {
            ServiceUserListener listener = listenerMap.get(serverUser);
            // todo 关闭session
            /*try {
                listener.closeSession();
            } catch (CoreException e) {
                LoggerEx.error(TAG, "Listener close session error when handlerShutdown, eMsg : " + e.getMessage());
            }*/
            listenerMap.remove(serverUser);
        }
    }

    public ServiceUserListener createAnnotatedListener(String userId, String subUserId, String service) {
        if (annotatedClassMap != null && !annotatedClassMap.isEmpty()) {
            String key = getUserKey(subUserId, service);
            ServiceUserListener listener = listenerMap.get(key);
            if (listener != null)
                return listener;
            for (Class<?> annotatedClass : annotatedClassMap.values()) {
                try {
                    if (annotatedClass.isAssignableFrom(ServiceUserListener.class)) {
                        ServiceUserListener obj = (ServiceUserListener)annotatedClass.getDeclaredConstructor().newInstance();
                        obj.setParentUserId(userId);
                        obj.setUserId(subUserId);
                        obj.setService(service);
                        runtimeContext.injectBean(obj);
                        listener = obj;
                    }
                } catch (Throwable t) {
                    LoggerEx.error(getTag(), "Create listener error, eMsg : " + t.getMessage());
                }
                if (listener != null)
                    break;
            }
            if (listener != null) {
                listenerMap.putIfAbsent(key, listener);
            } else {
                LoggerEx.error(getTag(), "Create listener error, eMsg : can not find listener.");
            }
            return listenerMap.get(key);
        }
        return null;
    }

    public ServiceUserListener getAnnotatedListener(String userId, String service) {
        if (listenerMap != null) {
            String key = getUserKey(userId, service);
            return listenerMap.get(key);
        }
        return null;
    }

    public void removeListeners(String subRoomId, String service) {
        if (listenerMap != null)
            listenerMap.remove(getUserKey(subRoomId, service));
    }

    private String getUserKey(String userId, String service) {
        return userId + "@" + service;
    }

    public ConcurrentHashMap<String, ServiceUserListener> getListenerMap() {
        return listenerMap;
    }

    public void setListenerMap(ConcurrentHashMap<String, ServiceUserListener> listenerMap) {
        this.listenerMap = listenerMap;
    }

    public Map<String, Class<?>> getAnnotatedClassMap() {
        return annotatedClassMap;
    }

    public void setAnnotatedClassMap(Map<String, Class<?>> annotatedClassMap) {
        this.annotatedClassMap = annotatedClassMap;
    }

    @Override
    public boolean isBean() {
        return false;
    }

    public String getTag() {
        return TAG;
    }
}
