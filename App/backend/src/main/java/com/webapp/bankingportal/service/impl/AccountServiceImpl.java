package com.webapp.bankingportal.service.impl;

import com.webapp.bankingportal.dto.AccountResponse;
import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.Transaction;
import com.webapp.bankingportal.entity.TransactionType;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.repository.TransactionRepository;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.util.ApiMessages;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Account createAccount(User user) {
        try {
            log.info("Creating new account for user: {}", user.getId());
            Account account = new Account();
            account.setUser(user);
            account.setBalance(BigDecimal.ZERO);
            account.setAccountNumber(generateAccountNumber());
            account.setPin("0000");
            
            val savedAccount = accountRepository.save(account);
            accountRepository.flush(); // Ensure account is written to DB immediately
            log.info("Account saved with ID: {} and number: {}", savedAccount.getId(), savedAccount.getAccountNumber());
            
            return savedAccount;
        } catch (Exception e) {
            log.error("Failed to create account: {}", e.getMessage());
            throw e;
        }
    }

    private String generateAccountNumber() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    @Override
    @Transactional
    public void createPin(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (account.getPin() != null) {
            throw new RuntimeException("PIN already exists");
        }
        
        account.setPin(pin);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void updatePin(String accountNumber, String oldPin, String newPin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getPin().equals(oldPin)) {
            throw new UnauthorizedException("Invalid PIN");
        }
        
        if (newPin == null || newPin.length() != 4 || !newPin.matches("^[0-9]*$")) {
            throw new RuntimeException("Invalid PIN format");
        }
        
        account.setPin(newPin);
        accountRepository.save(account);
    }

    @Override
    public boolean isPinCreated(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> account.getPin() != null)
                .orElse(false);
    }

    @Override
    @Transactional
    public void deleteAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        accountRepository.delete(account);
    }

    private void validateAccountAndPin(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getPin().equals(pin)) {
            throw new UnauthorizedException("Invalid PIN");
        }
    }

    @Override
    @Transactional
    public AccountResponse cashDeposit(String accountNumber, String pin, BigDecimal amount) {
        log.info("Processing cash deposit for account: {} with amount: {}", accountNumber, amount);
        
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(ApiMessages.AMOUNT_NEGATIVE_ERROR.getMessage());
        }
        
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException(ApiMessages.AMOUNT_EXCEED_100_000_ERROR.getMessage());
        }
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        log.info("Found account with ID: {} and number: {}", account.getId(), account.getAccountNumber());
        
        validateAccountAndPin(accountNumber, pin);
        
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        
        // Save account and verify it has an ID
        account = accountRepository.save(account);
        if (account.getId() == null) {
            throw new RuntimeException("Failed to save account - no ID generated");
        }
        
        log.info("Account updated with ID: {} and new balance: {}", account.getId(), newBalance);
        
        // Verify account exists in database
        val verifiedAccount = accountRepository.findById(account.getId())
            .orElseThrow(() -> new RuntimeException("Account not found after save"));
        log.info("Verified account exists with ID: {} and number: {}", verifiedAccount.getId(), verifiedAccount.getAccountNumber());
        
        // Record the transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_DEPOSIT);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(verifiedAccount);  // Use the verified account
        
        log.info("Creating transaction with source account ID: {}", verifiedAccount.getId());
        
        // Save transaction and verify it was saved
        val savedTransaction = transactionRepository.save(transaction);
        if (savedTransaction.getId() == null) {
            throw new RuntimeException("Failed to save transaction - no ID generated");
        }
        
        log.info("Transaction saved with ID: {} for account: {}", savedTransaction.getId(), accountNumber);
        return new AccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse cashWithdrawal(String accountNumber, String pin, BigDecimal amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        validateAccountAndPin(accountNumber, pin);
        
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);
        
        // Record the transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_WITHDRAWAL);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(account);
        transactionRepository.save(transaction);
        
        return new AccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse fundTransfer(String sourceAccountNumber, String targetAccountNumber, BigDecimal amount, String pin) {
        Account sourceAccount = accountRepository.findByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new RuntimeException("Source account not found"));
        
        Account targetAccount = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new RuntimeException("Target account not found"));
        
        validateAccountAndPin(sourceAccountNumber, pin);
        
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(amount);
        BigDecimal newTargetBalance = targetAccount.getBalance().add(amount);
        
        sourceAccount.setBalance(newSourceBalance);
        targetAccount.setBalance(newTargetBalance);
        
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);
        
        // Record the transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_TRANSFER);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(sourceAccount);
        transaction.setTargetAccount(targetAccount);
        transactionRepository.save(transaction);
        
        return new AccountResponse(sourceAccount);
    }

    @Override
    public AccountResponse getAccountInfo(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return new AccountResponse(account);
    }
} 