package script.core.annotation;

import java.lang.annotation.*;

@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface RegisterClassAnnotation {

}
