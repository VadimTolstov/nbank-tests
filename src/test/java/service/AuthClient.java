package service;

import lombok.NonNull;

public interface AuthClient {

    String authUser(@NonNull String username, @NonNull String password);
}
