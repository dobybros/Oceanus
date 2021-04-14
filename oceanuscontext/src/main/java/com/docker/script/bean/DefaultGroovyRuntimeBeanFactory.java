package com.docker.script.bean;

import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.utils.ReflectionUtil;
import groovy.lang.GroovyObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.annotation.Bean;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.GroovyRuntimeBeanFactory;
import script.core.runtime.groovy.object.AbstractObject;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.AbstractFieldAnnotationHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultGroovyRuntimeBeanFactory extends GroovyRuntimeBeanFactory {
    private Map<String, AbstractObject> beanMap = new ConcurrentHashMap<>();
    private AbstractRuntimeContext runtimeContext;
    public DefaultGroovyRuntimeBeanFactory(AbstractRuntimeContext runtimeContext){
        this.runtimeContext = runtimeContext;
    }
    @Override
    protected <T> AbstractObject<T> getBeanGroovy(String beanName, String groovyPath) throws CoreException {
        if(StringUtils.isBlank(beanName)){
            beanName = groovyPath;
        }
        AbstractObject<T> goe = beanMap.get(beanName);
        if(goe == null){
            goe = new GroovyObjectEx<T>(groovyPath);
            AbstractObject oldObj = beanMap.putIfAbsent(beanName, goe);
            if(oldObj != null){
                goe = oldObj;
            }else {
                goe.setRuntimeContext(this.runtimeContext);
            }
        }
        return goe;
    }

    @Override
    protected void fillObjectGroovy() throws CoreException {
        for (AbstractObject object : beanMap.values()){
            try {
                fillObject(object);
            } catch(Throwable throwable) {
                throwable.printStackTrace();
                LoggerEx.error(TAG, "Fill object " + object + " failed, " + throwable.getMessage());
            }
        }
    }

    @Override
    public void fillObject(Object o) throws CoreException {
        try {
            GroovyObject gObj = null;
            if(o instanceof AbstractObject){
                if(((AbstractObject) o).isFill()){
                    return;
                }
                gObj = (GroovyObject) ((AbstractObject) o).getObject(false);
            }else if(o instanceof GroovyObject){
                gObj = (GroovyObject)o;
            }
            Field[] fields = ReflectionUtil.getFields(gObj.getClass());
            if (fields != null) {
                for (Field field : fields) {
                    Bean bean = field.getAnnotation(Bean.class);
                    if (bean != null) {
                        String beanName = bean.name();
                        Class<?> gClass = null;
                        if (StringUtils.isBlank(beanName)) {
                            if (field.getType().isAssignableFrom(GroovyObjectEx.class)) {
                                Type fieldType = field.getGenericType();
                                if (fieldType instanceof ParameterizedType) {
                                    ParameterizedType pType = (ParameterizedType) fieldType;
                                    Type[] aTypes = pType.getActualTypeArguments();
                                    if (aTypes != null && aTypes.length == 1) {
                                        gClass = (Class<?>) aTypes[0];
                                    }
                                }
                            } else {
                                gClass = field.getType();
                            }
                        }

                        AbstractObject<?> beanValue = null;
                        if (StringUtils.isBlank(beanName)) {
                            if(gClass != null){
                                Class<?> theClass = runtimeContext.getClass(gClass.getName());
                                if(theClass == null) {
                                    LoggerEx.error(TAG, "Class " + gClass + " doesn't be found in your runtime, perhaps you want to use JavaBean instead");
                                } else {
                                    gClass = theClass;
                                }
                                beanValue = (AbstractObject<?>) get(null, runtimeContext.getRuntime().path(gClass));
                            }
                        } else {
                            beanValue = (AbstractObject<?>) runtimeContext.getRuntimeBeanFactory().get(beanName, null);
                        }
                        if (beanValue != null) {
                            if (field.getType().isAssignableFrom(AbstractObject.class)) {
                                field.setAccessible(true);
                                field.set(gObj, beanValue);
                            } else {
                                field.setAccessible(true);
                                Object obj = null;
                                try {
                                    obj = beanValue.getObject();
                                    field.set(gObj, gClass.cast(obj));
                                } catch (CoreException e) {
                                    e.printStackTrace();
                                    LoggerEx.warn(TAG, "Assign value failed, " + field + " error " + e.getMessage());
                                }
                            }
                        }
                    }else {
                        List<AbstractFieldAnnotationHandler> injectListeners = runtimeContext.getFieldAnnotationHandlers();
                        if (injectListeners != null) {
                            for (AbstractFieldAnnotationHandler listener : injectListeners) {
                                try {
                                    Class<? extends Annotation> annotationClass = listener.annotationClass();
                                    if (annotationClass != null) {
                                        Annotation annotation = field.getAnnotation(annotationClass);
                                        if (annotation != null) {
                                            listener.inject(annotation, field, gObj);
                                            break;
                                        }
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    LoggerEx.error(TAG, "handle field inject listener " + listener + " failed, " + t.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }catch (IllegalAccessException e){
            throw new CoreException(ChatErrorCodes.ERROR_REFLECT, ExceptionUtils.getFullStackTrace(e));
        }
    }

    @Override
    public void close() {
        beanMap.clear();
    }
}
