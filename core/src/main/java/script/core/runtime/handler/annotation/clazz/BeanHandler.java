package script.core.runtime.handler.annotation.clazz;

import chat.errors.CoreException;
import org.apache.commons.lang.StringUtils;
import script.core.annotation.Bean;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

public class BeanHandler extends ClassAnnotationHandler {
	private static final String TAG = BeanHandler.class.getSimpleName();
	@Override
	public void handlerShutdown() {
		runtimeContext.getRuntimeBeanFactory().close();
	}

	public BeanHandler() {

	}

	@Override
	public Class<? extends Annotation> handleAnnotationClass() {
		return Bean.class;
	}

	@Override
	public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
		if (annotatedClassMap != null) {
			Collection<Class<?>> values = annotatedClassMap.values();
			for (Class<?> groovyClass : values) {
				Bean bean = groovyClass.getAnnotation(Bean.class);
				String name = processAnnotationString(runtimeContext, bean.name());
				if (StringUtils.isBlank(name)) {
					name = null;
				}
				getObject(name, groovyClass, runtimeContext);
			}
		}
	}
}
