package com.gupaoedu.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author bobstorm
 * @date 2020/6/6 16:34
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPRequestMapping {
    String value() default "";
}
