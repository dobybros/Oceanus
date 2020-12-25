package script;

import chat.errors.CoreException;
import lombok.Getter;
import lombok.Setter;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.classloader.ClassHolder;
import script.core.runtime.classloader.DefaultClassLoader;
import script.core.runtime.handler.AbstractClassAnnotationHandler;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public abstract class Runtime {
	protected AbstractRuntimeContext runtimeContext;
	public abstract void start() throws CoreException;
	
	public abstract void close();

	public String path(Class<?> c){return null;}
}
