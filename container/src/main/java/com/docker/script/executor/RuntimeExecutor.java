package com.docker.script.executor;

import chat.errors.CoreException;
import chat.config.BaseConfiguration;
import chat.config.Configuration;
import script.Runtime;

import java.util.Map;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public interface RuntimeExecutor {
    /**
     * compile services sync
     */
    void execute(BaseConfiguration baseConfiguration, RuntimeExecutorHandler runtimeExecutorHandler);
    /**
     * compile services async
     */
    void executeAsync(BaseConfiguration baseConfiguration, RuntimeExecutorHandler runtimeExecutorHandler);
}
