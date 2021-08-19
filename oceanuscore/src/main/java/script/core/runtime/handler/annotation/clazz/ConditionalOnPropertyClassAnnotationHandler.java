package script.core.runtime.handler.annotation.clazz;

import com.google.common.collect.Maps;
import oceanus.apis.CoreException;
import org.apache.commons.collections.MapUtils;
import script.core.annotation.condition.ConditionalOnProperty;
import script.core.annotation.condition.OnPropertyCondition;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author <zft>
 * @date 2021/6/7
 * @Description: <>
 * @ClassName: ConditionalClassAnnotationHandler
 */
public class ConditionalOnPropertyClassAnnotationHandler extends ClassAnnotationHandler {

    /**
     * 类->判定结果
     * 这个类是否可以做某些事情，比如是否可以{@link RedeployMainHandler}自启动
     */
    private final Map<Class<?>, Boolean> classToMatchMap = Maps.newConcurrentMap();

    @Override
    public void handleAnnotatedClasses(Map<String, Class<?>> annotatedClassMap) throws CoreException {
        for (Class<?> clazz : annotatedClassMap.values()) {
            OnPropertyCondition onPropertyCondition = new OnPropertyCondition();
            classToMatchMap.put(clazz, onPropertyCondition.match(runtimeContext, clazz));
        }
    }

    @Override
    public Class<? extends Annotation> handleAnnotationClass() {
        return ConditionalOnProperty.class;
    }

    /**
     * 是否满足判定条件，如果没有判定条件，表示没有条件，可以继续
     *
     * @param clazz
     * @return
     */
    public boolean match(Class<?> clazz) {
        return clazz != null && MapUtils.getBoolean(classToMatchMap, clazz, true);
    }
}
