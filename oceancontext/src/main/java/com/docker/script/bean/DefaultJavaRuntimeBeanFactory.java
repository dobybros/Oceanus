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
import script.core.runtime.java.JavaRuntimeBeanFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultJavaRuntimeBeanFactory extends JavaRuntimeBeanFactory {
    private Map<String, AbstractObject> beanMap = new ConcurrentHashMap<>();
    private AbstractRuntimeContext runtimeContext;
    public DefaultJavaRuntimeBeanFactory(AbstractRuntimeContext runtimeContext){
        this.runtimeContext = runtimeContext;
    }
}
