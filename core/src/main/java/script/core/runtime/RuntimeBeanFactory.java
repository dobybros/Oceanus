package script.core.runtime;

import chat.errors.CoreException;

import java.util.Collection;

/**
 * Created by lick on 2020/12/18.
 * Bean加载流程:处理注解过程中，如果需要调用(invoke)，就需要getObject(true),会填充field,如果field是bean,会继续getBean(true);
 * 所有注解处理完之后，会把所有bean的field重新装一遍,前边如果getObject了，就不再执行
 *
 */
public interface RuntimeBeanFactory {
    Object get(String beanName, String clazz) throws CoreException;

    void fillAllObject() throws CoreException;

    void fillObject(Object o) throws CoreException;

    void close();
}
