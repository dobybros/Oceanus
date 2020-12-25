package com.docker.script.executor.prepare.source;

import chat.errors.CoreException;
import chat.config.Configuration;

import java.io.IOException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface ServiceDownloadHandler {
    Boolean prepare(Configuration configuration) throws CoreException, IOException;
}
