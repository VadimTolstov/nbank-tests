package config;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public interface Config {
    static Config getInstance() {
        return "docker".equals(System.getProperty("test.env"))
                ? DockerConfig.instance
                : LocalConfig.instance;
    }

    @NotNull
    String frontUrl();


    default @NotNull String url(@NonNull String type, @NonNull String host, @NonNull String port) {
        return String.format("%s://%s:%s/", type, host, port);
    }
}
