package com.docker.script.executor;

/**
 * Created by lick on 2020/12/24.
 * Description：
 */
public interface RuntimeExecutorListener {
    public void handleSuccess();

    public void handleFailed(Throwable t);

    public void handleFailed(Throwable t, String service, Integer version);
}
