package com.webapp.bankingportal.dto;

import com.webapp.bankingportal.entity.User;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UserResponse {

    private String name;
    private String email;
    private String countryCode;
    private String phoneNumber;
    private String address;
    private String accountNumber;
    private String accountType;

    public UserResponse(User user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.countryCode = user.getCountryCode();
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        this.accountNumber = user.getAccount().getAccountNumber();
        this.accountType = user.getAccount().getAccountType().name();
    }

}
