package tests;

import jupiter.annotation.Data;
import jupiter.annotation.User;
import jupiter.annotation.meta.RestTest;
import models.TestData;
import org.junit.jupiter.api.Test;

@RestTest
public class Test1 {


    @Data(
            users = @User(username = "alicess1", password = "Vadim123!"),
            randomUsers = 3
    )
    @Test
    void test(TestData user) {
        System.out.printf("User: %s", user.usersJson().stream().findFirst().get());
        System.out.println(user.usersJson().size() + " users found");
    }
}
