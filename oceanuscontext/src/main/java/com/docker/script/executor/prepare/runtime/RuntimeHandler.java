package com.docker.script.executor.prepare.runtime;

import chat.config.Configuration;
import com.docker.script.BaseRuntimeContext;
import oceanus.apis.CoreException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface RuntimeHandler {
    BaseRuntimeContext prepare(Configuration configuration) throws CoreException;
}
