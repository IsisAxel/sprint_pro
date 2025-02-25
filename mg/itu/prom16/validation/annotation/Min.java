package mg.itu.prom16.validation.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME) 
public @interface Min 
{
    double value() default 0;
    String message() default "";
}