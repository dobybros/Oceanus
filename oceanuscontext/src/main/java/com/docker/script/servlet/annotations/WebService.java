package com.docker.script.servlet.annotations;

import java.lang.annotation.*;

@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface WebService {
}
