package tests;

import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.rest.CreateUserJsonResponse;
import org.junit.jupiter.api.Test;

@RestTest
public class Test1 {


    @User
    @Test
    void test(CreateUserJsonResponse user) {
        System.out.printf("User: %s%n", user);
    }
}
