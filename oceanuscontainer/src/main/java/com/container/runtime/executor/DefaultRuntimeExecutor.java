package com.container.runtime.executor;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.main.ServerStart;
import chat.config.BaseConfiguration;
import chat.config.Configuration;
import com.container.runtime.DefaultRuntimeContext;
import com.container.runtime.executor.prepare.config.DiscoveryConfigHandler;
import com.container.runtime.executor.prepare.source.DefaultServiceDownloadHandler;
import com.container.runtime.executor.serviceversion.LocalServiceVersionsHandler;
import com.docker.script.executor.RuntimeExecutor;
import com.docker.script.executor.RuntimeExecutorListener;
import com.docker.script.executor.prepare.PrepareAndStartServiceHandler;
import com.container.runtime.executor.prepare.DefaultPrepareAndStartServiceHandler;
import com.docker.script.executor.prepare.config.ConfigHandler;
import com.docker.script.executor.prepare.source.ServiceDownloadHandler;
import com.docker.script.executor.serviceversion.ServiceVersionsHandler;
import com.docker.server.OnlineServer;
import core.discovery.impl.client.ServiceRuntime;
import core.discovery.node.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class DefaultRuntimeExecutor implements RuntimeExecutor {
    private final String TAG = DefaultRuntimeExecutor.class.getName();
    private ServiceVersionsHandler serviceVersionsHandler;
    private PrepareAndStartServiceHandler prepareServiceHandler;
    private ConfigHandler configHandler;
    private ServiceDownloadHandler serviceDownloadHandler;

//    private ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<>();

    public DefaultRuntimeExecutor(){
        this.serviceDownloadHandler = new DefaultServiceDownloadHandler();
        this.serviceVersionsHandler = new LocalServiceVersionsHandler();//new DefaultServiceVersionsHandler();
        this.prepareServiceHandler = new DefaultPrepareAndStartServiceHandler();
        this.configHandler = new DiscoveryConfigHandler();
    }

    @Override
    public void execute(BaseConfiguration baseConfiguration, RuntimeExecutorListener runtimeExecutorHandler) {
        //TODO 应该判断generateConfigurations返回的Map里如果没有包含已存在的Runtime时， 应该卸载这个Runtime
        try {
            this.serviceVersionsHandler.generateConfigurations(baseConfiguration).values().forEach(configuration -> compileService(configuration, runtimeExecutorHandler));
        } catch (Throwable t){
            t.printStackTrace();
            runtimeExecutorHandler.handleFailed(t);
            return;
        }
        runtimeExecutorHandler.handleSuccess();
    }

    @Override
    public void executeAsync(BaseConfiguration baseConfiguration, RuntimeExecutorListener runtimeExecutorHandler) {
        //TODO 应该判断generateConfigurations返回的Map里如果没有包含已存在的Runtime时， 应该卸载这个Runtime
        Map<String, Configuration> configurationMap = null;
        try {
            configurationMap = this.serviceVersionsHandler.generateConfigurations(baseConfiguration);
        }catch (CoreException e){
            runtimeExecutorHandler.handleFailed(e);
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(configurationMap.size());
        configurationMap.values().forEach(configuration -> ServerStart.getInstance().getThreadPool().execute(()->{
            compileService(configuration, runtimeExecutorHandler);
            countDownLatch.countDown();
        }));
        try {
            countDownLatch.await();
        }catch (InterruptedException e){
            LoggerEx.error(TAG, "configurations await, err: " + e.getCause());
            runtimeExecutorHandler.handleFailed(e);
            return;
        }
        runtimeExecutorHandler.handleSuccess();
    }

    private void compileService(Configuration configuration, RuntimeExecutorListener runtimeExecutorListener){
        //解析config.properties到configuration
        try {
            //        //下载service,以及判断是否需要热加载
            if(this.serviceDownloadHandler.prepare(configuration)){
                //下载pom.xml的依赖
                this.configHandler.prepare(configuration);
                CompletableFuture<Void> future = updateService(configuration, Service.STATUS_WILL_DEPLOY).thenAccept(o -> {
                    try {
                        this.prepareServiceHandler.prepareAndStart(configuration, (runtimeContext) -> {
                            //add service to dockerStatus
                            updateService(configuration, Service.STATUS_DEPLOYED);
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        updateService(configuration, Service.STATUS_DEPLOY_FAILED);
                        runtimeExecutorListener.handleFailed(t, configuration.getService(), configuration.getVersion());
                    }
                });
                future.get();
            }
        } catch (IOException | CoreException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Read config failed, " + e.getMessage() + " configuration " + configuration);
        }
    }

    private CompletableFuture<Void> updateService(Configuration configuration, int serviceStatus) {
        CompletableFuture<Void> future = new CompletableFuture<>();
//        DockerStatusService dockerStatusService = (DockerStatusService) BeanFactory.getBean(DockerStatusServiceImpl.class.getName());
        if(configuration.getService().equals("discovery")) {
            LoggerEx.info(TAG, "Service discovery will not register service to itself. configuration " + configuration);
            return CompletableFuture.completedFuture(null);
        }

        Service service = new Service();
//        String owner = configuration.getConfig().getProperty("self.owner");
//        String project = configuration.getConfig().getProperty("self.project");
//        if(owner == null || project == null) {
//            throw new CoreException(DiscoveryErrorCodes.ERROR_OWNER_PROJECT_MISSING, "Owner or project is missing in config.properties, please check \"self.owner\" and \"self.project\"");
//        }
//        service.setOwner(owner);
//        service.setProject(project);
        service.setService(configuration.getService());
        service.setVersion(configuration.getVersion());
        service.setUploadTime(configuration.getDeployVersion());
        service.setStatus(serviceStatus);
        String minVersionStr = configuration.getConfig().getProperty("oceanus.min.version", "1");
        Integer minVersion = 1;
        try {
            minVersion = Integer.parseInt(minVersionStr);
        } catch(Throwable ignored){}
        service.setMinVersion(minVersion);

        String serviceSuffix = configuration.getConfig().getProperty("oceanus.service.suffix");
        if(serviceSuffix != null && serviceSuffix.contains("_")) {
            serviceSuffix = serviceSuffix.replace("_", "");
            LoggerEx.warn(TAG, "Service suffix don't allow \"_\" in it, will remove it by default, changed to suffix " + serviceSuffix);
        }
        service.setServiceSuffix(serviceSuffix);

//        if(configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER) != null){
//            service.setMaxUserNumber(Long.parseLong((String) configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER)));
//        }
//        Collection<AbstractClassAnnotationHandler> handlers = runtimeContext.getAnnotationHandlerMap().values();
//        for (AbstractClassAnnotationHandler handler : handlers) {
//            if (handler instanceof ClassAnnotationHandlerEx)
//                ((ClassAnnotationHandlerEx) handler).configService(service);
//        }
//        service.setType(Service.FIELD_SERVER_TYPE_NORMAL);
//        serviceMap.put(configuration.getService(), service);

        CompletableFuture<ServiceRuntime> registerServiceFuture = OnlineServer.getInstance().registerService(service);
        registerServiceFuture.thenAccept(serviceRuntime -> {
            LoggerEx.info(TAG, "Service " + service.generateServiceKey() + " registered!");
            future.complete(null);
        }).exceptionally(throwable -> {
            LoggerEx.error(TAG, "Service " + service.generateServiceKey() + " register failed! " + throwable.getMessage());
            future.completeExceptionally(throwable);
            return null;
        });
        try {
            registerServiceFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Register service " + service + " failed, " + e.getMessage());
        }
//        dockerStatusService.deleteService(configuration.getBaseConfiguration().getServer(), service.getService(), service.getVersion());
//        dockerStatusService.addService(configuration.getBaseConfiguration().getServer(), service);
        return future;
    }
}
