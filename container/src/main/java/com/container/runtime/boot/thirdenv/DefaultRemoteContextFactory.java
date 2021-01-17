package com.container.runtime.boot.thirdenv;

import com.docker.context.RemoteContext;
import com.docker.context.RemoteContextFactory;

/**
 * Created by lick on 2021/1/6.
 * Description：
 */
public class DefaultRemoteContextFactory implements RemoteContextFactory {
    private RemoteContext remoteContext = new DefaultRemoteContext();
    @Override
    public RemoteContext get() {
        return remoteContext;
    }
}
