package com.docker.context;

import chat.errors.CoreException;

/**
 * Created by lick on 2021/1/6.
 * Description：
 */
public interface RemoteContext {
    public <T>T getService(String service, Class<T> clazz) throws CoreException;

    public <T>T getService(String lanId, String service, Class<T> clazz) throws CoreException;
}
