package models.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record CreateUserJsonResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("username")
        String username,
        @JsonProperty("password")
        String password,
        @JsonProperty("name")
        String name,
        @JsonProperty("role")
        UserRole role,
        @JsonProperty("accounts")
        List<CustomerAccountJson> accounts,

        /** Сколько счетов без баланса создать. Не входит в JSON. */
        @JsonIgnore
        int randomAccounts,

        /** Явные счета с балансами. Не входит в JSON. */
        @JsonIgnore
        List<AccountMeta> accountMetas
) {

    public record AccountMeta(String balance) {
    }
}
