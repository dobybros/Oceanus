package script.core.runtime.handler.annotation.clazz;


import oceanus.apis.CoreException;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.handler.AbstractClassAnnotationHandler;

import java.util.Map;
public abstract class ClassAnnotationHandler extends AbstractClassAnnotationHandler {
	protected AbstractRuntimeContext runtimeContext;

	public abstract void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException;

	public AbstractRuntimeContext getRuntimeContext() {
		return runtimeContext;
	}

	public void setRuntimeContext(AbstractRuntimeContext runtimeContext) {
		this.runtimeContext = runtimeContext;
	}
}
