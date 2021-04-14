package com.container.runtime.boot.bean;

import com.container.runtime.executor.DefaultRuntimeExecutor;
import com.container.runtime.boot.manager.BootManager;
import com.docker.script.executor.prepare.config.BaseConfigurationBuilder;
import com.docker.oceansbean.BeanFactory;


/**
 * @author lick
 * @date 2019/11/12
 */
public class BeanApp extends ContextBeanApp {
    public static volatile BeanApp instance;
    private BootManager bootManager;

    public synchronized BootManager getBootManager() {
        if (bootManager == null) {
            bootManager = new BootManager();
            bootManager.setBaseConfiguration(baseConfiguration);
            bootManager.setRuntimeExecutor(new DefaultRuntimeExecutor());
//            bootManager.setDockerStatusService(getDockerStatusService());
//            bootManager.setDeployServiceVersionService(getDeployServiceVersionService());
//            bootManager.setServiceVersionService(getServiceVersionService());
        }
        return bootManager;
    }

    public static synchronized BeanApp getInstance(){
        if(instance == null){
            synchronized (BeanApp.class){
                if(instance == null){
                    instance = new BeanApp();
                    baseConfiguration = new BaseConfigurationBuilder().build();
                    BeanFactory.init(baseConfiguration);
                }
            }
        }
        return instance;
    }
}
