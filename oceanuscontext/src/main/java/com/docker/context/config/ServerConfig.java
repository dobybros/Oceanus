package com.docker.context.config;

import com.docker.oceansbean.BeanFactory;

import chat.config.BaseConfiguration;
import chat.config.Configuration;
import chat.utils.IPHolder;

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
    private String scaleInstanceId;
    private String publicDomain;
    private String localPath;
    private String discoveryHost;

    public ServerConfig(Configuration configuration) {
        BaseConfiguration baseConfiguration = configuration.getBaseConfiguration();
        this.serverPort = baseConfiguration.getServerPort();
        this.server = baseConfiguration.getServer();
        this.lanId = baseConfiguration.getLanId();
        IPHolder ipHolder = (IPHolder) BeanFactory.getBean(IPHolder.class.getName());
        this.ip = ipHolder.getIp();
        this.service = configuration.getService();
        this.version = configuration.getVersion();
        this.rpcPort = baseConfiguration.getRpcPort();
        this.scaleInstanceId = baseConfiguration.getScaleInstanceId();
        this.publicDomain = baseConfiguration.getPublicDomain();
        this.localPath = configuration.getLocalPath();
        this.discoveryHost = baseConfiguration.getDiscoveryHost();
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

    public void setScaleInstanceId(String scaleInstanceId) {
        this.scaleInstanceId = scaleInstanceId;
    }

    public void setPublicDomain(String publicDomain) {
        this.publicDomain = publicDomain;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
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

    public String getScaleInstanceId() {
        return scaleInstanceId;
    }

    public Integer getRpcPort() {
        return rpcPort;
    }

    public String getPublicDomain() {
        return publicDomain;
    }

    public String getDiscoveryHost() {
        return discoveryHost;
    }

    public void setDiscoveryHost(String discoveryHost) {
        this.discoveryHost = discoveryHost;
    }
}
