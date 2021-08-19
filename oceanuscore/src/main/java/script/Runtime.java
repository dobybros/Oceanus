package script;

import oceanus.apis.CoreException;
import script.core.runtime.AbstractRuntimeContext;

public abstract class Runtime {
	protected AbstractRuntimeContext runtimeContext;
	public abstract void start() throws CoreException;
	
	public abstract void close();

	public String path(Class<?> c){return null;}
}
