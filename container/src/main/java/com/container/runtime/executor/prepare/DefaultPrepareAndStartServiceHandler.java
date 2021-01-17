package com.container.runtime.executor.prepare;

import chat.config.Configuration;
import chat.logs.LoggerEx;
import com.container.runtime.DefaultRuntimeContext;
import com.docker.script.executor.prepare.PrepareAndStartServiceHandler;
import com.docker.script.executor.prepare.PrepareAndStartServiceProcessListener;
import com.docker.script.executor.prepare.config.ConfigHandler;
import com.docker.script.executor.prepare.dependency.DependencyDownloadHandler;
import com.docker.script.executor.prepare.source.ServiceDownloadHandler;
import com.docker.script.executor.prepare.runtime.RuntimeHandler;
import com.container.runtime.executor.prepare.config.DefaultConfigHandler;
import com.container.runtime.executor.prepare.dependency.MvnDependencyDownloadHandler;
import com.container.runtime.executor.prepare.runtime.DefaultRuntimeHandler;
import com.container.runtime.executor.prepare.source.GridFSServiceDownloadHandler;
import script.RuntimeContext;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultPrepareAndStartServiceHandler implements PrepareAndStartServiceHandler {
    private final String TAG = DefaultPrepareAndStartServiceHandler.class.getSimpleName();
    private ServiceDownloadHandler serviceDownloadHandler;
    private ConfigHandler configHandler;
    private DependencyDownloadHandler dependencyDownloadHandler;
    private RuntimeHandler runtimeHandler;
    public DefaultPrepareAndStartServiceHandler(){
        this.serviceDownloadHandler = new GridFSServiceDownloadHandler();
        this.configHandler = new DefaultConfigHandler();
        this.dependencyDownloadHandler = new MvnDependencyDownloadHandler();
        this.runtimeHandler = new DefaultRuntimeHandler();
    }

    @Override
    public void prepareAndStart(Configuration configuration, PrepareAndStartServiceProcessListener prepareAndStartServiceProcessHandler) throws Throwable {
        //下载service,以及判断是否需要热加载
        if(this.serviceDownloadHandler.prepare(configuration)){
            //解析config.properties到configuration
            this.configHandler.prepare(configuration);
            //下载pom.xml的依赖
            this.dependencyDownloadHandler.prepare(configuration);
            //添加注解handler,以及初始化runtime和runtimeContext,并开始编译原码
            RuntimeContext runtimeContext = this.runtimeHandler.prepare(configuration);
            if(prepareAndStartServiceProcessHandler != null){
                prepareAndStartServiceProcessHandler.afterStart((DefaultRuntimeContext)runtimeContext);
            }
            LoggerEx.info(TAG, "=====Notice!!! The service: " + configuration.getServiceVersion() + " has being redeployed====");
        }
    }
}
