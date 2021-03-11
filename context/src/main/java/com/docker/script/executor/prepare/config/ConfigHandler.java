package com.docker.script.executor.prepare.config;

import chat.config.Configuration;

import java.io.IOException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface ConfigHandler {
    void prepare(Configuration configuration) throws IOException;
}
