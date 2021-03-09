package com.docker.script.executor;

import chat.config.BaseConfiguration;
import com.docker.script.executor.RuntimeExecutorListener;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public interface RuntimeExecutor {
    /**
     * compile services sync
     */
    void execute(BaseConfiguration baseConfiguration, RuntimeExecutorListener runtimeExecutorHandler);
    /**
     * compile services async
     */
    void executeAsync(BaseConfiguration baseConfiguration, RuntimeExecutorListener runtimeExecutorHandler);
}
