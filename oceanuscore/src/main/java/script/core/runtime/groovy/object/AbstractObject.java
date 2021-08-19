package script.core.runtime.groovy.object;

import chat.errors.GroovyErrorCodes;
import chat.logs.LoggerEx;
import chat.utils.ReflectionUtil;
import oceanus.apis.CoreException;
import oceanus.sdk.rpc.interceptor.MethodInterceptor;
import oceanus.sdk.rpc.interceptor.MethodInvocation;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.classloader.ClassHolder;
import script.core.runtime.classloader.DefaultClassLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lick on 2020/12/25.
 * Description：
 */
public abstract class AbstractObject<T> {
    protected static final String TAG = GroovyObjectEx.class.getSimpleName();
    protected String classPath;
    protected AbstractRuntimeContext runtimeContext;
    protected ObjectListener objectListener;
    protected Map<String, List<MethodInterceptor>> methodInterceptorMap;
    private boolean isFill = false;
    AbstractObject(String classPath){
        this.classPath = classPath;
    }

    public void setRuntimeContext(AbstractRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public void setObjectListener(ObjectListener objectListener) {
        this.objectListener = objectListener;
    }

    public boolean isFill() {
        return isFill;
    }

    public Object invokeRootMethod(String method, Object... parameters) throws CoreException {
        String methodKey = method;//ReflectionUtil.getMethodKey(getGroovyClass(),method);
        if (methodInterceptorMap != null && methodKey != null) {
            List<MethodInterceptor> methodInterceptors = methodInterceptorMap.get(methodKey);
            if (methodInterceptors != null && !methodInterceptors.isEmpty()) {
                MethodInvocation invocation = new MethodInvocation(getObject(), getObject().getClass(), method, parameters, methodInterceptors, methodKey);
                return invocation.proceed();
            } else {
                return invokeMethod(method, parameters);
            }
        } else {
            return invokeMethod(method, parameters);
        }
    }

    public Class<T> getGroovyClass() throws CoreException {
        DefaultClassLoader classLoader = (DefaultClassLoader) runtimeContext.getCurrentClassLoader();
        if (classLoader == null)
            throw new CoreException(GroovyErrorCodes.ERROR_GROOVY_CLASSLOADERNOTFOUND, "Classloader is null");
        ClassHolder holder = classLoader.getClass(classPath);
        if (holder == null)
            throw new CoreException(GroovyErrorCodes.ERROR_GROOVY_CLASSNOTFOUND, "Groovy " + classPath + " doesn't be found in classLoader " + classLoader);
        return (Class<T>) holder.getParsedClass();
    }

    public T getObject() throws CoreException{
        isFill = true;
        return getObject(true);
    }

    public T getObject(boolean forceFill) throws CoreException {
        DefaultClassLoader classLoader = (DefaultClassLoader) runtimeContext.getCurrentClassLoader();
        if (classLoader == null)
            throw new CoreException(GroovyErrorCodes.ERROR_GROOVY_CLASSLOADERNOTFOUND, "Classloader is null");
        ClassHolder holder = classLoader.getClass(classPath);
        if (holder == null)
            throw new CoreException(GroovyErrorCodes.ERROR_GROOVY_CLASSNOTFOUND, "Groovy " + classPath + " doesn't be found in classLoader " + classLoader);

        Object object = holder.getCachedObject();
        if (object == null) {
            Class<?> groovyClass = holder.getParsedClass();
            synchronized (GroovyObjectEx.class) {
                if (groovyClass != null) {
                    try {
                        object = groovyClass.getDeclaredConstructor().newInstance();
                        holder.setCachedObject(object);
                        if (forceFill)
                            runtimeContext.injectBean(object);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw new CoreException(GroovyErrorCodes.ERROR_GROOY_NEWINSTANCE_FAILED, "New instance for class " + groovyClass + " failed " + ExceptionUtils.getFullStackTrace(e) + " in classLoader " + classLoader);
                    }
                    if (objectListener != null)
                        objectListener.objectPrepared(object);
                }
            }
        }
        return (T) object;
    }


    public void addMethodInterceptors(String key, MethodInterceptor methodInterceptor) {
        if (key != null && methodInterceptor != null) {
            if (methodInterceptorMap == null) {
                methodInterceptorMap = new HashMap<>();
            }

            if (!methodInterceptorMap.containsKey(key)) {
                List methodInterceptors = new ArrayList<MethodInterceptor>();
                methodInterceptors.add(methodInterceptor);
                methodInterceptorMap.put(key, methodInterceptors);
            } else {
                List<MethodInterceptor> methodInterceptors = methodInterceptorMap.get(key);
                if (methodInterceptors != null) {
                    methodInterceptors.add(methodInterceptor);
                }
            }
            LoggerEx.info("SCAN", "Mapping interceptor key " + key + ", value + " + methodInterceptor.getClass().getSimpleName());
        }
    }

    private String getCrc(Class clazz, String methodName, int paramCount) {
        Long crc = ReflectionUtil.getCrc(clazz, methodName);
        return crc + "" + paramCount;
    }

    public String getClassPath() {
        return classPath;
    }

    public abstract Object invokeMethod(String method, Object... parameters) throws CoreException;

    public interface ObjectListener {
        void objectPrepared(Object obj) throws CoreException;
    }
}
