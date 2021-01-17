package com.docker.context;

import script.RuntimeContext;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public interface ContextFactory {
    public Context get(RuntimeContext runtimeContext);
}
