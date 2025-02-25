package mg.itu.prom16.validation.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME) 
public @interface NotEmpty 
{
    String message() default "";
}