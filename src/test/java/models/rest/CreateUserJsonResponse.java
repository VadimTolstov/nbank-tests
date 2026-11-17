package models.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
) implements BaseModel {

    public record AccountMeta(String balance) {
    }

    public CustomerAccountJson getFirstAccount() {
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalStateException("У пользователя '" + username + "' нет ни одного счета");
        }
        return accounts.getFirst();
    }

    public String getFirstAccountNumber() {
        return getFirstAccount().accountNumber();
    }

    public Long getAccountId() {
        return getFirstAccount().id();
    }
}
