package script.core.runtime.impl;

import script.Runtime;
import script.RuntimeContext;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.RuntimeFactory;

import chat.config.Configuration;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class DefaultRuntimeFactory implements RuntimeFactory {//script.core.runtime.groovy
    @Override
    public Runtime create(AbstractRuntimeContext runtimeContext) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> clazz = Class.forName("script.core.runtime." + runtimeContext.getConfiguration().getLanguageType().toLowerCase() + "." + runtimeContext.getConfiguration().getLanguageType() + "Runtime");
        return (Runtime) clazz.getDeclaredConstructor(AbstractRuntimeContext.class).newInstance((AbstractRuntimeContext)runtimeContext);
    }
}
