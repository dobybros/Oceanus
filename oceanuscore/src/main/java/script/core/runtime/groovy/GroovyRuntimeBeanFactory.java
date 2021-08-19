package script.core.runtime.groovy;


import oceanus.apis.CoreException;
import script.core.runtime.RuntimeBeanFactory;
import script.core.runtime.groovy.object.AbstractObject;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public abstract class GroovyRuntimeBeanFactory implements RuntimeBeanFactory {
    protected final String TAG = GroovyRuntimeBeanFactory.class.getSimpleName();
    protected abstract  <T> AbstractObject<T> getBeanGroovy(String beanName, String groovyPath) throws CoreException;


    protected abstract void fillObjectGroovy() throws CoreException;

    @Override
    public Object get(String beanName, String clazz) throws CoreException {
        return getBeanGroovy(beanName, clazz);
    }

    @Override
    public void fillAllObject() throws CoreException {
        fillObjectGroovy();
    }
}
