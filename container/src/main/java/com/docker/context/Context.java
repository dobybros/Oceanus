package com.docker.context;

import chat.errors.CoreException;
import com.docker.context.RPCCaller;
import com.docker.context.ServiceGenerator;
import com.docker.context.config.ServerConfig;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public interface Context {
    Properties getConfig();

    ServerConfig getServerConfig();

    RPCCaller getRPCCaller();
    RPCCaller getRPCCaller(String lanId);

    ServiceGenerator getServiceGenerator();
    ServiceGenerator getServiceGenerator(String lanId);

    void injectBean(Object obj) throws CoreException;

    Collection<Class<?>> getClasses();

    List<String> getServersByService(String service) throws CoreException;
}
