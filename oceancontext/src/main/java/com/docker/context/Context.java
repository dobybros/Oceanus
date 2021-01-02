package com.docker.context;

import chat.errors.CoreException;
import com.docker.context.config.ServerConfig;

import java.util.Properties;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public interface Context {
    public Properties getConfig();

    public ServerConfig getServerConfig();

    public <T>T getService(String service, Class<T> clazz) throws CoreException;

    public <T>T getService(String lanId, String service, Class<T> clazz) throws CoreException;

    public void injectBean(Object obj) throws CoreException ;
}
