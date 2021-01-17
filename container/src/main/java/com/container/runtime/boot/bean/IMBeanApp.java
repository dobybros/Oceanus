package com.container.runtime.boot.bean;

import com.dobybros.chat.handlers.*;
import com.dobybros.chat.handlers.imextention.IMExtensionCache;
import com.dobybros.chat.services.impl.ConsumeQueueService;
import com.dobybros.chat.tasks.OfflineMessageSavingTask;
import com.dobybros.chat.tasks.RPCMessageSendingTask;
import com.dobybros.gateway.channels.tcp.UpStreamHandler;
import com.dobybros.gateway.channels.tcp.codec.HailProtocalCodecFactory;
import com.dobybros.gateway.channels.websocket.codec.WebSocketCodecFactory;
import com.dobybros.gateway.eventhandler.MessageEventHandler;
import com.dobybros.gateway.onlineusers.impl.OnlineUserManagerImpl;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.KeyStoreFactory;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptorEx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

//import com.dobybros.chat.log.LogIndexQueue;
//import com.dobybros.chat.storage.mongodb.daos.BulkLogDAO;

/**
 * @Auther: lick
 * @Description:
 * @Date:2019/5/26 15:41
 */
public class IMBeanApp extends ContextBeanApp {
    protected UpStreamHandler upstreamHandler;
    protected ProtocolCodecFilter tcpCodecFilter;
    protected DefaultIoFilterChainBuilder tcpFilterChainBuilder;
    protected NioSocketAcceptorEx tcpIoAcceptor;
    protected ProtocolCodecFilter sslTcpCodecFilter;
    protected HailProtocalCodecFactory hailProtocalCodecFactory;
    protected KeyStoreFactory keystoreFactory;
    protected SslContextFactory sslContextFactory;
    protected SslFilter sslFilter;
    protected DefaultIoFilterChainBuilder sslTcpFilterChainBuilder;
    protected NioSocketAcceptorEx sslTcpIoAcceptor;
    protected WebSocketCodecFactory webSocketCodecFactory;
    protected ProtocolCodecFilter wsCodecFilter;
    protected DefaultIoFilterChainBuilder wsFilterChainBuilder;
    protected NioSocketAcceptorEx wsIoAcceptor;
    protected ConsumeQueueService bulkLogQueueService;
    protected ConsumeOfflineMessageHandler consumeOfflineMessageHandler;
    protected OfflineMessageSavingTask offlineMessageSavingTask;
    protected RPCMessageSendingTask messageSendingTask;
    protected OnlineUserManagerImpl onlineUserManager;
    protected MessageEventHandler messageEventHandler;
    protected PingHandler pingHandler;
    protected IMExtensionCache imExtensionCache;
    protected ProxyContainerDuplexSender proxyContainerDuplexSender;
    protected RpcProxyContainerDuplexSender rpcProxyContainerDuplexSender;
    protected QueueProxyContainerDuplexSender queueProxyContainerDuplexSender;

    public synchronized IMExtensionCache getIMExtensionCache() {
        if(imExtensionCache == null){
            imExtensionCache = new IMExtensionCache();
            imExtensionCache.setHost(baseConfiguration.getRedisHost());
        }
        return imExtensionCache;
    }
    public synchronized PingHandler getPingHandler() {
        if(pingHandler == null){
            pingHandler = new PingHandler();
            pingHandler.setUseProxy(baseConfiguration.getUseProxy());
        }
        return pingHandler;
    }

    public synchronized MessageEventHandler getMessageEventHandler() {
        if(messageEventHandler == null){
            messageEventHandler = new MessageEventHandler();
            messageEventHandler.setOnlineUserManager(getOnlineUserManager());
            messageEventHandler.setMessageSendingTask(getMessageSendingTask());
            messageEventHandler.setOfflineMessageSavingTask(getOfflineMessageSavingTask());
        }
        return messageEventHandler;
    }

    public synchronized OnlineUserManagerImpl getOnlineUserManager() {
        if(onlineUserManager == null){
            onlineUserManager = new OnlineUserManagerImpl();
            onlineUserManager.setMessageSendingTask(getMessageSendingTask());
            onlineUserManager.setOfflineMessageSavingTask(getOfflineMessageSavingTask());
            onlineUserManager.setMessageEventHandler(getMessageEventHandler());
            onlineUserManager.setOnlineServer(getOnlineServer());
        }
        return onlineUserManager;
    }

