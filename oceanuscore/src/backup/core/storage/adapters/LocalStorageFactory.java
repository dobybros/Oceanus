package core.storage.adapters;

import core.common.AbstractFactory;
import core.common.CoreRuntime;
import core.common.InternalTools;
import core.log.LoggerHelper;

import java.util.concurrent.ConcurrentHashMap;

public class LocalStorageFactory extends AbstractFactory<LocalStorage> {
    private String path;
    private Class<? extends LocalStorage> localStorageClass;
    private final byte[] localStorageClassLock = new byte[0];
    protected InternalTools internalTools;
    private ConcurrentHashMap<String, LocalStorage> nameLocalStorageMap = new ConcurrentHashMap<>();
    public LocalStorageFactory(Class<? extends LocalStorage> localStorageClass, String path, InternalTools internalTools) {
        super();
        if(path == null || localStorageClass == null)
            throw new NullPointerException("Illegal localStorageClass " + localStorageClass + " or path " + path + " while initiate LocalStorageFactory");
        this.localStorageClass = localStorageClass;
        this.path = path;
        this.internalTools = internalTools;
    }

    public LocalStorage getLocalStorage(String name) {
        LocalStorage localStorage = nameLocalStorageMap.get(name);
        if(localStorage == null) {
            try {
                synchronized (localStorageClassLock) { // make sure init method is executed in multiple thread environment.
                    localStorage = get(localStorageClass);
                    localStorage.name = name;
                    localStorage.path = path;
                    localStorage.internalTools = internalTools;
                    LocalStorage old = nameLocalStorageMap.putIfAbsent(name, localStorage);
                    if(old != null) {
                        localStorage = old;
                    }
                }
            } catch (Throwable t) {
                LoggerHelper.logger.error("LocalStorage " + localStorageClass + " on " + path + "#" + name + " init failed, " + t.getMessage());
            }
            if(localStorage == null) {
                localStorage = nameLocalStorageMap.get(name);
            }
            LoggerHelper.logger.info("localStorage for name " + name + " is " + localStorage + " on path " + path);
        }
        return localStorage;
    }
}
