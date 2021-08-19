package core.storage;

import chat.logs.LoggerEx;
import core.storage.adapters.impl.RocksDBLocalStorage;
import org.apache.commons.io.FileUtils;
import core.common.CoreRuntime;
import core.storage.adapters.LocalStorage;
import core.storage.adapters.LocalStorageFactory;
import core.storage.serializations.SerializationDataFactory;
import core.storage.serializations.SerializationDataHandler;
import core.storage.serializations.handlers.FastJsonSerializationDataHandler;

import java.io.File;
import java.io.IOException;

public class StorageRuntime extends CoreRuntime {
    private static final String TAG = StorageRuntime.class.getSimpleName();
    private static SerializationDataFactory serializationDataFactory = new SerializationDataFactory();
    private static Class<? extends SerializationDataHandler> serializationDataHandlerClass;

    private static LocalStorageFactory localStorageFactory;
    private static Class<? extends LocalStorage> localStorageClass;

    public static SerializationDataHandler getSerializationDataHandler() {
        if(serializationDataHandlerClass == null) {
            String serializationHandlerClassStr = System.getProperty("starfish.serialization.data.class");
            if(serializationHandlerClassStr != null) {
                try {
                    Class<? extends SerializationDataHandler> clazz = (Class<? extends SerializationDataHandler>) Class.forName(serializationHandlerClassStr);
                    serializationDataHandlerClass = clazz;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    LoggerEx.error(TAG, "Class not found while read from system property \"starfish.serialization.data.class\", " + serializationHandlerClassStr);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Unknown error occurred while read from system property \"starfish.serialization.data.class\", " + serializationHandlerClassStr + " error " + t.getMessage());
                }
            }
            if(serializationDataHandlerClass == null)
                serializationDataHandlerClass = FastJsonSerializationDataHandler.class;
            LoggerEx.info(TAG, "serializationHandlerClass is " + serializationDataHandlerClass);
        }
        return serializationDataFactory.getSerializationDataHandler(serializationDataHandlerClass);
    }

    private static LocalStorageFactory getLocalStorageFactory() {
        if(localStorageFactory == null) {
            String localStorageClassStr = System.getProperty("starfish.local.storage.class");
            if(localStorageClassStr != null) {
                try {
                    Class<? extends LocalStorage> clazz = (Class<? extends LocalStorage>) Class.forName(localStorageClassStr);
                    localStorageClass = clazz;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    LoggerEx.error(TAG, "Class not found while read from system property \"starfish.local.storage.class\", " + localStorageClassStr);
                } catch (Throwable t) {
                    t.printStackTrace();
                    LoggerEx.error(TAG, "Unknown error occurred while read from system property \"starfish.local.storage.class\", " + localStorageClassStr + " error " + t.getMessage());
                }
            }
            String localStoragePath = System.getProperty("starfish.local.storage.path");
            if(localStoragePath == null) {
                localStoragePath = "../localstorage/";
            }
            try {
                FileUtils.forceMkdir(new File(localStoragePath));
            } catch (IOException e) {
                e.printStackTrace();
                LoggerEx.error(TAG, "LocalStorageFactory init path " + localStoragePath + " failed, " + e.getMessage());
            }
            if(localStorageClass == null)
                localStorageClass = RocksDBLocalStorage.class;
            localStorageFactory = new LocalStorageFactory(localStorageClass, localStoragePath, getInternalTools());
            LoggerEx.info(TAG, "LocalStorageFactory created with localStorageClass " + localStorageClass + " on path " + localStoragePath);
        }
        return localStorageFactory;
    }

    public static LocalStorage getLocalStorage(String name){
        return getLocalStorageFactory().getLocalStorage(name);
    }
}
