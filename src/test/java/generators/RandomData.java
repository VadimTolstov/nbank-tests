package generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RandomData {
    private RandomData() {
    }

    public static String getUsername() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getPassword() {
        return RandomStringUtils.randomAlphabetic(3).toUpperCase() +
                RandomStringUtils.randomAlphabetic(5).toLowerCase() +
                RandomStringUtils.randomNumeric(3) + "$";
    }

    public static String getFakeToken() {
        return "Basic " + Base64.getEncoder()
                .encodeToString("wrong:wrong".getBytes(StandardCharsets.UTF_8));
    }
}
