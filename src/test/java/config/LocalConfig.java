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
    public @NotNull String nbankUrl() {
        return serverUrl("4111");
    }

    @Override
    public @NotNull String nbankUiUrl() {
        return "http://192.168.100.2:3000";
    }

    @Override
    public @NotNull String selenoidUrl() {
        return "http://localhost:4444/wd/hub";
    }
}