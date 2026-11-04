package jupiter.annotation.meta;

import io.qameta.allure.Feature;
import jupiter.extension.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Feature("WEB тесты")
@Tag("WEB")
@ExtendWith({
        BrowserExtension.class,
        TestDataExtension.class,
        UserExtension.class,
        ApiLoginExtension.class,
        AccountExtension.class
})
public @interface WebTest {
}
