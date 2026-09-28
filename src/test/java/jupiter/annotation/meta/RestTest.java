package jupiter.annotation.meta;

import io.qameta.allure.Feature;
import jupiter.extension.ApiLoginExtension;
import jupiter.extension.TestDataExtension;
import jupiter.extension.UserExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Feature("REST тесты")
@Tag("REST")
@ExtendWith({
        TestDataExtension.class,
        UserExtension.class,
        ApiLoginExtension.class,
})
public @interface RestTest {
}
