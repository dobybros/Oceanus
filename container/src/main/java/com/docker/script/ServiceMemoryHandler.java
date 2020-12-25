package com.docker.script;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.script.annotations.ServiceMemory;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by lick on 2019/11/4
 */
public class ServiceMemoryHandler extends ClassAnnotationHandler {
    private final String TAG = ServiceMemory.class.getSimpleName();
    private List<GroovyObjectEx<?>> serviceMemoryList = new ArrayList();
    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return ServiceMemory.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        if(annotatedClassMap != null){
            Collection<Class<?>> values = annotatedClassMap.values();
            for (Class<?> groovyClass : values){
                GroovyObjectEx<?> groovyObj = (GroovyObjectEx<?>) getObject(null, groovyClass, runtimeContext);
                serviceMemoryList.add(groovyObj);
            }
        }
    }
    public List<Object> getMemory(){
        if(!serviceMemoryList.isEmpty()){
            List<Object> list = new ArrayList<>();
            for (GroovyObjectEx<?> groovyObjectEx : serviceMemoryList){
                try {
                    Object jsonObject = groovyObjectEx.invokeRootMethod("memory");
                    list.add(jsonObject);
                } catch (Throwable e) {
                    LoggerEx.error(TAG, "Get service memory error, err: " + ExceptionUtils.getFullStackTrace(e));
                    e.printStackTrace();
                }
            }
            return list;
        }
        return null;
    }
    @Override
    public void handlerShutdown() {
    }
}