    public synchronized RPCMessageSendingTask getMessageSendingTask() {
        if(messageSendingTask == null){
            messageSendingTask = new RPCMessageSendingTask();
            messageSendingTask.setNumOfThreads(4);
            messageSendingTask.setOfflineMessageSavingTask(getOfflineMessageSavingTask());
            messageSendingTask.setOnlineServer(getOnlineServer());
        }
        return messageSendingTask;
    }
    public synchronized ProxyContainerDuplexSender getProxyContainerDuplexSender() {
        if (proxyContainerDuplexSender == null) {
            proxyContainerDuplexSender = new ProxyContainerDuplexSender();
            proxyContainerDuplexSender.setDockerStatusService(getDockerStatusService());
            proxyContainerDuplexSender.setRpcProxyContainerDuplexSender(getRpcProxyContainerDuplexSender());
        }
        return proxyContainerDuplexSender;
    }
    public synchronized RpcProxyContainerDuplexSender getRpcProxyContainerDuplexSender() {
        if (rpcProxyContainerDuplexSender == null) {
            rpcProxyContainerDuplexSender = new RpcProxyContainerDuplexSender();
        }
        return rpcProxyContainerDuplexSender;
    }
    public synchronized QueueProxyContainerDuplexSender getQueueProxyContainerDuplexSender() {
        if (queueProxyContainerDuplexSender == null) {
            queueProxyContainerDuplexSender = new QueueProxyContainerDuplexSender();
        }
        return queueProxyContainerDuplexSender;
    }
    public synchronized OfflineMessageSavingTask getOfflineMessageSavingTask() {
        if(offlineMessageSavingTask == null){
            offlineMessageSavingTask = new OfflineMessageSavingTask();
        }
        return offlineMessageSavingTask;
    }

    public synchronized ConsumeOfflineMessageHandler getConsumeOfflineMessageHandler() {
        if(consumeOfflineMessageHandler == null){
            consumeOfflineMessageHandler = new ConsumeOfflineMessageHandler();
            consumeOfflineMessageHandler.setMessageSendingTask(getMessageSendingTask());
        }
        return consumeOfflineMessageHandler;
    }

    public synchronized ConsumeQueueService getBulkLogQueueService() {
        if(bulkLogQueueService == null){
            bulkLogQueueService = new ConsumeQueueService();
        }
        return bulkLogQueueService;
    }

    public synchronized NioSocketAcceptorEx getWsIoAcceptor() {
        if(wsIoAcceptor == null){
            wsIoAcceptor = new NioSocketAcceptorEx();
            wsIoAcceptor.setHandler(getUpstreamHandler());
            wsIoAcceptor.setFilterChainBuilder(getWsFilterChainBuilder());
            wsIoAcceptor.setReuseAddress(true);
            wsIoAcceptor.setDefaultLocalAddress(new InetSocketAddress(Integer.valueOf(baseConfiguration.getUpstreamWsPort())));
        }
        return wsIoAcceptor;
    }

    public synchronized DefaultIoFilterChainBuilder getWsFilterChainBuilder() {
        if(wsFilterChainBuilder == null){
            wsFilterChainBuilder = new DefaultIoFilterChainBuilder();
            Map map = new LinkedHashMap();
//            map.put("sslFilter", getSslFilter());
            map.put("codecFilter", getWsCodecFilter());
            wsFilterChainBuilder.setFilters(map);
        }
        return wsFilterChainBuilder;
    }

    public synchronized ProtocolCodecFilter getWsCodecFilter() {
        if(wsCodecFilter == null){
            wsCodecFilter = new ProtocolCodecFilter(getWebSocketCodecFactory());
        }
        return wsCodecFilter;
    }

    public synchronized WebSocketCodecFactory getWebSocketCodecFactory() {
        if(webSocketCodecFactory == null){
            webSocketCodecFactory = new WebSocketCodecFactory();
        }
        return webSocketCodecFactory;
    }

