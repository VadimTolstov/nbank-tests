package jupiter.annotation;

import models.rest.AdminConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ApiLogin(
        username = AdminConstants.LOGIN,
        password = AdminConstants.PASSWORD
)
public @interface AdminApiLogin {
}
