package com.docker.script.executor.prepare;

import chat.config.Configuration;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface PrepareAndStartServiceHandler {
    void prepareAndStart(Configuration configuration, PrepareAndStartServiceProcessHandler prepareAndStartServiceProcessHandler) throws Throwable;
}
