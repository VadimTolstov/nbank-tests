package config;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public enum LocalConfig implements Config {
    instance;

    @NotNull
    private String localhost() {
        return "127.0.0.1";
    }

    private @NotNull String serverUrl(@NonNull String port) {
        return url("http", localhost(), port);
    }

    @Override
    public @NotNull String frontUrl() {
        return serverUrl("4111");
    }
}
