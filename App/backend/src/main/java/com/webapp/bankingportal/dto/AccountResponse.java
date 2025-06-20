package com.webapp.bankingportal.dto;

import java.math.BigDecimal;
import com.webapp.bankingportal.entity.Account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String accountNumber;
    private BigDecimal balance;
    private String accountType;

    public AccountResponse(Account account) {
        this.accountNumber = account.getAccountNumber();
        this.balance = account.getBalance();
        this.accountType = account.getAccountType().name();
    }

}
