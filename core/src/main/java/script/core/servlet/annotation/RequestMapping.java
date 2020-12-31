package script.core.servlet.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import script.core.servlets.GroovyServletManager;

@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
	String method() default "GET";
	String uri();
	String responseType() default GroovyServletManager.RESPONSETYPE_JSON;
	String[] perms() default "";
	boolean asyncSupported() default false;
}
