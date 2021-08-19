package com.docker.script.bean;

import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.object.AbstractObject;
import script.core.runtime.java.JavaRuntimeBeanFactory;

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
