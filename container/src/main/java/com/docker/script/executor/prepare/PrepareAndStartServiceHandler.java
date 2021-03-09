package com.docker.script.executor.prepare;

import chat.config.Configuration;
import com.docker.script.executor.prepare.PrepareAndStartServiceProcessListener;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface PrepareAndStartServiceHandler {
    void prepareAndStart(Configuration configuration, PrepareAndStartServiceProcessListener prepareAndStartServiceProcessHandler) throws Throwable;
}
