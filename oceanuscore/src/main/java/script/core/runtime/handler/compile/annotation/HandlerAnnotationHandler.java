package script.core.runtime.handler.compile.annotation;

import oceanus.apis.CoreException;
import script.core.runtime.AbstractRuntimeContext;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public interface HandlerAnnotationHandler {
    void handle(AbstractRuntimeContext runtimeContext) throws CoreException;
}
