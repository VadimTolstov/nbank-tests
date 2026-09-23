package models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;

@NoArgsConstructor
@Getter
@ToString
public class GetCustomerAccountsResponse extends ArrayList<CustomerAccount> {
}
