package com.container.runtime.boot.manager;

import com.docker.oceansbean.OceanusBeanManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public class DefaultOceansBeanManager implements OceanusBeanManager {
    private Map<String, Object> beanNameMap = new ConcurrentHashMap<>();
    private Map<String, Object> classNameMap = new ConcurrentHashMap<>();
    @Override
    public void addBean(String className, String[] beanNames, Object o) {
        for (String beanName : beanNames){
            Object duplicate = this.beanNameMap.putIfAbsent(beanName, o);
            if(duplicate != null){
                throw new RuntimeException("Bean name is duplicate, beanName: " + beanName);
            }
        }
        this.classNameMap.putIfAbsent(className, o);
    }

    @Override
    public Object getBeanByClassName(String className) {
        return this.classNameMap.get(className);
    }

    @Override
    public Object getBeanByBeanName(String beanName) {
        return this.beanNameMap.get(beanName);
    }
}
