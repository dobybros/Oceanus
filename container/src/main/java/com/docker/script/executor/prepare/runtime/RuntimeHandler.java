package com.docker.script.executor.prepare.runtime;

import chat.config.Configuration;
import chat.errors.CoreException;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface RuntimeHandler {
    void prepare(Configuration configuration) throws CoreException;
}
