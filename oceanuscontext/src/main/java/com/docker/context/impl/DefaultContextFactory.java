package com.docker.context.impl;

import com.docker.context.Context;
import com.docker.context.ContextFactory;
import com.docker.script.BaseRuntimeContext;
import script.RuntimeContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public class DefaultContextFactory implements ContextFactory {
    private final Map<RuntimeContext, Context> runtimeContextContextMap = new ConcurrentHashMap<>();
    @Override
    public Context get(RuntimeContext runtimeContext) {
        Context context = runtimeContextContextMap.get(runtimeContext);
        if(context == null){
            context = new ServiceContext((BaseRuntimeContext) runtimeContext);
            Context oldContext = runtimeContextContextMap.putIfAbsent(runtimeContext, context);
            if(oldContext != null){
                context = oldContext;
            }
        }
        return context;
    }
}
