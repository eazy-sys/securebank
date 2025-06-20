package com.webapp.bankingportal.service;

import static org.springframework.security.core.userdetails.User.withUsername;

import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.Token;
import com.webapp.bankingportal.exception.InvalidTokenException;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.repository.TokenRepository;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.util.ApiMessages;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final AccountRepository accountRepository;

    @Override
    public String getUsernameFromToken(String token) throws InvalidTokenException {
        return getClaimFromToken(token, Claims::getSubject);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        log.info("Generating token for user: {}", userDetails.getUsername());
        return doGenerateToken(userDetails,
                new Date(System.currentTimeMillis() + expiration));
    }

    @Override
    public String generateToken(UserDetails userDetails, Date expiry) {
        log.info("Generating token for user: {}", userDetails.getUsername());
        return doGenerateToken(userDetails, expiry);
    }

    private String doGenerateToken(UserDetails userDetails, Date expiry) {
        return Jwts.builder().setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    @Override
    public UserDetails loadUserByUsername(String accountNumber) throws UsernameNotFoundException {
        val user = userRepository.findByAccountAccountNumber(accountNumber)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format(ApiMessages.USER_NOT_FOUND_BY_ACCOUNT.getMessage(), accountNumber)));

        return withUsername(accountNumber).password(user.getPassword()).build();
    }

    @Override
    public Date getExpirationDateFromToken(String token) throws InvalidTokenException {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    @Override
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver)
            throws InvalidTokenException {
        val claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) throws InvalidTokenException {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            invalidateToken(token);
            throw new InvalidTokenException(ApiMessages.TOKEN_EXPIRED_ERROR.getMessage());
        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException(ApiMessages.TOKEN_UNSUPPORTED_ERROR.getMessage());
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException(ApiMessages.TOKEN_MALFORMED_ERROR.getMessage());
        } catch (SignatureException e) {
            throw new InvalidTokenException(ApiMessages.TOKEN_SIGNATURE_INVALID_ERROR.getMessage());
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException(ApiMessages.TOKEN_EMPTY_ERROR.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateToken(String token) throws InvalidTokenException {
        try {
            getAllClaimsFromToken(token);
            if (tokenRepository.findByToken(token) == null) {
                throw new InvalidTokenException(ApiMessages.TOKEN_NOT_FOUND_ERROR.getMessage());
            }
        } catch (InvalidTokenException e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public String generateAndSaveToken(String accountNumber) throws InvalidTokenException {
        try {
            // First, verify the account exists and has an ID
            val account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new InvalidTokenException("Account not found"));
            
            if (account.getId() == null) {
                throw new InvalidTokenException("Account has no ID");
            }
            
            log.info("Generating token for account: {} with ID: {}", accountNumber, account.getId());
            
            // Generate the token
            val userDetails = loadUserByUsername(accountNumber);
            val token = generateToken(userDetails);
            
            // Check if token already exists
            if (tokenRepository.findByToken(token) != null) {
                throw new InvalidTokenException(ApiMessages.TOKEN_ALREADY_EXISTS_ERROR.getMessage());
            }
            
            // Create and save the token
            val tokenObj = new Token();
            tokenObj.setToken(token);
            tokenObj.setExpiryAt(getExpirationDateFromToken(token));
            tokenObj.setAccount(account);  // Set the account reference
            tokenObj.setCreatedAt(new Date());
            
            // Save the token and verify it was saved
            val savedToken = tokenRepository.save(tokenObj);
            if (savedToken.getId() == null) {
                throw new InvalidTokenException("Failed to save token - no ID generated");
            }
            
            log.info("Token saved successfully with ID: {} for account: {} with account ID: {}", 
                    savedToken.getId(), account.getAccountNumber(), account.getId());
            
            return token;
        } catch (InvalidTokenException e) {
            log.error("Token generation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token generation: {}", e.getMessage());
            throw new InvalidTokenException("Failed to generate token: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveToken(String token) throws InvalidTokenException {
        try {
            if (tokenRepository.findByToken(token) != null) {
                throw new InvalidTokenException(ApiMessages.TOKEN_ALREADY_EXISTS_ERROR.getMessage());
            }

            val accountNumber = getUsernameFromToken(token);
            val account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new InvalidTokenException(ApiMessages.USER_NOT_FOUND_BY_ACCOUNT.getMessage()));

            if (account.getId() == null) {
                throw new InvalidTokenException("Account has no ID");
            }

            log.info("Saving token for account: {} with ID: {}", account.getAccountNumber(), account.getId());

            val tokenObj = new Token();
            tokenObj.setToken(token);
            tokenObj.setExpiryAt(getExpirationDateFromToken(token));
            tokenObj.setAccount(account);  // Set the account reference
            tokenObj.setCreatedAt(new Date());

            val savedToken = tokenRepository.save(tokenObj);
            if (savedToken.getId() == null) {
                throw new InvalidTokenException("Failed to save token - no ID generated");
            }
            
            log.info("Token saved successfully with ID: {} for account: {} with account ID: {}", 
                    savedToken.getId(), account.getAccountNumber(), account.getId());
        } catch (InvalidTokenException e) {
            log.error("Failed to save token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token save: {}", e.getMessage());
            throw new InvalidTokenException("Failed to save token: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidateToken(String token) {
        try {
            if (tokenRepository.findByToken(token) != null) {
                tokenRepository.deleteByToken(token);
                log.info("Token invalidated successfully: {}", token);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate token: {}", e.getMessage());
            throw e;
        }
    }

}
