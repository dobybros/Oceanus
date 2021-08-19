package script.core.runtime.handler.compile.classloader;

import oceanus.apis.CoreException;
import script.core.runtime.AbstractRuntimeContext;

/**
 * Created by lick on 2020/12/21.
 * Description：
 */
public interface PrepareClassLoaderHandler {
    ClassLoader prepare(AbstractRuntimeContext runtimeContext) throws CoreException;
}
