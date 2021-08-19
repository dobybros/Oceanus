package com.container.runtime;

import chat.config.Configuration;
import com.docker.script.BaseRuntimeContext;
import oceanus.apis.CoreException;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public class DefaultRuntimeContext extends BaseRuntimeContext {
    public DefaultRuntimeContext(Configuration configuration) throws CoreException {
        super(configuration);
    }
}
