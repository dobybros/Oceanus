package script.core.runtime.handler.annotation.clazz;


import chat.errors.CoreException;
import lombok.Getter;
import lombok.Setter;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.handler.AbstractClassAnnotationHandler;

import java.util.Map;
@Setter
@Getter
public abstract class ClassAnnotationHandler extends AbstractClassAnnotationHandler {
	protected AbstractRuntimeContext runtimeContext;

	public abstract void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException;
}
