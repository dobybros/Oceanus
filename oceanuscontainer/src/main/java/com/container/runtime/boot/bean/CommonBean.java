package com.container.runtime.boot.bean;

import chat.config.BaseConfiguration;
import chat.utils.IPHolder;
import chat.base.bean.annotation.OceanusBean;
import com.docker.context.ContextFactory;
import com.docker.context.impl.DefaultContextFactory;
import com.docker.onlineserver.OnlineServerWithStatus;
import com.docker.rpc.QueueSimplexListener;
import com.docker.tasks.RepairTaskHandler;
import com.container.runtime.boot.manager.BootManager;
import script.core.runtime.RuntimeFactory;
import script.core.runtime.classloader.ClassLoaderFactory;
import script.core.runtime.classloader.impl.DefaultClassLoaderFactory;
import script.core.runtime.impl.DefaultRuntimeFactory;
import script.core.servlets.RequestPermissionHandler;
import script.filter.JsonFilterFactory;

//import com.dobybros.chat.log.LogIndexQueue;

/**
 * Created by lick on 2019/5/27.
 * Description：
 */
@OceanusBean
public class CommonBean {

    private BeanApp instance;
    public CommonBean(){
        instance = BeanApp.getInstance();
    }

    @OceanusBean
    public IPHolder ipHolder() {
        return instance.getIpHolder();
    }

    @OceanusBean
    public JsonFilterFactory jsonFilterFactory() {
        return instance.getJsonFilterFactory();
    }

    @OceanusBean
    public RequestPermissionHandler requestPermissionHandler() {
        return instance.getRequestPermissionHandler();
    }

    @OceanusBean
    public BootManager bootManager() {
        return instance.getBootManager();
    }

    @OceanusBean
    public OnlineServerWithStatus onlineServer() {
        return instance.getOnlineServer();
    }

    @OceanusBean
    public RepairTaskHandler repairTaskHandler(){return instance.getRepairTaskHandler();}

//    @OceanusBean
//    public QueueSimplexListener queueSimplexListener(){
//        return instance.getQueueSimplexListener();
//    }

    @OceanusBean
    public DefaultRuntimeFactory runtimeFactory(){
        return instance.getRuntimeFactory();
    }

    @OceanusBean
    public DefaultClassLoaderFactory classLoaderFactory(){
        return instance.getClassLoaderFactory();
    }

    @OceanusBean
    public BaseConfiguration baseConfiguration(){
        return instance.getBaseConfiguration();
    }

    @OceanusBean
    public DefaultContextFactory contextFactory(){
        return instance.getContextFactory();
    }
}
