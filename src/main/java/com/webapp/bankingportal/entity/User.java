package com.webapp.bankingportal.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    @NotEmpty
    private String password;

    @Email
    @NotEmpty
    @Column(unique = true)
    private String email;

    @NotEmpty
    private String countryCode;

    @NotEmpty
    @Column(unique = true)
    private String phoneNumber;

    @NotEmpty
    private String address;

    // Establishing a one-to-one relationship with the account
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Account account;

    // Helper method to set the account and maintain bidirectional relationship
    public void setAccount(Account account) {
        if (this.account != null) {
            this.account.setUser(null);
        }
        this.account = account;
        if (account != null) {
            account.setUser(this);
        }
    }
}
