package com.docker.oceansbean;

import chat.config.BaseConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2019/12/25.
 * Description：
 */
public class BeanFactory {
    private static BaseConfiguration baseConfiguration;
    private static OceanusBeanManager oceanusBeanManager;

    public static Object getBean(String classPath) throws BeansException {
        if(classPath.equals(BaseConfiguration.class.getName())){
            return baseConfiguration;
        }
        return oceanusBeanManager.getBeanByClassName(classPath);
    }
    public static Object getBeanByName(String beanName) throws BeansException {
        return oceanusBeanManager.getBeanByBeanName(beanName);
    }

    public static void init(BaseConfiguration o){
        baseConfiguration = o;
    }

    public static void setOceanusBeanManager(OceanusBeanManager oceanusBeanManager) {
        BeanFactory.oceanusBeanManager = oceanusBeanManager;
    }
}
