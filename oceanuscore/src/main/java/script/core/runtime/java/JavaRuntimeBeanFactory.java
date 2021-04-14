package script.core.runtime.java;

import chat.errors.CoreException;
import script.core.runtime.RuntimeBeanFactory;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class JavaRuntimeBeanFactory  implements RuntimeBeanFactory {
    @Override
    public Object get(String beanName, String clazz) throws CoreException {
        return null;
    }

    @Override
    public void fillAllObject() throws CoreException {

    }

    @Override
    public void fillObject(Object o) throws CoreException {

    }

    @Override
    public void close() {

    }
}
