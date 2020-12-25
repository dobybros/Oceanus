package script.core.runtime.groovy.object;

import chat.errors.GroovyErrorCodes;
import chat.logs.LoggerEx;
import chat.utils.ReflectionUtil;
import groovy.lang.GroovyObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.annotation.Bean;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.handler.AbstractFieldAnnotationHandler;
import chat.errors.CoreException;
import script.core.runtime.MethodInterceptor;
import script.core.runtime.classloader.ClassHolder;
import script.core.runtime.classloader.MyGroovyClassLoader;

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