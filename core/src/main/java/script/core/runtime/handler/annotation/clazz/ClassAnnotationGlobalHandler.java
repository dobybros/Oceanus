package script.core.runtime.handler.annotation.clazz;


import chat.errors.CoreException;
import script.Runtime;
import script.RuntimeContext;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.handler.AbstractClassAnnotationHandler;

import java.util.Map;

public abstract class ClassAnnotationGlobalHandler extends AbstractClassAnnotationHandler {
    public abstract void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap, AbstractRuntimeContext runtimeContext) throws CoreException;

    public abstract void handleAnnotatedClassesInjectBean(AbstractRuntimeContext runtimeContext);
}
