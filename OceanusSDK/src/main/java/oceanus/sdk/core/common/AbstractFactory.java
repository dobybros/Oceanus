package oceanus.sdk.core.common;

import oceanus.sdk.logger.LoggerEx;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFactory<T> {
    private static final String TAG = AbstractFactory.class.getSimpleName();
    protected ConcurrentHashMap<Class<? extends T>, T> classTMap = new ConcurrentHashMap<>();
    protected T get(Class<? extends T> tClass) {
        if(tClass == null) {
            LoggerEx.error(TAG, "tClass is null while get");
            return null;
        }
        T t = classTMap.get(tClass);
        if(t == null) {
            try {
                t = tClass.getConstructor().newInstance();
                T old = classTMap.putIfAbsent(tClass, t);
                if(old != null) {
//                    LoggerEx.warn(TAG, "Already existing, " + tClass + " obj " + old + " new " + t + " will be obsoleted");
                    t = old;
                }
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                LoggerEx.error(TAG, "Create for class " + tClass + " failed, " + e.getMessage());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LoggerEx.error(TAG, "Create for class " + tClass + " unknown error, " + throwable.getMessage());
            }
        }
        return t;
    }

    protected T create(Class<? extends T> tClass) {
        if(tClass == null) {
            LoggerEx.error(TAG, "tClass is null while create");
            return null;
        }
        try {
            return tClass.getConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Create for class " + tClass + " failed, " + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            LoggerEx.error(TAG, "Create for class " + tClass + " unknown error, " + throwable.getMessage());
        }
        return null;
    }
}