    public synchronized NioSocketAcceptorEx getSslTcpIoAcceptor() {
        if(sslTcpIoAcceptor == null){
            sslTcpIoAcceptor = new NioSocketAcceptorEx();
            sslTcpIoAcceptor.setHandler(getUpstreamHandler());
            sslTcpIoAcceptor.setFilterChainBuilder(getSslTcpFilterChainBuilder());
            sslTcpIoAcceptor.setReuseAddress(true);
            sslTcpIoAcceptor.setDefaultLocalAddress(new InetSocketAddress(Integer.valueOf(baseConfiguration.getUpstreamSslPort())));
        }
        return sslTcpIoAcceptor;
    }

    public DefaultIoFilterChainBuilder getSslTcpFilterChainBuilder() {
        if(sslTcpFilterChainBuilder == null){
            sslTcpFilterChainBuilder = new DefaultIoFilterChainBuilder();
            Map map = new LinkedHashMap();
            map.put("codecFilter", getSslTcpCodecFilter());
            map.put("sslFilter", getSslFilter());
            sslTcpFilterChainBuilder.setFilters(map);
        }
        return sslTcpFilterChainBuilder;
    }

    public synchronized SslFilter getSslFilter() {
        if(sslFilter == null){
            try {
                sslFilter = new SslFilter(getSslContextFactory().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslFilter;
    }

    public synchronized SslContextFactory getSslContextFactory() {
        if(sslContextFactory == null){
            sslContextFactory = new SslContextFactory();
            try {
                sslContextFactory.setKeyManagerFactoryKeyStore(getKeystoreFactory().newInstance());
                sslContextFactory.setProtocol("TLSV1.2");
                sslContextFactory.setKeyManagerFactoryAlgorithm("SunX509");
                sslContextFactory.setKeyManagerFactoryKeyStorePassword(baseConfiguration.getKeymanagerPwd());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslContextFactory;
    }

    public synchronized KeyStoreFactory getKeystoreFactory() {
        if(keystoreFactory == null){
            keystoreFactory = new KeyStoreFactory();
            keystoreFactory.setPassword(baseConfiguration.getKeystorePwd());
            URL keystorePathUrl = null;
            try {
                keystorePathUrl = new URL(baseConfiguration.getKeystorePath());
                keystoreFactory.setDataUrl(keystorePathUrl);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return keystoreFactory;
    }

    public synchronized ProtocolCodecFilter getSslTcpCodecFilter() {
        if(sslTcpCodecFilter == null){
            sslTcpCodecFilter = new ProtocolCodecFilter(getHailProtocalCodecFactory());
        }
        return sslTcpCodecFilter;
    }

    public synchronized NioSocketAcceptorEx getTcpIoAcceptor() {
        if(tcpIoAcceptor == null){
            tcpIoAcceptor = new NioSocketAcceptorEx();
            tcpIoAcceptor.setHandler(getUpstreamHandler());
            tcpIoAcceptor.setFilterChainBuilder(getTcpFilterChainBuilder());
            tcpIoAcceptor.setReuseAddress(true);
            tcpIoAcceptor.setDefaultLocalAddress(new InetSocketAddress(Integer.valueOf(baseConfiguration.getUpstreamPort())));
        }
        return tcpIoAcceptor;
    }

    public synchronized UpStreamHandler getUpstreamHandler() {
        if(upstreamHandler == null){
            upstreamHandler = new UpStreamHandler();
            upstreamHandler.setReadIdleTime(720);
            upstreamHandler.setWriteIdleTime(720);
        }
        return upstreamHandler;
    }

    public synchronized HailProtocalCodecFactory getHailProtocalCodecFactory() {
        if(hailProtocalCodecFactory == null){
            hailProtocalCodecFactory = new HailProtocalCodecFactory();
        }
        return hailProtocalCodecFactory;
    }

    public synchronized ProtocolCodecFilter getTcpCodecFilter() {
        if(tcpCodecFilter == null){
            tcpCodecFilter = new ProtocolCodecFilter(getHailProtocalCodecFactory());
        }
        return tcpCodecFilter;
    }

    public synchronized DefaultIoFilterChainBuilder getTcpFilterChainBuilder() {
        if(tcpFilterChainBuilder == null){
            tcpFilterChainBuilder = new DefaultIoFilterChainBuilder();
            Map map = new LinkedHashMap();
            map.put("codecFilter", getTcpCodecFilter());
            tcpFilterChainBuilder.setFilters(map);
        }
        return tcpFilterChainBuilder;
    }
}
