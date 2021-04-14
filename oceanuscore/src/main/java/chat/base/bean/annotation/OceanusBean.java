package chat.base.bean.annotation;

import java.lang.annotation.*;

/**
 * Created by lick on 2021/1/2.
 * Description：
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OceanusBean {
    String[] value() default {};
}
