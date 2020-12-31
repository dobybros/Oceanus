package com.docker.handler.annotation.field;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.context.Context;
import com.docker.context.ServiceContext;
import com.docker.script.BaseRuntimeContext;
import com.docker.script.i18n.I18nHandler;
import com.docker.script.i18n.MessageProperties;
import com.docker.storage.cache.CacheStorageFactory;
import com.docker.storage.cache.CacheStorageMethod;
import com.docker.storage.cache.handlers.RedisCacheStorageHandler;
import com.docker.storage.redis.RedisHandler;
import com.docker.storage.zookeeper.ZookeeperFactory;
import com.docker.storage.zookeeper.ZookeeperHandler;
import org.apache.commons.lang.StringUtils;
import script.core.annotation.JavaBean;
import com.docker.utils.BeanFactory;
import script.core.runtime.handler.AbstractFieldAnnotationHandler;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public class JavaBeanHandler extends AbstractFieldAnnotationHandler<JavaBean> {
    private ZookeeperFactory zookeeperFactory = (ZookeeperFactory) BeanFactory.getBean(ZookeeperFactory.class.getName());
    private final String TAG = JavaBeanHandler.class.getSimpleName();
    //TODO need implement
    @Override
    public Class<JavaBean> annotationClass() {
        return JavaBean.class;
    }

    @Override
    public void inject(JavaBean annotation, Field field, Object obj) throws CoreException {
        Class fieldClass = (Class) field.getGenericType();
        try {
            Object o = null;
            if(fieldClass.equals(Context.class)){
                o = new ServiceContext((BaseRuntimeContext) runtimeContext);
            }else if(fieldClass.equals(RedisHandler.class)){
                String redisHost = runtimeContext.getConfiguration().getConfig().getProperty("db.redis.uri");
                if(StringUtils.isBlank(redisHost)){
                    LoggerEx.error(TAG, "Cant find db.redis.uri, cant get redisHandler, configuration: " + runtimeContext.getConfiguration());
                }else {
                    RedisCacheStorageHandler cacheStorageAdapter = (RedisCacheStorageHandler) CacheStorageFactory.getInstance().getCacheStorageAdapter(CacheStorageMethod.METHOD_REDIS, redisHost);
                    o = cacheStorageAdapter.getRedisHandler();
                }
            }else if(fieldClass.equals(ZookeeperHandler.class)){
                String zookeeperHost = runtimeContext.getConfiguration().getConfig().getProperty("db.zk.uri");
                if(StringUtils.isBlank(zookeeperHost)){
                    LoggerEx.error(TAG, "Cant find db.zk.uri, cant get zookeeperHandler, configuration: " + runtimeContext.getConfiguration());
                }else {
                    o = zookeeperFactory.get(zookeeperHost);
                }
            }else if(fieldClass.equals(I18nHandler.class)){
                String i18nFolder = runtimeContext.getConfiguration().getConfig().getProperty("i18n.folder");
                String name = runtimeContext.getConfiguration().getConfig().getProperty("i18n.name");
                if (i18nFolder != null && name != null) {
                    I18nHandler i18nHandler = new I18nHandler();
                    File messageFile = new File(runtimeContext.getConfiguration().getLocalPath() + File.separator + i18nFolder);
                    if (messageFile.exists()) {
                        File[] files = messageFile.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                String fileName = file.getName();
                                fileName = fileName.replace(name + "_", "");
                                fileName = fileName.replace(".properties", "");
                                MessageProperties messageProperties = new MessageProperties();
                                new MessageProperties().setAbsolutePath(file.getAbsolutePath());
                                i18nHandler.getMsgPropertyMap().put(fileName, messageProperties);
                            }
                        }
                    }
                    o = i18nHandler;
                }
            }
            field.setAccessible(true);
            if(o != null){
                field.set(obj, o);
            }
        }catch (IllegalAccessException e){
            LoggerEx.error(TAG, "field " + field + " inject failed, errMsg: " + e.getCause());
        }
    }
}
