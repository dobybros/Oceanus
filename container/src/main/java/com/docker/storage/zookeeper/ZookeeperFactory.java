package com.docker.storage.zookeeper;

import chat.logs.LoggerEx;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/4/22.
 * Description：
 */
public class ZookeeperFactory {
    private final String TAG = ZookeeperFactory.class.getSimpleName();
    private Map<String, ZookeeperClient> zookeeperHandlerMap = new ConcurrentHashMap<>();

    public ZookeeperHandler get(String zkHost) {
        if (StringUtils.isNotBlank(zkHost)) {
            ZookeeperClient zookeeperHandler = zookeeperHandlerMap.get(zkHost);
            if (zookeeperHandler == null) {
                zookeeperHandler = new ZookeeperClient();
                ZookeeperClient oldZookeeperHandler = zookeeperHandlerMap.putIfAbsent(zkHost, zookeeperHandler);
                if (oldZookeeperHandler != null) {
                    zookeeperHandler = oldZookeeperHandler;
                } else {
                    zookeeperHandler.connect(zkHost);
                    LoggerEx.info(TAG, "Zookeeper connect, zkHost: " + zkHost);
                }
            }
            return zookeeperHandler.getClient();
        } else {
            LoggerEx.error(TAG, "Cant getZookeeperHandler, zkHost is null");
        }
        return null;
    }

    void disconnect(){
        for (ZookeeperClient zookeeperHandler : zookeeperHandlerMap.values()){
            try {
                zookeeperHandler.disconnect();
            }catch (Throwable t){
                LoggerEx.error(TAG, t.getMessage());
            }
        }
    }
}
