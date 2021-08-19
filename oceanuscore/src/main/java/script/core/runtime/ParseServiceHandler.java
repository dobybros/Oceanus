package script.core.runtime;

import oceanus.apis.CoreException;

/**
 * Created by lick on 2019/5/15.
 * Description：
 */
public interface ParseServiceHandler {

    void start(ClassLoader classLoader) throws CoreException;
}
