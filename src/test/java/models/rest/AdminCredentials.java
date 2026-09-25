package models.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdminCredentials {
    LOGIN("admin"),
    PASSWORD("admin");

    private final String value;

}
