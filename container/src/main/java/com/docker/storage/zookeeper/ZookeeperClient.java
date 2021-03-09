package com.docker.storage.zookeeper;

import com.docker.storage.zookeeper.ZookeeperHandler;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

/**
 * Created by lick on 2020/4/22.
 * Description：
 */
public class ZookeeperClient {
    ZookeeperHandler client = new ZookeeperHandler();

    public void connect(String zkHost) {
        RetryNTimes retryNTimes = new RetryNTimes(5, 10000);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().connectString(zkHost)
                .sessionTimeoutMs(30000)
                .connectionTimeoutMs(15000)
                .namespace("groovycloud")
                .retryPolicy(retryNTimes)
                .build();
        client.setCuratorFramework(curatorFramework);
    }
    public void disconnect(){
        if(client != null){
            client.close();
        }
    }
    public ZookeeperHandler getClient() {
        return client;
    }
}
