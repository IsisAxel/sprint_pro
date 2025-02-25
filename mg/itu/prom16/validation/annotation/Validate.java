package mg.itu.prom16.validation.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME) 
public @interface Validate {

}
