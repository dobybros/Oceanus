package com.container.runtime.boot.thirdenv;

import chat.logs.LoggerEx;
import com.docker.annotations.ServiceBean;
import com.docker.context.RemoteContext;
import com.docker.context.RemoteContextFactory;
import script.core.annotation.JavaBean;

import java.lang.reflect.Field;

/**
 * Created by lick on 2021/1/6.
 * Description：
 */
public class OceanusContext {
    private static final String TAG = OceanusContext.class.getSimpleName();
    private static RemoteContextFactory remoteContextFactory;
    private static void inject(Object o) {
        if(o != null){
            Field[] fields = o.getClass().getDeclaredFields();
            for (Field field : fields){
                if(field.isAnnotationPresent(JavaBean.class)){
                    if(field.getGenericType().equals(RemoteContext.class)){
                        field.setAccessible(true);
                        try {
                            field.set(o, remoteContextFactory().get());
                        } catch (IllegalAccessException e) {
                            LoggerEx.error(TAG, "Reflection set field failed, errMsg: " + e.getMessage());
                        }
                    }
                }else if(field.isAnnotationPresent(ServiceBean.class)){
                    ServiceBean serviceBean = field.getDeclaredAnnotation(ServiceBean.class);
                    field.setAccessible(true);
                    try {
                        field.set(o, remoteContextFactory().get().getService(serviceBean.lanId(), serviceBean.name(), (Class)field.getGenericType()));
                    } catch (Exception e) {
                        LoggerEx.error(TAG, "Reflection set field failed, errMsg: " + e.getMessage());
                    }
                }
            }
        }
    }

    public static void inject(Object... objects){
        for (Object o : objects){
            inject(o);
        }
    }

    private static RemoteContextFactory remoteContextFactory(){
        if(remoteContextFactory == null){
            synchronized (OceanusContext.class){
                if(remoteContextFactory == null){
                    remoteContextFactory = new DefaultRemoteContextFactory();
                }
            }
        }
        return remoteContextFactory;
    }

}
