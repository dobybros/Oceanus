package com.docker.annotations;

import com.docker.annotations.Summary;

import java.lang.annotation.*;

@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Summaries {
	Summary[] values() default {};
}
