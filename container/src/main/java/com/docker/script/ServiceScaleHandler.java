package com.docker.script;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.alibaba.fastjson.JSONObject;
import com.docker.script.annotations.ServiceScaleListener;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

/**
 * Created by lick on 2019/11/4
 */
public class ServiceScaleHandler extends ClassAnnotationHandler {
    private final String TAG = ServiceScaleHandler.class.getSimpleName();
    private GroovyObjectEx<?> serviceScaleObj = null;

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return ServiceScaleListener.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        if (annotatedClassMap != null) {
            Collection<Class<?>> values = annotatedClassMap.values();
            for (Class<?> groovyClass : values) {
                serviceScaleObj = (GroovyObjectEx<?>) getObject(null, groovyClass, runtimeContext);
            }
        }
    }

    public Object invoke(String methodName, JSONObject params) throws CoreException {
        if (serviceScaleObj != null) {
            try {
                Object result = null;
                if(params != null){
                    result = serviceScaleObj.invokeRootMethod(methodName, params);
                }else {
                    result = serviceScaleObj.invokeRootMethod(methodName);
                }
                return result;
            } catch (CoreException e) {
                LoggerEx.error(TAG, "Scale invoke service error, err: " + ExceptionUtils.getFullStackTrace(e));
                throw e;
            }
        } else {
            LoggerEx.error(TAG, "Scale invoke service error, cant find serviceScaleObj");
        }
        return null;
    }

    @Override
    public void handlerShutdown() {
    }
}
