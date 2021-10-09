package com.docker.rpc;


import chat.config.BaseConfiguration;
import com.docker.oceansbean.BeanFactory;

public abstract class RPCRequest extends RPCBase {
    protected BaseConfiguration baseConfiguration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());
    public static final int MAXRETRYTIMES = 1;
    //cache local retry times; don't need to transfer to another side.
    private int retryTimes = 0;

    public RPCRequest(String type) {
        super(type);
    }

    public void retry() {
        retryTimes++;
    }

    public boolean canRetry() {
        return retryTimes < MAXRETRYTIMES;
    }


}
