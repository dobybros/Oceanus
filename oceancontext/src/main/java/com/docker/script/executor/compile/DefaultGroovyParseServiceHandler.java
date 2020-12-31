package com.docker.script.executor.compile;

import com.docker.script.BaseRuntimeContext;
import com.docker.utils.ScriptUtils;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.impl.GroovyParseServiceHandler;

/**
 * Created by lick on 2019/5/15.
 * Description：
 */
public class DefaultGroovyParseServiceHandler extends GroovyParseServiceHandler {
    public DefaultGroovyParseServiceHandler() {}
    public void beforeDeploy(AbstractRuntimeContext runtimeContext) {
        ScriptUtils.serviceStubProxy(runtimeContext, TAG);
    }
}
