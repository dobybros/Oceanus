package script.core.runtime.handler;

import chat.errors.CoreException;
import org.apache.commons.lang.StringUtils;
import script.core.runtime.AbstractRuntimeContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
public abstract class AbstractFieldAnnotationHandler<T extends Annotation> {
    protected AbstractRuntimeContext runtimeContext;
    public abstract Class<T> annotationClass();

    public abstract void inject(T annotation, Field field, Object obj) throws CoreException;

    protected String processAnnotationString(String markParam) {
        if (StringUtils.isNotBlank(markParam)) {
            if (markParam.startsWith("#{") && markParam.endsWith("}")) {
                markParam = markParam.replaceAll(" ", "");
                String[] markParams = markParam.split("#\\{");
                if (markParams.length == 2) {
                    markParam = markParams[1];
                    markParams = markParam.split("}");
                    if (markParams.length == 1) {
                        markParam = markParams[0];
                        markParam = runtimeContext.getConfiguration().getConfig().getProperty(markParam);
                    }
                }
            }
        }
        return markParam;
    }

    public AbstractRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    public void setRuntimeContext(AbstractRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }
}
