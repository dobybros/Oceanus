package com.container.runtime.boot.bean;

import com.container.im.ProxyAnnotationHandler;
import com.container.im.ProxyUpStreamAnnotationHandler;
import com.container.im.ProxyUpStreamHandler;
import chat.base.bean.annotation.OceanusBean;
import com.dobybros.chat.handlers.ProxyContainerDuplexSender;
import com.dobybros.chat.handlers.RpcProxyContainerDuplexSender;
import com.dobybros.gateway.channels.tcp.codec.HailProtocalCodecFactory;
import com.dobybros.gateway.eventhandler.MessageEventHandler;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.KeyStoreFactory;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptorEx;
import org.springframework.beans.factory.config.CustomEditorConfigurer;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther: lick
 * @Description:
 * @Date:2019/5/26 17:37
 */
@OceanusBean
public class TcpBean {
    private BeanApp instance;
    public TcpBean(){
        instance = BeanApp.getInstance();
    }
    @OceanusBean
    public ProxyUpStreamAnnotationHandler proxyUpStreamAnnotationHandler() {
        return instance.getProxyUpStreamAnnotationHandler();
    }

    @OceanusBean
    public ProxyUpStreamHandler proxyUpStreamHandler() {
        return instance.getProxyUpStreamHandler();
    }

    @OceanusBean
    public CustomEditorConfigurer customEditorConfigurer() {
        CustomEditorConfigurer customEditorConfigurer = new CustomEditorConfigurer();
        Map<Class<?>, Class<? extends PropertyEditor>> map = new HashMap<>();
        map.put(java.net.SocketAddress.class, org.apache.mina.integration.beans.InetSocketAddressEditor.class);
        customEditorConfigurer.setCustomEditors(map);
        return customEditorConfigurer;
    }

    @OceanusBean
    public ProtocolCodecFilter tcpCodecFilter() {
        return instance.getTcpCodecFilter();
    }

    //TODO 检查
    @OceanusBean
    public DefaultIoFilterChainBuilder tcpFilterChainBuilder() {
        return instance.getTcpFilterChainBuilder();
    }

    @OceanusBean
    public NioSocketAcceptorEx tcpIoAcceptor() {
        return instance.getTcpIoAcceptor();
    }

    @OceanusBean
    public HailProtocalCodecFactory hailProtocalCodecFactory() {
        return instance.getHailProtocalCodecFactory();
    }

    @OceanusBean
    public ProtocolCodecFilter sslTcpCodecFilter() {
        return instance.getSslTcpCodecFilter();
    }

    @OceanusBean
    public KeyStoreFactory keystoreFactory() {
        return instance.getKeystoreFactory();
    }
    @OceanusBean
    public SslContextFactory sslContextFactory() {
        return instance.getSslContextFactory();
    }
    @OceanusBean
    public SslFilter sslFilter() {
        return instance.getSslFilter();
    }
    @OceanusBean
    public DefaultIoFilterChainBuilder sslTcpFilterChainBuilder() {
        return instance.getSslTcpFilterChainBuilder();
    }
    @OceanusBean
    public NioSocketAcceptorEx sslTcpIoAcceptor() {
        return instance.getSslTcpIoAcceptor();
    }
    @OceanusBean
    public ProtocolCodecFilter wsCodecFilter() {
        return instance.getWsCodecFilter();
    }
    @OceanusBean
    public DefaultIoFilterChainBuilder wsFilterChainBuilder() {
        return instance.getWsFilterChainBuilder();
    }
    @OceanusBean
    public NioSocketAcceptorEx wsIoAcceptor() {
        return instance.getWsIoAcceptor();
    }
    @OceanusBean
    public MessageEventHandler messageEventHandler(){
        return instance.getMessageEventHandler();
    }
    @OceanusBean
    public ProxyAnnotationHandler proxyAnnotationHandler(){
        return instance.getProxyAnnotationHandler();
    }
    @OceanusBean
    public ProxyContainerDuplexSender proxyContainerDuplexSender(){
        return instance.getProxyContainerDuplexSender();
    }
    @OceanusBean
    public RpcProxyContainerDuplexSender rpcProxyContainerDuplexSender(){
        return instance.getRpcProxyContainerDuplexSender();
    }
}