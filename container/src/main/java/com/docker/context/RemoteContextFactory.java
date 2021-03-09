package com.docker.context;

import com.docker.context.RemoteContext;

/**
 * Created by lick on 2021/1/6.
 * Description：
 */
public interface RemoteContextFactory {
    public RemoteContext get();
}
