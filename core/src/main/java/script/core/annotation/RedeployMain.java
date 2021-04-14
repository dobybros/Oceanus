package script.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface RedeployMain {
    /**
     * 加载顺序,数字越小优先级越高
     * 1-100给系统(例：db)相关的顺序
     * 其他自定义>100
     * @return
     */
    int order() default 100;
}
