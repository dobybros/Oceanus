package com.container.runtime.executor;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.main.ServerStart;
import chat.config.BaseConfiguration;
import chat.config.Configuration;
import com.container.runtime.DefaultRuntimeContext;
import com.container.runtime.executor.serviceversion.LocalServiceVersionsHandler;
import com.docker.oceansbean.BeanFactory;
import com.docker.script.ClassAnnotationHandlerEx;
import com.docker.script.executor.RuntimeExecutor;
import com.docker.script.executor.RuntimeExecutorListener;
import com.docker.script.executor.prepare.PrepareAndStartServiceHandler;
import com.container.runtime.executor.prepare.DefaultPrepareAndStartServiceHandler;
import com.docker.script.executor.serviceversion.ServiceVersionsHandler;
import com.container.runtime.executor.serviceversion.DefaultServiceVersionsHandler;
import com.docker.server.OnlineServer;
import com.docker.storage.adapters.DockerStatusService;
import com.docker.storage.adapters.impl.DockerStatusServiceImpl;
import core.discovery.errors.DiscoveryErrorCodes;
import core.discovery.impl.client.ServiceRuntime;
import core.discovery.node.Service;
import script.core.runtime.handler.AbstractClassAnnotationHandler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class DefaultRuntimeExecutor implements RuntimeExecutor {
    private final String TAG = DefaultRuntimeExecutor.class.getName();
    private ServiceVersionsHandler serviceVersionsHandler;
    private PrepareAndStartServiceHandler prepareServiceHandler;

    private ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<>();

    public DefaultRuntimeExecutor(){
        this.serviceVersionsHandler = new LocalServiceVersionsHandler();//new DefaultServiceVersionsHandler();
        this.prepareServiceHandler = new DefaultPrepareAndStartServiceHandler();
    }

    @Override
    public void execute(BaseConfiguration baseConfiguration, RuntimeExecutorListener runtimeExecutorHandler) {
        //TODO 应该判断generateConfigurations返回的Map里如果没有包含已存在的Runtime时， 应该卸载这个Runtime
        try {
            this.serviceVersionsHandler.generateConfigurations(baseConfiguration).values().forEach(configuration -> compileService(configuration, runtimeExecutorHandler));
        } catch (Throwable t){
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
        try {
            this.prepareServiceHandler.prepareAndStart(configuration, (runtimeContext) -> {
                //add service to dockerStatus
                updateService(configuration, (DefaultRuntimeContext) runtimeContext);
            });
        } catch (Throwable t) {
            runtimeExecutorListener.handleFailed(t, configuration.getService(), configuration.getVersion());
        }
    }

    private void updateService(Configuration configuration, DefaultRuntimeContext runtimeContext) throws CoreException {
//        DockerStatusService dockerStatusService = (DockerStatusService) BeanFactory.getBean(DockerStatusServiceImpl.class.getName());
        if(configuration.getService().equals("discovery")) {
            LoggerEx.info(TAG, "Service discovery will not register service to itself. configuration " + configuration);
            return;
        }

        Service service = new Service();
        String owner = configuration.getConfig().getProperty("self.owner");
        String project = configuration.getConfig().getProperty("self.project");
        if(owner == null || project == null) {
            throw new CoreException(DiscoveryErrorCodes.ERROR_OWNER_PROJECT_MISSING, "Owner or project is missing in config.properties, please check \"self.owner\" and \"self.project\"");
        }
        service.setOwner(owner);
        service.setProject(project);
        service.setService(configuration.getService());
        service.setVersion(configuration.getVersion());
        service.setUploadTime(configuration.getDeployVersion());
//        if(configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER) != null){
//            service.setMaxUserNumber(Long.parseLong((String) configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER)));
//        }
//        Collection<AbstractClassAnnotationHandler> handlers = runtimeContext.getAnnotationHandlerMap().values();
//        for (AbstractClassAnnotationHandler handler : handlers) {
//            if (handler instanceof ClassAnnotationHandlerEx)
//                ((ClassAnnotationHandlerEx) handler).configService(service);
//        }
//        service.setType(Service.FIELD_SERVER_TYPE_NORMAL);
        serviceMap.put(configuration.getService(), service);

        OnlineServer.getInstance().registerService(service).thenAccept(serviceRuntime -> {
            LoggerEx.info(TAG, "Service " + service.generateServiceKey() + " registered!");
        }).exceptionally(throwable -> {
            LoggerEx.error(TAG, "Service " + service.generateServiceKey() + " register failed! " + throwable.getMessage());
            return null;
        });
//        dockerStatusService.deleteService(configuration.getBaseConfiguration().getServer(), service.getService(), service.getVersion());
//        dockerStatusService.addService(configuration.getBaseConfiguration().getServer(), service);
    }
}
