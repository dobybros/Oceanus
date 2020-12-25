package script.core.runtime.handler.compile.annotation.impl;

import chat.logs.LoggerEx;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.handler.AbstractClassAnnotationHandler;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationGlobalHandler;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;
import script.core.runtime.handler.compile.annotation.HandlerAnnotationHandler;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public class DefaultHandlerAnnotationHandler implements HandlerAnnotationHandler {
    private final String TAG = HandlerAnnotationHandler.class.getSimpleName();
    @Override
    public void handle(AbstractRuntimeContext runtimeContext) {
        final Map<AbstractClassAnnotationHandler, Map<String, Class<?>>> handlerMap = new ConcurrentHashMap<>();
        for (Class clazz : runtimeContext.getAllClasses().values()) {
            LoggerEx.info(TAG, "Loaded class " + clazz.getName());
            if (!runtimeContext.getAnnotationHandlerMap().isEmpty()) {
                for (AbstractClassAnnotationHandler handler : runtimeContext.getAnnotationHandlerMap().values()) {
                    Class<? extends Annotation> annotationClass = handler.handleAnnotationClass();
                    if (annotationClass != null) {
                        Annotation annotation = clazz.getAnnotation(annotationClass);
                        if (annotation != null) {
                            Map<String, Class<?>> classes = handlerMap.computeIfAbsent(handler, k -> new HashMap<>());
                            classes.put(clazz.getName(), clazz);
                        }
                    }
                }
            }
        }
        for (AbstractClassAnnotationHandler handler : handlerMap.keySet()){
            try {
                if(handler instanceof ClassAnnotationHandler){
                    ((ClassAnnotationHandler)handler).handleAnnotatedClasses(handlerMap.get(handler));
                }else if(handler instanceof ClassAnnotationGlobalHandler){
                    ((ClassAnnotationGlobalHandler)handler).handleAnnotatedClasses(handlerMap.get(handler), runtimeContext);
                }
            }catch (Throwable t) {
                        t.printStackTrace();
                        LoggerEx.fatal(TAG, "Handle annotated classes failed, " + handlerMap.get(handler) + " the handler " + handler
                                        + " is ignored!errMsg: " + ExceptionUtils.getFullStackTrace(t));
            }
        }
    }
}
