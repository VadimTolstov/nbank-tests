package models.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdminCredentials {

    LOGIN(AdminConstants.LOGIN),
    PASSWORD(AdminConstants.PASSWORD);

    private final String value;
}
