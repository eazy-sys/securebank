package com.webapp.bankingportal.service;

import java.math.BigDecimal;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.dto.AccountResponse;

public interface AccountService {

	public Account createAccount(User user);
	public boolean isPinCreated(String accountNumber) ;
	public void createPin(String accountNumber, String pin) ;
	public void updatePin(String accountNumber, String oldPin, String newPin);
	public AccountResponse cashDeposit(String accountNumber, String pin, BigDecimal amount);
	public AccountResponse cashWithdrawal(String accountNumber, String pin, BigDecimal amount);
	public AccountResponse fundTransfer(String sourceAccountNumber, String targetAccountNumber, BigDecimal amount, String pin);
	public void deleteAccount(String accountNumber);
	public AccountResponse getAccountInfo(String accountNumber);
	
	
}
