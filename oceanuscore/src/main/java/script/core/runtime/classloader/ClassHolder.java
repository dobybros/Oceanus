package script.core.runtime.classloader;


public class ClassHolder {
    private Class<?> parsedClass;
    private Object cachedObject;

    public ClassHolder(Class<?> parsedClass) {
        this.parsedClass = parsedClass;
    }

    public Class<?> getParsedClass() {
        return parsedClass;
    }

    public void setParsedClass(Class<?> parsedClass) {
        this.parsedClass = parsedClass;
    }

    public Object getCachedObject() {
        return cachedObject;
    }

    public void setCachedObject(Object cachedObject) {
        this.cachedObject = cachedObject;
    }
}