package com.container.runtime.boot.bean;

import chat.base.bean.annotation.OceanusBean;

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

}
