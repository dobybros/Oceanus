package script.core.annotation.condition;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import script.core.runtime.AbstractRuntimeContext;

import java.util.Objects;
import java.util.Properties;

/**
 * @author <zft>
 * @date 2021/6/7
 * @Description: <>
 * @ClassName: ConditionOnProperty
 */
public class OnPropertyCondition implements Condition {
    @Override
    public boolean match(AbstractRuntimeContext context, Class<?> clazz) {
        ConditionalOnProperty annotation = clazz.getAnnotation(ConditionalOnProperty.class);
        Properties properties = context.getConfiguration().getConfig();
        if (annotation != null && MapUtils.isNotEmpty(properties)) {
            String property = MapUtils.getString(properties, annotation.propertyName());
            if (StringUtils.isEmpty(property)) {
                //空是否算匹配上了
                return annotation.matchIfMissing();
            } else if (StringUtils.isEmpty(annotation.havingValue())) {
                //没有预期值。不是空，也别是false
                return !Objects.equals(property, "false");
            } else {
                //和预期值相匹配
                return Objects.equals(property, annotation.havingValue());
            }
        }
        return false;
    }
}
