package script.core.runtime;

import chat.errors.CoreException;
import script.core.runtime.groovy.object.MethodInvocation;

public interface MethodInterceptor {

    Object invoke(MethodInvocation methodInvocation) throws CoreException;
}
