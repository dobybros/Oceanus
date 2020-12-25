package com.docker.handler.annotation.field;

import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import com.docker.annotations.ConfigProperty;
import org.apache.commons.lang.StringUtils;
import script.core.runtime.handler.AbstractFieldAnnotationHandler;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Created by lick on 2020/12/22.
 * Description：
 */
public class ConfigPropertyHandler extends AbstractFieldAnnotationHandler<ConfigProperty> {
    private final String TAG = ConfigPropertyHandler.class.getSimpleName();

    @Override
    public Class<ConfigProperty> annotationClass() {
        return ConfigProperty.class;
    }

    @Override
    public void inject(ConfigProperty annotation, Field field, Object obj) throws CoreException {
        String key = processAnnotationString(annotation.name());
        if (!StringUtils.isBlank(key)) {
            Properties properties = runtimeContext.getConfiguration().getConfig();
            if (properties == null)
                return;
            String value = properties.getProperty(key);
            if (value == null)
                return;
            try {
                field.setAccessible(true);
                field.set(obj, TypeUtils.cast(value, field.getType(), ParserConfig.getGlobalInstance()));
            } catch (Throwable e) {
                e.printStackTrace();
                LoggerEx.error(TAG, "Set field " + field.getName() + " for config key " + key + " class " + field.getType() + " in class " + obj.getClass());
            }
        }
    }
}
