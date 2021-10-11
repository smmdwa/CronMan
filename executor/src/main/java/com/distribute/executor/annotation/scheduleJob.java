package com.distribute.executor.annotation;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface scheduleJob {

    String name() ;

    String init() default "init";

    String destroy() default "destroy";

}
