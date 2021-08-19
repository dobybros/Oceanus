package oceanus.sdk.rpc.interceptor;

import oceanus.apis.CoreException;

public interface MethodInterceptor {

    Object invoke(MethodInvocation methodInvocation) throws CoreException;
}
