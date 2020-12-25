package script.core.runtime;

import chat.errors.CoreException;
import script.Runtime;
import script.RuntimeContext;

/**
 * Created by lick on 2019/5/15.
 * Description：
 */
public interface ParseServiceHandler {

    AbstractRuntimeContext getRuntimeContext();

    void start(ClassLoader classLoader) throws CoreException;
}
