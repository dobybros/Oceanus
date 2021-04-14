package com.docker.storage.redis;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.storage.redis.annotation.RedisListener;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.object.GroovyObjectEx;
import script.core.runtime.handler.annotation.clazz.ClassAnnotationGlobalHandler;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by lick on 2020/3/5.
 * Description：
 */
public class RedisListenerHandler extends ClassAnnotationGlobalHandler {
    private final String TAG = RedisListenerHandler.class.getSimpleName();
    private List<GroovyObjectEx<com.docker.storage.redis.MyRedisListener>> groovyObjectExes = new CopyOnWriteArrayList<>();

    @Override
    public void handlerShutdown() {
    }

    @Override
    public void handleAnnotatedClassesInjectBean(AbstractRuntimeContext runtimeContext) {
        for (GroovyObjectEx<com.docker.storage.redis.MyRedisListener> groovyObjectEx : groovyObjectExes) {
            try {
                groovyObjectEx = (GroovyObjectEx<com.docker.storage.redis.MyRedisListener>) getObject(null, groovyObjectEx.getGroovyClass(), runtimeContext);
            }catch (CoreException e){
                LoggerEx.error(TAG, e.getMessage());
            }
        }
    }

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return RedisListener.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap, AbstractRuntimeContext runtimeContext) throws CoreException {
        if (annotatedClassMap != null) {
            Set<String> keys = annotatedClassMap.keySet();
            for (String key : keys) {
                Class<?> groovyClass = annotatedClassMap.get(key);
                if (groovyClass != null) {
                    RedisListener redisListenerAnnotation = groovyClass.getAnnotation(RedisListener.class);
                    if (redisListenerAnnotation != null) {
                        GroovyObjectEx<com.docker.storage.redis.MyRedisListener> redisListenerObj = (GroovyObjectEx<com.docker.storage.redis.MyRedisListener>) getObject(null, groovyClass, runtimeContext);
                        if (redisListenerObj != null) {
                            groovyObjectExes.add(redisListenerObj);
                            //TODO RedisHandler
//                            try {
//                                redisListenerObj.getObject().redisHandler = ((BaseRuntime) groovyRuntime).getRedisHandler();
//                            } catch (CoreException e) {
//                                LoggerEx.error(TAG, ExceptionUtils.getFullStackTrace(e));
//                            }
                        }
                    }
                }
            }
        }
    }


//    public void setRedisHandler() {
//        for (GroovyObjectEx<com.docker.storage.redis.MyRedisListener> groovyObjectEx : groovyObjectExes) {
//            try {
//                groovyObjectEx.getObject().redisHandler = ((BaseRuntime)groovyObjectEx.getGroovyRuntime()).getRedisHandler();
//            } catch (CoreException e) {
//                LoggerEx.error(TAG, ExceptionUtils.getFullStackTrace(e));
//            }
//        }
//    }
}
