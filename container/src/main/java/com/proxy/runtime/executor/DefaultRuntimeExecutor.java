package com.proxy.runtime.executor;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.main.ServerStart;
import chat.config.BaseConfiguration;
import chat.config.Configuration;
import com.docker.script.executor.RuntimeExecutor;
import com.docker.script.executor.RuntimeExecutorHandler;
import com.docker.script.executor.prepare.PrepareServiceHandler;
import com.proxy.runtime.executor.prepare.DefaultPrepareServiceHandler;
import com.docker.script.executor.serviceversion.ServiceVersionsHandler;
import com.proxy.runtime.executor.serviceversion.DefaultServiceVersionsHandler;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public class DefaultRuntimeExecutor implements RuntimeExecutor {
    private final String TAG = DefaultRuntimeExecutor.class.getName();
    private ServiceVersionsHandler serviceVersionsHandler;
    private PrepareServiceHandler prepareServiceHandler;
    public DefaultRuntimeExecutor(){
        this.serviceVersionsHandler = new DefaultServiceVersionsHandler();
        this.prepareServiceHandler = new DefaultPrepareServiceHandler();
    }

    @Override
    public void execute(BaseConfiguration baseConfiguration, RuntimeExecutorHandler runtimeExecutorHandler) {
        try {
            this.serviceVersionsHandler.generateConfigurations(baseConfiguration).values().forEach(configuration -> compileService(configuration, runtimeExecutorHandler));
        }catch (Throwable t){
            runtimeExecutorHandler.handleFailed(t);
            return;
        }
        runtimeExecutorHandler.handleSuccess();
    }

    @Override
    public void executeAsync(BaseConfiguration baseConfiguration, RuntimeExecutorHandler runtimeExecutorHandler) {
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
        }
        runtimeExecutorHandler.handleSuccess();
    }

    private void compileService(Configuration configuration, RuntimeExecutorHandler runtimeExecutorHandler){
        try {
            this.prepareServiceHandler.prepare(configuration);
        } catch (Throwable t) {
            runtimeExecutorHandler.handleFailed(t, configuration.getService(), configuration.getVersion());
        }
    }
}
