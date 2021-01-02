package com.docker.context.config;

import chat.config.BaseConfiguration;
import chat.utils.ChatUtils;
import chat.utils.IPHolder;
import com.docker.utils.BeanFactory;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public class ServerConfig {

    private Integer serverPort;
    private String server;
    private String lanId;
    private String ip;

    public ServerConfig(BaseConfiguration baseConfiguration) {
        this.serverPort = baseConfiguration.getServerPort();
        this.server = baseConfiguration.getServer();
        this.lanId = baseConfiguration.getLanId();
        IPHolder ipHolder = (IPHolder) BeanFactory.getBean(IPHolder.class.getName());
        this.ip = ipHolder.getIp();
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
}
