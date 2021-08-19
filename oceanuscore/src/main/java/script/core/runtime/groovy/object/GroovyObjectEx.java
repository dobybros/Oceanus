package script.core.runtime.groovy.object;

import groovy.lang.GroovyObject;
import oceanus.apis.CoreException;

public class GroovyObjectEx<T> extends AbstractObject<T>{
    public GroovyObjectEx(String classPath) {
        super(classPath);
    }

    public Object invokeMethod(String method, Object... parameters) throws CoreException {
        T obj = getObject();
        if (obj != null && obj instanceof GroovyObject) {
            GroovyObject gObj = (GroovyObject) obj;
            //TODO Bind GroovyClassLoader base on current thread.
            return gObj.invokeMethod(method, parameters);
        }
        return null;
    }
}