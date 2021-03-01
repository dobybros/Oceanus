package com.dobybros.chat.open.annotations;

import java.lang.annotation.*;

// 使用后整个service只会有一个listener

@Deprecated
@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface SessionHandler {
}
