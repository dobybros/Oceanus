package oceanus.sdk.rpc.remote.stub;


import oceanus.apis.CoreException;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public interface ServiceStubManagerFactory {
    ServiceStubManager get(String lanId) throws CoreException;

    ServiceStubManager get() throws CoreException;
}
