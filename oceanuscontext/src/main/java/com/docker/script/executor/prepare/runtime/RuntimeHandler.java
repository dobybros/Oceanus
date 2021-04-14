package com.docker.script.executor.prepare.runtime;

import chat.config.Configuration;
import chat.errors.CoreException;
import com.docker.script.BaseRuntimeContext;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface RuntimeHandler {
    BaseRuntimeContext prepare(Configuration configuration) throws CoreException;
}
