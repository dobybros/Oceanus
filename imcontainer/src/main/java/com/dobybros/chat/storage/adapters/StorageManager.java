package com.dobybros.chat.storage.adapters;

import chat.config.BaseConfiguration;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.dobybros.chat.script.IMRuntimeContext;
import com.docker.utils.BeanFactory;

import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private static final String TAG = StorageManager.class.getSimpleName();
    private static volatile StorageManager instance;
    private BaseConfiguration baseConfiguration = (BaseConfiguration) BeanFactory.getBean(BaseConfiguration.class.getName());

    // 跨区的adapter，比较特殊
    private ConcurrentHashMap<Class, StorageAdapter> acrossAdaptorMap = new ConcurrentHashMap<>();

    public static StorageManager getInstance() {
        if (instance == null) {
            synchronized (StorageManager.class) {
                if (instance == null)
                    instance = new StorageManager();
            }
        }
        return instance;
    }

    public <T extends StorageAdapter> T getStorageAdapter(Class<T> adapterClass) throws CoreException {
        return getStorageAdapter(adapterClass, baseConfiguration.getLanId());
    }

    @SuppressWarnings("unchecked")
    public <T extends StorageAdapter> T getStorageAdapter(Class<T> adapterClass, String lanId) throws CoreException {
        String className = adapterClass.getName();
        String serviceName = baseConfiguration.getExtraProperties().getProperty("service." + className);
        if (serviceName == null) {
            LoggerEx.warn(TAG, "service not exist, class:" + adapterClass);
            return null;
        } else {
            return ((IMRuntimeContext)baseConfiguration.getRuntimeContext(serviceName)).getServiceStubManagerFactory().get(lanId).getService(serviceName, adapterClass);
        }
    }
}
