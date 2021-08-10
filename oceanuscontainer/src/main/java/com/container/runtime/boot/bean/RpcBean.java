package com.container.runtime.boot.bean;

import chat.base.bean.annotation.OceanusBean;
import com.docker.rpc.impl.RMIServerHandler;

/**
 * @Auther: lick
 * @Description:
 * @Date:2019/5/26 16:50
 */
@OceanusBean
public class RpcBean {
    private BeanApp instance;

    public RpcBean() {
        instance = BeanApp.getInstance();
    }

    @OceanusBean
    public com.docker.rpc.impl.RMIServerImplWrapper dockerRpcServer() {
        return instance.getDockerRpcServer();
    }

    @OceanusBean
    public RMIServerHandler dockerRpcServerAdapter() {
        return instance.getDockerRpcServerAdapter();
    }

}
