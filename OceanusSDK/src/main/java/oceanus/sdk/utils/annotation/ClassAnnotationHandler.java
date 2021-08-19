package oceanus.sdk.utils.annotation;

import oceanus.apis.CoreException;
import org.reflections.Reflections;

public abstract class ClassAnnotationHandler  {
	protected Reflections reflections;
	public abstract void handle() throws CoreException;
	public Object getKey() {
		return this.getClass();
	}

	public void setReflections(Reflections reflections) {
		this.reflections = reflections;
	}
}
