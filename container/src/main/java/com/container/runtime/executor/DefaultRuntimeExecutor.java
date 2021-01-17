package com.container.runtime.executor;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.main.ServerStart;
import chat.config.BaseConfiguration;
import chat.config.Configuration;
import com.container.runtime.DefaultRuntimeContext;
import com.docker.data.Service;
import com.docker.oceansbean.BeanFactory;
import com.docker.script.ClassAnnotationHandlerEx;
import com.docker.script.executor.RuntimeExecutor;
import com.docker.script.executor.RuntimeExecutorListener;
import com.docker.script.executor.prepare.PrepareAndStartServiceHandler;
import com.container.runtime.executor.prepare.DefaultPrepareAndStartServiceHandler;
import com.docker.script.executor.serviceversion.ServiceVersionsHandler;
import com.container.runtime.executor.serviceversion.DefaultServiceVersionsHandler;
import com.docker.storage.adapters.DockerStatusService;
import com.docker.storage.adapters.impl.DockerStatusServiceImpl;
import script.core.runtime.handler.AbstractClassAnnotationHandler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class DefaultRuntimeExecutor implements RuntimeExecutor {
    private final String TAG = DefaultRuntimeExecutor.class.getName();
    private ServiceVersionsHandler serviceVersionsHandler;
    private PrepareAndStartServiceHandler prepareServiceHandler;
    public DefaultRuntimeExecutor(){
        this.serviceVersionsHandler = new DefaultServiceVersionsHandler();
        this.prepareServiceHandler = new DefaultPrepareAndStartServiceHandler();
    }

    @Override
    public void execute(BaseConfiguration baseConfiguration, RuntimeExecutorListener runtimeExecutorHandler) {
        try {
            this.serviceVersionsHandler.generateConfigurations(baseConfiguration).values().forEach(configuration -> compileService(configuration, runtimeExecutorHandler));
        }catch (Throwable t){
            runtimeExecutorHandler.handleFailed(t);
            return;
        }
        runtimeExecutorHandler.handleSuccess();
    }

    @Override
    public void executeAsync(BaseConfiguration baseConfiguration, RuntimeExecutorListener runtimeExecutorHandler) {
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
        DockerStatusService dockerStatusService = (DockerStatusService) BeanFactory.getBean(DockerStatusServiceImpl.class.getName());
        Service service = new Service();
        service.setService(configuration.getService());
        service.setVersion(configuration.getVersion());
        service.setUploadTime(configuration.getDeployVersion());
        if(configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER) != null){
            service.setMaxUserNumber(Long.parseLong((String) configuration.getConfig().get(Service.FIELD_MAXUSERNUMBER)));
        }
        Collection<AbstractClassAnnotationHandler> handlers = runtimeContext.getAnnotationHandlerMap().values();
        for (AbstractClassAnnotationHandler handler : handlers) {
            if (handler instanceof ClassAnnotationHandlerEx)
                ((ClassAnnotationHandlerEx) handler).configService(service);
        }
        service.setType(Service.FIELD_SERVER_TYPE_NORMAL);
        dockerStatusService.deleteService(configuration.getBaseConfiguration().getServer(), service.getService(), service.getVersion());
        dockerStatusService.addService(configuration.getBaseConfiguration().getServer(), service);
    }
}
