package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config2 {
    private static final Config2 INSTANCE = new Config2();
    private final Properties properties = new Properties();

    private Config2() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Fail to load config.properties", e);
        }
    }

    public static String getProperty(String key) {
        return INSTANCE.properties.getProperty(key);
    }
}