package script.core.runtime.handler.annotation.clazz;

import chat.errors.CoreException;
import script.core.annotation.RegisterClassAnnotation;
import script.core.runtime.groovy.object.GroovyObjectEx;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

/**
 * Created by lick on 2020/6/19.
 * Description：
 */
public class RegisterClassAnnotationHandler extends ClassAnnotationHandler {
    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return RegisterClassAnnotation.class;
    }

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        if (annotatedClassMap != null) {
            Collection<Class<?>> values = annotatedClassMap.values();
            for (Class<?> groovyClass : values) {
                RegisterClassAnnotation registerClassAnnotation = groovyClass.getAnnotation(RegisterClassAnnotation.class);
                if (registerClassAnnotation != null) {
                    GroovyObjectEx<?> groovyObj = (GroovyObjectEx<?>) getObject(null, groovyClass, runtimeContext);
                    try {
                        Object o = groovyObj.getObject();
                        if(o instanceof ClassAnnotationHandler){
                            ClassAnnotationHandler classAnnotationHandler = ((ClassAnnotationHandler)o);
                            classAnnotationHandler.handleAnnotatedClasses(runtimeContext.getAllClasses());
                        }
                    }catch (Throwable t){
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public boolean isBean() {
        return super.isBean();
    }

    @Override
    public Object getKey() {
        return super.getKey();
    }
}
