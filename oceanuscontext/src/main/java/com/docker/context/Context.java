package com.docker.context;

import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import com.docker.context.config.ServerConfig;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Created by lick on 2020/12/23.
 * Description：
 */
public interface Context {
    int ERROR_REMOTE_SERVICE_CONNECT_FAILED = ChatErrorCodes.ERROR_RMICALL_CONNECT_FAILED;
    int ERROR_REMOTE_SERVICE_CONNECT_TIMEOUT = ChatErrorCodes.ERROR_RMICALL_TIMEOUT;

    Properties getConfig();

    ServerConfig getServerConfig();

    RPCCaller getRPCCaller();
    RPCCaller getRPCCaller(String lanId);

    ServiceGenerator getServiceGenerator();
    ServiceGenerator getServiceGenerator(String lanId);

    Object getAndCreateBean(Class<?> clazz);
    Object getAndCreateBean(String beanName, Class<?> clazz);

    void injectBean(Object obj) throws CoreException;

    Collection<Class<?>> getClasses();

    List<String> getServersByService(String service) throws CoreException;


}
