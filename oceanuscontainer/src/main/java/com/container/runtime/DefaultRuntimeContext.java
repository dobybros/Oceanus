package com.container.runtime;

import chat.config.Configuration;
import chat.errors.CoreException;
import com.docker.script.BaseRuntimeContext;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public class DefaultRuntimeContext extends BaseRuntimeContext {
    public DefaultRuntimeContext(Configuration configuration) throws CoreException {
        super(configuration);
    }
}
