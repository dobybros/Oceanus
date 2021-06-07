package script.core.annotation.condition;

import java.lang.annotation.*;

/**
 * @author <zft>
 * @date 2021/6/7
 * @Description: <简单版Spring @Conditional,条件开关>
 * @ClassName: Conditional
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

    Class<? extends Condition>[] value();

}
