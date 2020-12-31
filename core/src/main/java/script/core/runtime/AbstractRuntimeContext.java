package script.core.runtime;

import chat.config.Configuration;
import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.Runtime;
import script.RuntimeContext;
import script.core.runtime.classloader.ClassHolder;
import script.core.runtime.classloader.DefaultClassLoader;
import script.core.runtime.handler.AbstractClassAnnotationHandler;
import script.core.runtime.handler.AbstractFieldAnnotationHandler;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
@Getter
@Setter
public abstract class AbstractRuntimeContext implements RuntimeContext {
    private final String TAG = AbstractRuntimeContext.class.getSimpleName();
    public AbstractRuntimeContext(Configuration configuration) throws CoreException {
        try {
            this.configuration = configuration;
            Class<?> runtimeFactoryClass = Class.forName("com.docker.script.bean.Default" + (configuration.getLanguageType().equals(Configuration.LANGEUAGE_JAVA_JAR) ? Configuration.LANGEUAGE_JAVA : configuration.getLanguageType()) + RuntimeBeanFactory.class.getSimpleName());
            this.runtimeBeanFactory = (RuntimeBeanFactory) runtimeFactoryClass.getDeclaredConstructor(AbstractRuntimeContext.class).newInstance((AbstractRuntimeContext)this);
        }catch (Throwable t){
            throw new CoreException(ChatErrorCodes.ERROR_REFLECT, "Init runtimeBeanFactory failed");
        }
    }
    protected Runtime runtime;
    protected Configuration configuration;
    protected RuntimeBeanFactory runtimeBeanFactory;
    protected ClassLoader currentClassLoader;
    protected Map<Object, AbstractClassAnnotationHandler> annotationHandlerMap = new ConcurrentHashMap<>();

    protected Map<String, ClassHolder> cachedClasses = new HashMap<>();

    protected Map<String, Class<?>> allClasses = new HashMap<>();

    private List<AbstractFieldAnnotationHandler> fieldAnnotationHandlers = new ArrayList<>();
    public void addFieldAnnotationHandler(AbstractFieldAnnotationHandler handler) {
        if (fieldAnnotationHandlers == null) {
            fieldAnnotationHandlers = new ArrayList<>();
        }
        if (!fieldAnnotationHandlers.contains(handler)){
            handler.setRuntimeContext(this);
            fieldAnnotationHandlers.add(handler);
        }
    }

    public void addClassAnnotationHandler(AbstractClassAnnotationHandler handler) {
        if(handler != null){
            if(handler instanceof ClassAnnotationHandler){
                ((ClassAnnotationHandler)handler).setRuntimeContext(this);
            }
            this.annotationHandlerMap.putIfAbsent(handler.getKey(), handler);
        }
    }

    public AbstractClassAnnotationHandler getClassAnnotationHandler(Object key) {
        return this.annotationHandlerMap.get(key);
    }
    public void addClass(ClassHolder classHolder){
        cachedClasses.put(classHolder.getParsedClass().getName(), classHolder);
        allClasses.put(classHolder.getParsedClass().getName(), classHolder.getParsedClass());
    }

    public void injectAllBean() throws CoreException {
        this.runtimeBeanFactory.fillAllObject();
    }

    public void injectBean(Object object) throws CoreException {
        this.runtimeBeanFactory.fillObject(object);
    }

    public Class<?> getClass(String classStr) {
        if (StringUtils.isBlank(classStr))
            return null;

        ClassHolder holder = ((DefaultClassLoader)this.getCurrentClassLoader()).getClass(classStr);
        if (holder != null) {
            return holder.getParsedClass();
        }
        return null;
    }
    @Override
    public void close(){
        for (AbstractClassAnnotationHandler annotationHandler : this.getAnnotationHandlerMap().values()) {
            try {
                annotationHandler.handlerShutdown();
            } catch (Throwable t) {
                t.printStackTrace();
                LoggerEx.fatal(TAG,
                        "Handle annotated classes shutdown failed "
                                + " the handler " + annotationHandler + " error " + ExceptionUtils.getFullStackTrace(t));
            }
        }
        this.annotationHandlerMap.clear();
        this.fieldAnnotationHandlers.clear();
        this.cachedClasses.clear();
        this.allClasses.clear();
        if(this.runtimeBeanFactory != null){
            this.runtimeBeanFactory.close();
        }
        if(this.runtime != null){
            this.runtime.close();
        }
    }
}
