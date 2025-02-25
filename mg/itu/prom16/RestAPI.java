package mg.itu.prom16;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME) 
public @interface RestAPI {
    String value() default "";
}
