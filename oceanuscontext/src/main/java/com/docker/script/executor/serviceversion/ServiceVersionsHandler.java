package com.docker.script.executor.serviceversion;

import chat.errors.CoreException;
import chat.config.BaseConfiguration;
import chat.config.Configuration;

import java.util.Map;

/**
 * Created by lick on 2020/12/17.
 * Description：
 */
public interface ServiceVersionsHandler {
    Map<String, Configuration> generateConfigurations(BaseConfiguration baseConfiguration) throws CoreException;
}
