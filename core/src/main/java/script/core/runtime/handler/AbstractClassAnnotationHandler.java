package script.core.runtime.handler;

import chat.errors.CoreException;
import org.apache.commons.lang.StringUtils;
import script.Runtime;
import script.RuntimeContext;
import script.core.runtime.AbstractRuntimeContext;

import java.lang.annotation.Annotation;

/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public abstract class AbstractClassAnnotationHandler {
    public boolean isBean() {
        return true;
    }

    public Object getKey() {
        return this.getClass();
    }

    public abstract Class<? extends Annotation> handleAnnotationClass();

    public void handlerShutdown() {
    }
    protected Object getObject(String beanName, Class clazz, AbstractRuntimeContext runtimeContext) throws CoreException {
        return getObject(beanName, runtimeContext.getRuntime().path(clazz), runtimeContext);
    }
    protected Object getObject(String beanName, String clazzStr, AbstractRuntimeContext runtimeContext) throws CoreException {
        return runtimeContext.getRuntimeBeanFactory().get(beanName, clazzStr);
    }
    protected String processAnnotationString(AbstractRuntimeContext runtimeContext, String markParam) {
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
}