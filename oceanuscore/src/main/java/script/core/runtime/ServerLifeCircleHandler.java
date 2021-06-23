package script.core.runtime;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import script.core.annotation.ServerLifeCircle;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationHandler;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerLifeCircleHandler extends ClassAnnotationHandler {
    private static final String TAG = ServerLifeCircleHandler.class.getSimpleName();
    private ConcurrentHashMap<String, GroovyObjectEx> handlerMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> startedHandlerMap = new ConcurrentHashMap<>();

    @Override
    public void handlerShutdown() {
        handlerMap.clear();
        startedHandlerMap.clear();
    }

    public ServerLifeCircleHandler() {
    }

    public void start() {
        try {
            if (handlerMap != null) {
                Collection<GroovyObjectEx> handlers = handlerMap.values();
                for (GroovyObjectEx<?> handler : handlers) {
                    if (handler != null) {
                        Long time = startedHandlerMap.get(handler.getClassPath());
                        if (time != null) {
                            LoggerEx.error(TAG, handler.getClassPath() + " has already been started");
                            continue;
                        }
                        try {
                            handler.invokeRootMethod("start");
                            time = System.currentTimeMillis();
                            startedHandlerMap.put(handler.getClassPath(), time);
                            LoggerEx.info(TAG, handler.getClassPath() + " has started at " + time);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerEx.error(TAG, "Execute start for " + handler.getClassPath() + " failed, " + t.getMessage());
                        }
                    } else {
                        LoggerEx.error(TAG, "Execute start for " + handler + " failed");
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.fatal(TAG, "Server start error " + e.getMessage());
//			System.exit(0);
        }
    }

    public void shutdown() {
        try {
            if (handlerMap != null) {
                Collection<GroovyObjectEx> handlers = handlerMap.values();
                for (GroovyObjectEx<?> handler : handlers) {
                    if (handler != null) {
                        try {
                            handler.invokeRootMethod("shutdown");
                            LoggerEx.info(TAG, handler.getClassPath() + " has shutted down");
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LoggerEx.error(TAG, "Execute shutdown for " + handler.getClassPath() + " failed, " + t.getMessage());
                        }
                    } else {
                        LoggerEx.error(TAG, "Execute shutdown for " + handler + " failed");
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.fatal(TAG, "Server start error " + e.getMessage());
//			System.exit(0);
        }
    }

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return ServerLifeCircle.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        if (annotatedClassMap != null) {
            ConcurrentHashMap<String, GroovyObjectEx> newHandlerMap = new ConcurrentHashMap<>();
            Collection<Class<?>> values = annotatedClassMap.values();
            for (Class<?> groovyClass : values) {
                GroovyObjectEx<?> groovyObj = (GroovyObjectEx<?>) getObject(null, groovyClass, runtimeContext);
                if (groovyObj != null) {
                    newHandlerMap.put(groovyClass.getName(), groovyObj);
                }
            }
            handlerMap = newHandlerMap;
            start();
        }
    }

}