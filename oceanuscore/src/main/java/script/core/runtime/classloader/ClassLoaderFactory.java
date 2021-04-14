package script.core.runtime.classloader;

import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;

import java.net.URL;

/**
 * Created by lick on 2020/12/21.
 * Description：
 */
public interface ClassLoaderFactory {
    ClassLoader create(URL[] urls, ClassLoader parentClassLoader, boolean isDependency, AbstractRuntimeContext runtimeContext);
}
