package com.docker.script.executor.prepare;

import com.docker.script.BaseRuntimeContext;
import oceanus.apis.CoreException;

/**
 * Created by lick on 2021/1/5.
 * Description：
 */
public interface PrepareAndStartServiceProcessListener {
    public void afterStart(BaseRuntimeContext runtimeContext) throws CoreException;
}
