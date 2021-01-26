package com.docker.context.config;

import chat.config.BaseConfiguration;
import chat.config.Configuration;
import chat.utils.IPHolder;
import com.docker.oceansbean.BeanFactory;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public class ServerConfig {

    private Integer serverPort;
    private String server;
    private String lanId;
    private String ip;
    private String service;
    private Integer version;
    private Integer rpcPort;
    private String publicDomain;

    public ServerConfig(Configuration configuration) {
        BaseConfiguration baseConfiguration = configuration.getBaseConfiguration();
        this.serverPort = baseConfiguration.getServerPort();
        this.rpcPort = baseConfiguration.getRpcPort();
        this.server = baseConfiguration.getServer();
        this.lanId = baseConfiguration.getLanId();
        IPHolder ipHolder = (IPHolder) BeanFactory.getBean(IPHolder.class.getName());
        this.ip = ipHolder.getIp();
        this.service = configuration.getService();
        this.version = configuration.getVersion();
        this.publicDomain = baseConfiguration.getPublicDomain();
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public String getServer() {
        return server;
    }

    public String getLanId() {
        return lanId;
    }

    public String getIp() {
        return ip;
    }

    public String getService() {
        return service;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getRpcPort() {
        return rpcPort;
    }

    public String getPublicDomain() {
        return publicDomain;
    }

    public void setPublicDomain(String publicDomain) {
        this.publicDomain = publicDomain;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setLanId(String lanId) {
        this.lanId = lanId;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setRpcPort(Integer rpcPort) {
        this.rpcPort = rpcPort;
    }
}
