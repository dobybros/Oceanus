package com.proxy.runtime;

import chat.config.Configuration;
import chat.errors.CoreException;
import com.dobybros.chat.script.IMRuntimeContext;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public class DefaultRuntimeContext extends IMRuntimeContext {
    public DefaultRuntimeContext(Configuration configuration) throws CoreException {
        super(configuration);
    }
}
