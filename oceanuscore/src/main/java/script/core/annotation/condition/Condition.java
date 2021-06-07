package script.core.annotation.condition;

import script.core.runtime.AbstractRuntimeContext;

/**
 * @author <zft>
 * @date 2021/6/7
 * @Description: <>
 * @ClassName: Condtion
 */
public interface Condition {
    boolean match(AbstractRuntimeContext context, Class<?> clazz);
}
