package com.docker.handler.annotation.field;

import chat.logs.LoggerEx;
import com.docker.annotations.ServiceBean;
import com.docker.script.BaseRuntimeContext;
import oceanus.apis.CoreException;
import org.apache.commons.lang.StringUtils;
import script.core.runtime.handler.AbstractFieldAnnotationHandler;

import java.lang.reflect.Field;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public class ServiceBeanHandler extends AbstractFieldAnnotationHandler<ServiceBean> {
    private final String TAG = ServiceBeanHandler.class.getSimpleName();
    @Override
    public Class<ServiceBean> annotationClass() {
        return ServiceBean.class;
    }

    @Override
    public void inject(ServiceBean annotation, Field field, Object obj) throws CoreException {
        String serviceName = processAnnotationString(annotation.name());
        String lanId = processAnnotationString(annotation.lanId());
        if (!StringUtils.isBlank(serviceName)) {
            Object serviceStub;
            if (StringUtils.isBlank(lanId)) {
                serviceStub = ((BaseRuntimeContext)runtimeContext).getServiceStubManagerFactory().get().getService(serviceName, field.getType());
            } else {
                serviceStub = ((BaseRuntimeContext)runtimeContext).getServiceStubManagerFactory().get(lanId).getService(serviceName, field.getType());
            }
            try {
                field.setAccessible(true);
                field.set(obj, serviceStub);
            } catch (Throwable e) {
                e.printStackTrace();
                LoggerEx.error(TAG, "Set field " + field.getName() + " for service " + serviceName + " class " + field.getType() + " in class " + obj.getClass());
            }
        }
    }
}
