package com.docker.rpc.remote.stub;

import com.docker.rpc.remote.stub.ServiceStubManager;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public interface ServiceStubManagerFactory {
    ServiceStubManager get(String lanId) throws CoreException;

    ServiceStubManager get() throws CoreException;
}
