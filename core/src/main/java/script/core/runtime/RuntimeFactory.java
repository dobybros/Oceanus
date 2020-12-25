package script.core.runtime;

import script.Runtime;

import chat.config.Configuration;
import script.RuntimeContext;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public interface RuntimeFactory {
    public Runtime create(AbstractRuntimeContext runtimeContext) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException;
}
