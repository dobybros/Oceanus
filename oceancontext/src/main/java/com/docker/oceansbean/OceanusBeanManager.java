package com.docker.oceansbean;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public interface OceanusBeanManager {
    public void addBean(String className, String[] beanNames, Object o);

    public Object getBeanByClassName(String className);

    public Object getBeanByBeanName(String beanName);
}
