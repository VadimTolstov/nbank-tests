package config;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public enum DockerConfig implements Config {
    instance;

    @Override
    public @NotNull String nbankUrl() {
        return "";
    }

    @Override
    public @NotNull String nbankUiUrl() {
        return "";
    }

    @Override
    public @NotNull String selenoidUrl() {
        return "";
    }
}
