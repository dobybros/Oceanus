package script.core.runtime.handler.compile.annotation;

import chat.errors.CoreException;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public interface HandlerAnnotationHandler {
    void handle(AbstractRuntimeContext runtimeContext) throws CoreException;
}
