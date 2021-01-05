package com.docker.script.executor.prepare;

import chat.errors.CoreException;
import com.docker.script.BaseRuntimeContext;

/**
 * Created by lick on 2021/1/5.
 * Description：
 */
public interface PrepareAndStartServiceProcessHandler {
    public void afterStart(BaseRuntimeContext runtimeContext) throws CoreException;
}
