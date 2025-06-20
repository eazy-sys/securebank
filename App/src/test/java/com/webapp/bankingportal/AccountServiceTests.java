package com.webapp.bankingportal;

import jakarta.validation.ConstraintViolationException;
import lombok.val;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.InvalidPinException;
import com.webapp.bankingportal.exception.NotFoundException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.repository.AccountRepository;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountServiceTests extends BaseTest {

    @Autowired
    AccountRepository accountRepository;

    @Test
    public void test_create_account_with_valid_user() {
        val user = createUser();
        userRepository.save(user);

        val account = accountService.createAccount(user);

        Assertions.assertNotNull(account);
        Assertions.assertNotNull(account.getAccountNumber());
        Assertions.assertEquals(user, account.getUser());
        Assertions.assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    public void test_create_account_with_null_user() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> accountService.createAccount(null));
    }

    @Test
    public void test_create_pin_with_valid_details() {
        val accountDetails = createAccount();

        val pin = getRandomPin();

        accountService.createPin(accountDetails.get("accountNumber"), pin);

        val account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Assertions.assertTrue(passwordEncoder.matches(pin, account.getPin()));
    }

    @Test
    public void test_create_pin_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.createPin(getRandomAccountNumber(), getRandomPin());
        });
    }

    @Test
    public void test_create_pin_with_invalid_password() {
        val accountNumber = createAccount()
                .get("accountNumber");

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.createPin(accountNumber, getRandomPin());
        });
    }

    @Test
    public void test_create_pin_with_existing_pin() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), getRandomPin());
        });
    }

    @Test
    public void test_create_pin_with_missing_or_empty_pin() {
        val accountDetails = createAccount();

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), null);
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), "");
        });
    }

    @Test
    public void test_create_pin_with_invalid_format() {
        val accountDetails = createAccount();

        // Short pin
        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), faker.number().digits(3));
        });

        // Long pin
        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), faker.number().digits(5));
        });

        // Invalid format
        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), getRandomPassword().substring(0, 4));
        });
    }

    @Test
    public void test_update_pin_with_valid_details() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        val newPin = getRandomPin();

        accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"), newPin);

        val account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Assertions.assertTrue(passwordEncoder.matches(newPin, account.getPin()));
    }

    @Test
    public void test_update_pin_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.updatePin(getRandomAccountNumber(), getRandomPin(), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_incorrect_password() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), getRandomPin(), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_password() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), null, getRandomPin());
        });

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), "", getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_incorrect_pin() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), getRandomPin(), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_for_account_with_no_pin() {
        val accountDetails = createAccount();

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), getRandomPin(), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_old_pin() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), null, getRandomPin());
        });

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), "", getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_new_pin() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"), null);
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"), "");
        });
    }

    @Test
    public void test_deposit_cash_with_valid_details() {
        val balance = new BigDecimal("1000.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        val account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Assertions.assertEquals(balance, account.getBalance());
    }

    @Test
    public void test_deposit_cash_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.cashDeposit(getRandomAccountNumber(), getRandomPin(), new BigDecimal("50.00"));
        });
    }

    @Test
    public void test_deposit_cash_with_invalid_pin() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), getRandomPin(), new BigDecimal("50.00"));
        });
    }

    @Test
    public void test_deposit_invalid_amount() {
        val accountDetails = createAccountWithPin(passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), new BigDecimal("-50.00"));
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), BigDecimal.ZERO);
        });
    }

    @Test
    public void test_withdraw_cash_with_valid_details() {
        val balance = new BigDecimal("1000.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        val amount = new BigDecimal("500.00");
        val response = accountService.cashWithdrawal(accountDetails.get("accountNumber"), accountDetails.get("pin"), amount);

        val account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Assertions.assertEquals(balance.subtract(amount), account.getBalance());
    }

    @Test
    public void test_withdraw_insufficient_balance() {
        val balance = new BigDecimal("100.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.cashWithdrawal(accountDetails.get("accountNumber"), accountDetails.get("pin"), new BigDecimal("200.00"));
        });
    }

    @Test
    public void test_transfer_funds_with_valid_accounts() {
        val balance = new BigDecimal("1000.00");
        val sourceAccountDetails = createAccountWithInitialBalance(balance);
        val targetAccountDetails = createAccountWithInitialBalance(BigDecimal.ZERO);

        val amount = new BigDecimal("500.00");
        val response = accountService.fundTransfer(
                sourceAccountDetails.get("accountNumber"),
                targetAccountDetails.get("accountNumber"),
                amount,
                sourceAccountDetails.get("pin"));

        val sourceAccount = accountRepository
                .findByAccountNumber(sourceAccountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Source account not found"));
        val targetAccount = accountRepository
                .findByAccountNumber(targetAccountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Target account not found"));

        Assertions.assertEquals(balance.subtract(amount), sourceAccount.getBalance());
        Assertions.assertEquals(amount, targetAccount.getBalance());
    }

    @Test
    public void test_transfer_non_existent_target_account() {
        val balance = new BigDecimal("1000.00");
        val sourceAccountDetails = createAccountWithInitialBalance(balance);

        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.fundTransfer(
                    sourceAccountDetails.get("accountNumber"),
                    getRandomAccountNumber(),
                    new BigDecimal("500.00"),
                    sourceAccountDetails.get("pin"));
        });
    }

    @Test
    public void test_transfer_funds_insufficient_balance() {
        val balance = new BigDecimal("100.00");
        val sourceAccountDetails = createAccountWithInitialBalance(balance);
        val targetAccountDetails = createAccountWithInitialBalance(BigDecimal.ZERO);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.fundTransfer(
                    sourceAccountDetails.get("accountNumber"),
                    targetAccountDetails.get("accountNumber"),
                    new BigDecimal("200.00"),
                    sourceAccountDetails.get("pin"));
        });
    }

    @Test
    void testCashDeposit() {
        val balance = new BigDecimal("1000.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        val amount = new BigDecimal("500.00");
        val response = accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), amount);

        val account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Assertions.assertEquals(balance.add(amount), account.getBalance());
    }

    @Test
    void testCashWithdrawal() {
        val balance = new BigDecimal("1000.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        val amount = new BigDecimal("500.00");
        val response = accountService.cashWithdrawal(accountDetails.get("accountNumber"), accountDetails.get("pin"), amount);

        val account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Assertions.assertEquals(balance.subtract(amount), account.getBalance());
    }

    @Test
    void testFundTransfer() {
        val balance = new BigDecimal("1000.00");
        val sourceAccountDetails = createAccountWithInitialBalance(balance);
        val targetAccountDetails = createAccountWithInitialBalance(BigDecimal.ZERO);

        val amount = new BigDecimal("500.00");
        val response = accountService.fundTransfer(
                sourceAccountDetails.get("accountNumber"),
                targetAccountDetails.get("accountNumber"),
                amount,
                sourceAccountDetails.get("pin"));

        val sourceAccount = accountRepository
                .findByAccountNumber(sourceAccountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Source account not found"));
        val targetAccount = accountRepository
                .findByAccountNumber(targetAccountDetails.get("accountNumber"))
                .orElseThrow(() -> new RuntimeException("Target account not found"));

        Assertions.assertEquals(balance.subtract(amount), sourceAccount.getBalance());
        Assertions.assertEquals(amount, targetAccount.getBalance());
    }

    @Test
    void testCashDepositWithInvalidPin() {
        val balance = new BigDecimal("1000.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), getRandomPin(), new BigDecimal("500.00"));
        });
    }

    @Test
    void testCashDepositWithNegativeAmount() {
        val balance = new BigDecimal("1000.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), new BigDecimal("-500.00"));
        });
    }

    @Test
    void testCashWithdrawalWithInsufficientFunds() {
        val balance = new BigDecimal("100.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.cashWithdrawal(accountDetails.get("accountNumber"), accountDetails.get("pin"), new BigDecimal("200.00"));
        });
    }

    @Test
    void testFundTransferWithInsufficientFunds() {
        val balance = new BigDecimal("100.00");
        val sourceAccountDetails = createAccountWithInitialBalance(balance);
        val targetAccountDetails = createAccountWithInitialBalance(BigDecimal.ZERO);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.fundTransfer(
                    sourceAccountDetails.get("accountNumber"),
                    targetAccountDetails.get("accountNumber"),
                    new BigDecimal("200.00"),
                    sourceAccountDetails.get("pin"));
        });
    }

    @Test
    void testFundTransferWithNegativeAmount() {
        val balance = new BigDecimal("1000.00");
        val sourceAccountDetails = createAccountWithInitialBalance(balance);
        val targetAccountDetails = createAccountWithInitialBalance(BigDecimal.ZERO);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            accountService.fundTransfer(
                    sourceAccountDetails.get("accountNumber"),
                    targetAccountDetails.get("accountNumber"),
                    new BigDecimal("-500.00"),
                    sourceAccountDetails.get("pin"));
        });
    }

}
