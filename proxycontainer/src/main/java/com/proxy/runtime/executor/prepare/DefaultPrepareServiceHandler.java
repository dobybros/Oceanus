package com.proxy.runtime.executor.prepare;

import chat.config.Configuration;
import chat.logs.LoggerEx;
import com.docker.script.executor.prepare.PrepareServiceHandler;
import com.docker.script.executor.prepare.config.ConfigHandler;
import com.docker.script.executor.prepare.dependency.DependencyDownloadHandler;
import com.docker.script.executor.prepare.source.ServiceDownloadHandler;
import com.docker.script.executor.prepare.runtime.RuntimeHandler;
import com.proxy.runtime.executor.prepare.config.DefaultConfigHandler;
import com.proxy.runtime.executor.prepare.dependency.MvnDependencyDownloadHandler;
import com.proxy.runtime.executor.prepare.runtime.DefaultRuntimeHandler;
import com.proxy.runtime.executor.prepare.source.GridFSServiceDownloadHandler;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultPrepareServiceHandler implements PrepareServiceHandler {
    private final String TAG = DefaultPrepareServiceHandler.class.getSimpleName();
    private ServiceDownloadHandler serviceDownloadHandler;
    private ConfigHandler configHandler;
    private DependencyDownloadHandler dependencyDownloadHandler;
    private RuntimeHandler runtimeHandler;
    public DefaultPrepareServiceHandler(){
        this.serviceDownloadHandler = new GridFSServiceDownloadHandler();
        this.configHandler = new DefaultConfigHandler();
        this.dependencyDownloadHandler = new MvnDependencyDownloadHandler();
        this.runtimeHandler = new DefaultRuntimeHandler();
    }

    @Override
    public void prepare(Configuration configuration) throws Throwable {
        //下载service,以及判断是否需要热加载
        if(this.serviceDownloadHandler.prepare(configuration)){
            //解析config.properties到configuration
            this.configHandler.prepare(configuration);
            //下载pom.xml的依赖
            this.dependencyDownloadHandler.prepare(configuration);
            //添加注解handler,以及初始化runtime和runtimeContext,并开始编译原码
            this.runtimeHandler.prepare(configuration);
            LoggerEx.info(TAG, "=====Notice!!! The service: " + configuration.getServiceVersion() + " has being redeployed====");
        }
    }
}
