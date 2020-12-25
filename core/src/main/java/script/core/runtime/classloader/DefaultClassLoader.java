package script.core.runtime.classloader;

import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;

/**
 * Created by lick on 2020/12/21.
 * Description：
 */
public interface DefaultClassLoader {
    public AbstractRuntimeContext getRuntimeContext();

    public ClassHolder getClass(String className);
}
