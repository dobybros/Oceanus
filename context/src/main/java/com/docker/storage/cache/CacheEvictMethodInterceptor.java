package com.docker.storage.cache;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import chat.utils.ReflectionUtil;
import com.docker.data.CacheObj;
import com.docker.rpc.method.RPCMethodInvocation;
import com.docker.storage.cache.handlers.CacheStorageAdapter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.MethodInterceptor;
import script.core.runtime.groovy.object.MethodInvocation;

import java.util.Map;

public class CacheEvictMethodInterceptor implements MethodInterceptor {
    public static final String TAG = CachePutMethodInterceptor.class.getSimpleName();
    private Map<String, CacheObj> cacheMethodMap;
    private CacheAnnotationHandler cacheAnnotationHandler;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws CoreException {
        RPCMethodInvocation rpcMethodInvocation = (RPCMethodInvocation) methodInvocation;
        String methodKey = rpcMethodInvocation.methodKey;
        //获取当前的Runtime
        AbstractRuntimeContext runtimeContext = cacheAnnotationHandler.getRuntimeContext();
        if (cacheMethodMap != null && !cacheMethodMap.isEmpty()) {
            CacheObj cacheObj = cacheMethodMap.get(methodKey);
            if (cacheObj != null) {
                String cacheHost = getCacheHost(cacheObj.getCacheMethod());
                CacheStorageAdapter cacheStorageAdapter = CacheStorageFactory.getInstance().getCacheStorageAdapter(cacheObj.getCacheMethod(), cacheHost);
                if (cacheStorageAdapter == null || cacheObj.isEmpty()) {
                    return rpcMethodInvocation.proceed();
                }
                Object key = ReflectionUtil.parseSpel(cacheObj.getParamNames(), rpcMethodInvocation.arguments, cacheObj.getSpelKey());
                if (key == null) {
                    return rpcMethodInvocation.proceed();
                } else {
                    try {
                        cacheStorageAdapter.deleteCacheData(cacheObj.getPrefix(), (String)key);
                    } catch (Throwable throwable) {
                        LoggerEx.error(TAG, "Delete cache failed by key : " + cacheObj.getPrefix() + "_" + key + ",reason is " + ExceptionUtils.getFullStackTrace(throwable));
                    }
                    return rpcMethodInvocation.proceed();
                }
            }
        }
        return rpcMethodInvocation.proceed();
    }
    public String getCacheHost(String cacheMethod) {
        if (StringUtils.isBlank(cacheMethod)) {
            cacheMethod = CacheStorageMethod.METHOD_REDIS;
        }
        if (CacheStorageMethod.METHOD_REDIS.equals(cacheMethod)) {
            Object cacheRedisUri = cacheAnnotationHandler.getRuntimeContext().getConfiguration().getConfig().getProperty("cache.redis.uri");
            if (cacheRedisUri == null) {
                return null;
            }
            return (String) cacheRedisUri;
        }
        return null;
    }
    public void setCacheAnnotationHandler(CacheAnnotationHandler cacheAnnotationHandler) {
        this.cacheAnnotationHandler = cacheAnnotationHandler;
        this.cacheMethodMap = cacheAnnotationHandler.getCacheMethodMap();

    }

}
