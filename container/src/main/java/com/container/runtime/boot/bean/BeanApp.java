package com.container.runtime.boot.bean;

import com.container.im.ProxyUpStreamAnnotationHandler;
import com.container.runtime.executor.DefaultRuntimeExecutor;
import com.container.runtime.boot.manager.BootManager;
import com.container.im.ProxyAnnotationHandler;
import com.container.im.ProxyUpStreamHandler;
import com.docker.script.executor.prepare.config.BaseConfigurationBuilder;
import com.docker.oceansbean.BeanFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptorEx;

import java.net.InetSocketAddress;

/**
 * @author lick
 * @date 2019/11/12
 */
public class BeanApp extends IMBeanApp {
    public static volatile BeanApp instance;
    private ProxyUpStreamHandler proxyUpStreamHandler;
    private ProxyAnnotationHandler proxyAnnotationHandler;
    private NioSocketAcceptorEx wsIoAcceptor;
    private NioSocketAcceptorEx sslTcpIoAcceptor;
    private NioSocketAcceptorEx tcpIoAcceptor;
    private BootManager bootManager;
    private ProxyUpStreamAnnotationHandler proxyUpStreamAnnotationHandler;

    public synchronized NioSocketAcceptorEx getTcpIoAcceptor() {
        if(tcpIoAcceptor == null){
            tcpIoAcceptor = new NioSocketAcceptorEx();
            tcpIoAcceptor.setHandler(getProxyUpStreamHandler());
            tcpIoAcceptor.setFilterChainBuilder(getTcpFilterChainBuilder());
            tcpIoAcceptor.setReuseAddress(true);
            tcpIoAcceptor.setDefaultLocalAddress(new InetSocketAddress(baseConfiguration.getUpstreamPort()));
        }
        return tcpIoAcceptor;
    }
    public synchronized NioSocketAcceptorEx getSslTcpIoAcceptor() {
        if(sslTcpIoAcceptor == null){
            sslTcpIoAcceptor = new NioSocketAcceptorEx();
            sslTcpIoAcceptor.setHandler(getProxyUpStreamHandler());
            sslTcpIoAcceptor.setFilterChainBuilder(getSslTcpFilterChainBuilder());
            sslTcpIoAcceptor.setReuseAddress(true);
            sslTcpIoAcceptor.setDefaultLocalAddress(new InetSocketAddress(baseConfiguration.getUpstreamSslPort()));
        }
        return sslTcpIoAcceptor;
    }
    public synchronized BootManager getBootManager() {
        if (bootManager == null) {
            bootManager = new BootManager();
            bootManager.setBaseConfiguration(baseConfiguration);
            bootManager.setRuntimeExecutor(new DefaultRuntimeExecutor());
            bootManager.setDockerStatusService(getDockerStatusService());
            bootManager.setDeployServiceVersionService(getDeployServiceVersionService());
            bootManager.setServiceVersionService(getServiceVersionService());
        }
        return bootManager;
    }
    public synchronized NioSocketAcceptorEx getWsIoAcceptor() {
        if(wsIoAcceptor == null){
            wsIoAcceptor = new NioSocketAcceptorEx();
            wsIoAcceptor.setHandler(getProxyUpStreamHandler());
            wsIoAcceptor.setFilterChainBuilder(getWsFilterChainBuilder());
            wsIoAcceptor.setReuseAddress(true);
            wsIoAcceptor.setDefaultLocalAddress(new InetSocketAddress(baseConfiguration.getUpstreamWsPort()));
        }
        return wsIoAcceptor;
    }

    public synchronized ProxyUpStreamHandler getProxyUpStreamHandler() {
        if (proxyUpStreamHandler == null) {
            proxyUpStreamHandler = new ProxyUpStreamHandler();
            proxyUpStreamHandler.setReadIdleTime(720);
            proxyUpStreamHandler.setWriteIdleTime(720);
            proxyUpStreamHandler.setProxyAnnotationHandler(getProxyAnnotationHandler());
            proxyUpStreamHandler.setProxyUpStreamAnnotationHandler(getProxyUpStreamAnnotationHandler());
        }
        return proxyUpStreamHandler;
    }
    public synchronized ProxyUpStreamAnnotationHandler getProxyUpStreamAnnotationHandler(){
        if(proxyUpStreamAnnotationHandler == null){
            proxyUpStreamAnnotationHandler = new ProxyUpStreamAnnotationHandler();
        }
        return proxyUpStreamAnnotationHandler;
    }
    public synchronized ProxyAnnotationHandler getProxyAnnotationHandler() {
        if (proxyAnnotationHandler == null) {
            proxyAnnotationHandler = new ProxyAnnotationHandler();
        }
        return proxyAnnotationHandler;
    }

    public static synchronized BeanApp getInstance(){
        if(instance == null){
            synchronized (BeanApp.class){
                if(instance == null){
                    instance = new BeanApp();
                    baseConfiguration = new BaseConfigurationBuilder().build();
                    BeanFactory.init(baseConfiguration);
                }
            }
        }
        return instance;
    }
}
