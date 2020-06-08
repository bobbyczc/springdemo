package com.gupaoedu.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author bobstorm
 * @date 2020/6/6 16:32
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPController {
    String value() default "";
}
