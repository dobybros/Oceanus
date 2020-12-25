package com.docker.script.executor.compile;

import com.docker.script.BaseRuntimeContext;
import com.docker.utils.ScriptUtils;
import script.core.runtime.impl.GroovyParseServiceHandler;

/**
 * Created by lick on 2019/5/15.
 * Description：
 */
public class DefaultGroovyParseServiceHandler extends GroovyParseServiceHandler {
    public DefaultGroovyParseServiceHandler(BaseRuntimeContext runtimeContext) {
        super(runtimeContext);
    }
    public void beforeDeploy() {
        ScriptUtils.serviceStubProxy(getRuntimeContext(), TAG);
    }
}
