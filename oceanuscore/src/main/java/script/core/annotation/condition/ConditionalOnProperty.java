package script.core.annotation.condition;

import org.apache.commons.lang.StringUtils;

import java.lang.annotation.*;

/**
 * @author <zft>
 * @date 2021/6/7
 * @Description: <根据application.properties 属性值，作为判定条件>
 * @ClassName: ConditionalOnProperty
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConditionalOnProperty {

//    Class<OnPropertyCondition> value() default OnPropertyCondition.class;

    /**
     * 声明的属性值
     *
     * @return
     */
    String propertyName() default StringUtils.EMPTY;

    /**
     * 指定的{@link ConditionalOnProperty#propertyName()}的值，如果不指定，只要不是false，都生效
     * The string representation of the expected value for the properties. If not
     * specified, the property must <strong>not</strong> be equals to {@code false}.
     *
     * @return
     */
    String havingValue() default StringUtils.EMPTY;

    /**
     * 没有配置property,是否算匹配上了。默认不能丢
     * Specify if the condition should match if the property is not set. Defaults to
     * {@code false}.
     *
     * @return
     */
    boolean matchIfMissing() default false;
}
