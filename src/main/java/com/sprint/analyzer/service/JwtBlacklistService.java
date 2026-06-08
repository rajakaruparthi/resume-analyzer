package com.sprint.analyzer.service;

import com.sprint.analyzer.entity.JwtBlacklist;
import com.sprint.analyzer.repo.JwtBlacklistRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtBlacklistService {

    private final JwtBlacklistRepository blacklistRepository;
    private final JwtService jwtService; // To extract expiry from token

    /**
     * Adds a JWT to the blacklist.
     *
     * @param token The JWT string to blacklist.
     */
    @Transactional
    public void blacklistToken(String token) {
        if (blacklistRepository.existsByToken(token)) {
            log.warn("Attempted to blacklist an already blacklisted token.");
            return;
        }

        Date expiryDate = jwtService.extractClaim(token, Claims::getExpiration);
        LocalDateTime localExpiryDate = Instant.ofEpochMilli(expiryDate.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        JwtBlacklist blacklistedToken = JwtBlacklist.builder()
                .token(token)
                .expiryDate(localExpiryDate)
                .build();

        blacklistRepository.save(blacklistedToken);
        log.info("Token blacklisted successfully. Expires: {}", localExpiryDate);
    }

    /**
     * Checks if a JWT is present in the blacklist.
     *
     * @param token The JWT string to check.
     * @return True if the token is blacklisted, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String token) {
        return blacklistRepository.existsByToken(token);
    }

    /**
     * Scheduled task to clean up expired tokens from the blacklist.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired JWT blacklist tokens.");
        LocalDateTime now = LocalDateTime.now();
        blacklistRepository.deleteExpiredTokens(now);
        log.info("Finished cleanup of expired JWT blacklist tokens.");
    }
}