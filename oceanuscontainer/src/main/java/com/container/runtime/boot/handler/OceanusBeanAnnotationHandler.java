package com.container.runtime.boot.handler;

import chat.base.bean.annotation.OceanusBean;
import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import chat.logs.LoggerEx;
import com.docker.oceansbean.AbstractOceansAnnotationHandler;
import com.docker.oceansbean.OceanusBeanManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
public class OceanusBeanAnnotationHandler extends AbstractOceansAnnotationHandler {
    public static final String TAG = OceanusBeanAnnotationHandler.class.getSimpleName();


    public OceanusBeanAnnotationHandler(OceanusBeanManager oceanusBeanManager) {
        super(oceanusBeanManager);
    }

    @Override
    public void handle(String packageName) throws CoreException {
        if (StringUtils.isBlank(packageName)) {
            packageName = "com.container.runtime.boot.bean";
        }
        // 扫包
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(packageName) // 指定路径URL
                .addScanners(new SubTypesScanner()) // 添加子类扫描工具
                .addScanners(new FieldAnnotationsScanner()) // 添加 属性注解扫描工具
                .addScanners(new MethodAnnotationsScanner()) // 添加 方法注解扫描工具
                .addScanners(new MethodParameterScanner()) // 添加方法参数扫描工具
        );
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(getAnnotationClass());
        for (Class c : classes) {
            try {
                Object o = c.getDeclaredConstructor().newInstance();
                Method[] methods = c.getMethods();
                String[] beanNames;
                for (Method method : methods) {
                    if (method.isAnnotationPresent(OceanusBean.class)) {
                        OceanusBean oceanusBean = method.getAnnotation(OceanusBean.class);
                        if (oceanusBean.value().length > 0) {
                            beanNames = oceanusBean.value();
                        } else {
                            beanNames = new String[1];
                            beanNames[0] = method.getName();
                        }
                        LoggerEx.info(TAG, "Oceanus Bean invoke bean class:" + c.getName() + "method:" + method.getName());
                        Object bean = method.invoke(o);
                        oceanusBeanManager.addBean(method.getReturnType().getName(), beanNames, bean);
                    }
                }
            } catch (Throwable t) {
                throw new CoreException(ChatErrorCodes.ERROR_REFLECT, ExceptionUtils.getFullStackTrace(t));
            }
        }
    }

    @Override
    public Class<? extends Annotation> getAnnotationClass() {
        return OceanusBean.class;
    }
}
