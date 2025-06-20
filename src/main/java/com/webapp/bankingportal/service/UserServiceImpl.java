package com.webapp.bankingportal.service;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.servlet.ModelAndView;

import com.webapp.bankingportal.dto.LoginRequest;
import com.webapp.bankingportal.dto.OtpRequest;
import com.webapp.bankingportal.dto.OtpVerificationRequest;
import com.webapp.bankingportal.dto.UserResponse;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.exception.InvalidTokenException;
import com.webapp.bankingportal.exception.PasswordResetException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.exception.UserInvalidException;
import com.webapp.bankingportal.mapper.UserMapper;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;
import com.webapp.bankingportal.util.ValidationUtil;
import com.webapp.bankingportal.util.ApiMessages;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final GeolocationService geolocationService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ValidationUtil validationUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<String> registerUser(User user) {
        try {
            log.info("Starting user registration for email: {}", user.getEmail());
            validationUtil.validateNewUser(user);
            log.info("User validation passed");
            
            encodePassword(user);
            log.info("Password encoded");
            
            // Save the user first and ensure it's flushed to the database
            val savedUser = userRepository.save(user);
            userRepository.flush();
            log.info("User saved with ID: {}", savedUser.getId());
            
            // Verify the user was saved
            val verifiedUser = userRepository.findById(savedUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found after save"));
            log.info("User verified with ID: {}", verifiedUser.getId());
            
            // Create and save the account using the verified user
            val account = accountService.createAccount(verifiedUser);
            log.info("Account created with number: {}", account.getAccountNumber());
            
            // Set the account to the verified user and save again
            verifiedUser.setAccount(account);
            val finalUser = userRepository.save(verifiedUser);
            userRepository.flush();
            log.info("Account set to user and saved");
            
            log.info("Registration successful. User ID: {}, Account Number: {}", 
                    finalUser.getId(), finalUser.getAccount().getAccountNumber());
            
            return ResponseEntity.ok(JsonUtil.toJson(new UserResponse(finalUser)));
        } catch (Exception e) {
            log.error("Error during user registration: ", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<String> login(LoginRequest loginRequest, HttpServletRequest request) throws InvalidTokenException {
        try {
            log.info("Attempting login for user: {}", loginRequest.identifier());
            
            // First, find the user
            val user = getUserByIdentifier(loginRequest.identifier());
            log.info("User found with ID: {}", user.getId());
            
            // Check if user has an account
            if (user.getAccount() == null) {
                log.info("User has no account, creating new account");
                // Create account in a new transaction
                val account = createAccountInNewTransaction(user);
                log.info("New account created with ID: {} and number: {}", account.getId(), account.getAccountNumber());
                
                // Update user with new account
                user.setAccount(account);
                userRepository.save(user);
                log.info("User updated with new account");
            }
            
            // Verify account exists and has an ID
            val account = user.getAccount();
            if (account == null || account.getId() == null) {
                throw new UserInvalidException("Account not properly created");
            }
            
            log.info("Verifying account with ID: {} and number: {}", account.getId(), account.getAccountNumber());
            
            // Authenticate user using account number
            try {
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        account.getAccountNumber(),
                        loginRequest.password()
                    )
                );
                log.info("User authenticated successfully");
            } catch (BadCredentialsException e) {
                log.error("Authentication failed: {}", e.getMessage());
                throw new UserInvalidException("Invalid credentials");
            }
            
            // Generate token in a new transaction
            log.info("Generating token for account: {}", account.getAccountNumber());
            val token = tokenService.generateAndSaveToken(account.getAccountNumber());
            log.info("Token generated successfully");
            
            // Send notifications asynchronously and handle failures gracefully
            try {
                sendLoginNotification(user, request.getRemoteAddr());
            } catch (Exception e) {
                log.warn("Failed to send login notification: {}", e.getMessage());
            }
            
            // Return token as JSON object
            val response = Map.of("token", token);
            return ResponseEntity.ok(JsonUtil.toJson(response));
        } catch (UserInvalidException e) {
            log.error("Login failed: {}", e.getMessage());
            throw new InvalidTokenException(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage());
            throw new InvalidTokenException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> generateOtp(OtpRequest otpRequest) {
        val user = getUserByIdentifier(otpRequest.identifier());
        val otp = otpService.generateOTP(user.getAccount().getAccountNumber());
        return sendOtpEmail(user, otp);
    }

    @Override
    public ResponseEntity<String> verifyOtpAndLogin(OtpVerificationRequest otpVerificationRequest)
            throws InvalidTokenException {
        validateOtpRequest(otpVerificationRequest);
        val user = getUserByIdentifier(otpVerificationRequest.identifier());
        validateOtp(user, otpVerificationRequest.otp());
        val token = generateAndSaveToken(user.getAccount().getAccountNumber());
        return ResponseEntity.ok(String.format(ApiMessages.TOKEN_ISSUED_SUCCESS.getMessage(), token));
    }

    @Override
    public ResponseEntity<String> updateUser(User updatedUser) {
        val accountNumber = LoggedinUser.getAccountNumber();
        authenticateUser(accountNumber, updatedUser.getPassword());
        val existingUser = getUserByAccountNumber(accountNumber);
        updateUserDetails(existingUser, updatedUser);
        val savedUser = saveUser(existingUser);
        return ResponseEntity.ok(JsonUtil.toJson(new UserResponse(savedUser)));
    }

    @Override
    @Transactional
    public boolean resetPassword(User user, String newPassword) {
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            throw new PasswordResetException(ApiMessages.PASSWORD_RESET_FAILURE.getMessage(), e);
        }
    }

    @Override
    public ModelAndView logout(String token) throws InvalidTokenException {
        token = token.substring(7);
        tokenService.validateToken(token);
        tokenService.invalidateToken(token);

        log.info("User logged out successfully {}", tokenService.getUsernameFromToken(token));

        return new ModelAndView("redirect:/logout");
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User getUserByIdentifier(String identifier) {
        User user = null;

        if (validationUtil.doesEmailExist(identifier)) {
            user = getUserByEmail(identifier);
        } else if (validationUtil.doesAccountExist(identifier)) {
            user = getUserByAccountNumber(identifier);
        } else {
            throw new UserInvalidException(
                    String.format(ApiMessages.USER_NOT_FOUND_BY_IDENTIFIER.getMessage(), identifier));
        }

        return user;
    }

    @Override
    public User getUserByAccountNumber(String accountNo) {
        return userRepository.findByAccountAccountNumber(accountNo).orElseThrow(
                () -> new UserInvalidException(
                        String.format(ApiMessages.USER_NOT_FOUND_BY_ACCOUNT.getMessage(), accountNo)));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UserInvalidException(String.format(ApiMessages.USER_NOT_FOUND_BY_EMAIL.getMessage(), email)));
    }

    private void encodePassword(User user) {
        try {
            log.info("Encoding password for user: {}", user.getEmail());
            user.setCountryCode(user.getCountryCode().toUpperCase());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            log.info("Password encoded successfully");
        } catch (Exception e) {
            log.error("Error encoding password: ", e);
            throw e;
        }
    }

    private User authenticateUser(LoginRequest loginRequest) {
        try {
            log.info("Authenticating user with identifier: {}", loginRequest.identifier());
            
            val user = getUserByIdentifier(loginRequest.identifier());
            log.info("User found with ID: {}", user.getId());
            
            if (user.getAccount() == null) {
                log.error("User has no account associated. User ID: {}", user.getId());
                throw new UserInvalidException("User account not found");
            }
            
            val accountNumber = user.getAccount().getAccountNumber();
            log.info("Authenticating with account number: {}", accountNumber);
            
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(accountNumber, loginRequest.password())
            );
            log.info("Authentication successful for account: {}", accountNumber);
            
            return user;
        } catch (Exception e) {
            log.error("Authentication failed: ", e);
            throw e;
        }
    }

    private void authenticateUser(String accountNumber, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(accountNumber, password));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private String generateAndSaveToken(String accountNumber) throws InvalidTokenException {
        try {
            // First, verify the account exists and has an ID
            val account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new InvalidTokenException("Account not found"));
            
            if (account.getId() == null) {
                throw new InvalidTokenException("Account has no ID");
            }
            
            log.info("Generating token for account: {} with ID: {}", accountNumber, account.getId());
            
            // Generate the token
            val userDetails = userDetailsService.loadUserByUsername(accountNumber);
            val token = tokenService.generateToken(userDetails);
            
            // Save the token
            tokenService.saveToken(token);
            
            return token;
        } catch (Exception e) {
            log.error("Failed to generate and save token: {}", e.getMessage());
            throw new InvalidTokenException("Failed to generate token: " + e.getMessage());
        }
    }

    private ResponseEntity<String> sendOtpEmail(User user, String otp) {
        val emailSendingFuture = otpService.sendOTPByEmail(
                user.getEmail(), user.getName(), user.getAccount().getAccountNumber(), otp);

        ResponseEntity<String> successResponse = ResponseEntity
                .ok(String.format(ApiMessages.OTP_SENT_SUCCESS.getMessage(), user.getEmail()));
        ResponseEntity<String> failureResponse = ResponseEntity.internalServerError()
                .body(String.format(ApiMessages.OTP_SENT_FAILURE.getMessage(), user.getEmail()));

        return emailSendingFuture.thenApply(result -> successResponse)
                .exceptionally(e -> failureResponse).join();
    }

    private void validateOtpRequest(OtpVerificationRequest request) {
        if (request.identifier() == null || request.identifier().isEmpty()) {
            throw new IllegalArgumentException(ApiMessages.IDENTIFIER_MISSING_ERROR.getMessage());
        }
        if (request.otp() == null || request.otp().isEmpty()) {
            throw new IllegalArgumentException(ApiMessages.OTP_MISSING_ERROR.getMessage());
        }
    }

    private void validateOtp(User user, String otp) {
        if (!otpService.validateOTP(user.getAccount().getAccountNumber(), otp)) {
            throw new UnauthorizedException(ApiMessages.OTP_INVALID_ERROR.getMessage());
        }
    }

    private void updateUserDetails(User existingUser, User updatedUser) {
        ValidationUtil.validateUserDetails(updatedUser);
        updatedUser.setPassword(existingUser.getPassword());
        userMapper.updateUser(updatedUser, existingUser);
    }

    private CompletableFuture<Boolean> sendLoginNotification(User user, String ip) {
        val loginTime = new Timestamp(System.currentTimeMillis()).toString();

        return geolocationService.getGeolocation(ip)
                .thenComposeAsync(geolocationResponse -> {
                    try {
                        val loginLocation = String.format("%s, %s",
                                geolocationResponse.getCity().getNames().get("en"),
                                geolocationResponse.getCountry().getNames().get("en"));
                        return sendLoginEmail(user, loginTime, loginLocation);
                    } catch (Exception e) {
                        log.warn("Failed to process geolocation response: {}", e.getMessage());
                        return sendLoginEmail(user, loginTime, "Unknown");
                    }
                })
                .exceptionallyComposeAsync(throwable -> {
                    log.warn("Failed to get geolocation: {}", throwable.getMessage());
                    return sendLoginEmail(user, loginTime, "Unknown");
                });
    }

    private CompletableFuture<Boolean> sendLoginEmail(User user, String loginTime, String loginLocation) {
        try {
            val emailText = emailService.getLoginEmailTemplate(user.getName(), loginTime, loginLocation);
            return emailService.sendEmail(user.getEmail(), ApiMessages.EMAIL_SUBJECT_LOGIN.getMessage(), emailText)
                    .thenApplyAsync(result -> true)
                    .exceptionally(ex -> {
                        log.warn("Failed to send login email: {}", ex.getMessage());
                        return false;
                    });
        } catch (Exception e) {
            log.warn("Failed to prepare login email: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    protected Account createAccountInNewTransaction(User user) {
        try {
            // Re-fetch the user within this new transaction to ensure it's a managed entity
            val managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found in new transaction"));
            
            log.info("Creating new account for user: {}", managedUser.getId());
            val account = accountService.createAccount(managedUser);
            log.info("Account created with ID: {} and number: {}", account.getId(), account.getAccountNumber());
            
            // Verify the account was saved
            val savedAccount = accountRepository.findById(account.getId())
                .orElseThrow(() -> new RuntimeException("Account not found after save"));
            
            log.info("Account verified with ID: {} and number: {}", savedAccount.getId(), savedAccount.getAccountNumber());
            return savedAccount;
        } catch (Exception e) {
            log.error("Failed to create account: {}", e.getMessage());
            throw e;
        }
    }

}
