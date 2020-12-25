package com.docker.script.executor.prepare;

import chat.config.Configuration;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface PrepareServiceHandler {
    void prepare(Configuration configuration) throws Throwable;
}
