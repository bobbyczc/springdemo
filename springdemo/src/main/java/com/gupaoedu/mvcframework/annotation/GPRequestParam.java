package com.gupaoedu.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author bobstorm
 * @date 2020/6/6 16:35
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPRequestParam {
    String value() default "";
}
