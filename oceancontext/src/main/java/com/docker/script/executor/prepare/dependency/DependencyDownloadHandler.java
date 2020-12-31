package com.docker.script.executor.prepare.dependency;

import chat.config.Configuration;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface DependencyDownloadHandler {
    void prepare(Configuration configuration) throws Throwable;
}
