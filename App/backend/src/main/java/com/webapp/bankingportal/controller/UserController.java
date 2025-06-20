package com.webapp.bankingportal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.webapp.bankingportal.dto.LoginRequest;
import com.webapp.bankingportal.dto.OtpRequest;
import com.webapp.bankingportal.dto.OtpVerificationRequest;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.InvalidTokenException;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.UserService;
import com.webapp.bankingportal.util.JsonUtil;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request)
            throws InvalidTokenException {

        return userService.login(loginRequest, request);
    }

    @PostMapping("/generate-otp")
    public ResponseEntity<String> generateOtp(@RequestBody OtpRequest otpRequest) {
        return userService.generateOtp(otpRequest);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtpAndLogin(@RequestBody OtpVerificationRequest otpVerificationRequest)
            throws InvalidTokenException {

        return userService.verifyOtpAndLogin(otpVerificationRequest);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }

    @GetMapping("/logout")
    public ModelAndView logout(@RequestHeader("Authorization") String token)
            throws InvalidTokenException {

        return userService.logout(token);
    }

    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(@RequestBody Map<String, String> request) {
        try {
            String accountNumber = request.get("accountNumber");
            if (accountNumber == null) {
                return ResponseEntity.badRequest().body("Account number is required");
            }
            accountService.deleteAccount(accountNumber);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting account: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting account: " + e.getMessage());
        }
    }

    @GetMapping("/account-info")
    public ResponseEntity<String> getAccountInfo(@RequestParam String email) {
        try {
            log.info("Looking up account info for email: {}", email);
            
            // First try to find the user
            User user;
            try {
                user = userService.getUserByEmail(email);
            } catch (Exception e) {
                log.error("User not found: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with email: " + email);
            }
            
            // Check if user has an account
            if (user.getAccount() == null) {
                log.error("No account found for user: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No account found for user: " + email);
            }
            
            // Build response with account details
            Map<String, Object> response = Map.of(
                "email", user.getEmail(),
                "name", user.getName(),
                "accountNumber", user.getAccount().getAccountNumber(),
                "balance", user.getAccount().getBalance(),
                "accountType", user.getAccount().getAccountType(),
                "accountStatus", user.getAccount().getAccountStatus()
            );
            
            log.info("Found account info for user: {}", email);
            return ResponseEntity.ok(JsonUtil.toJson(response));
            
        } catch (Exception e) {
            log.error("Error getting account info: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error getting account info: " + e.getMessage());
        }
    }

}
