package com.sprint.analyzer.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    /**
     * Base64 encoded secret key for signing JWTs.
     * Should be at least 32 bytes (256 bits) for HS256.
     * Generate with: `openssl rand -base64 32`
     */
    private String secret;

    /**
     * Expiration time for access tokens in minutes.
     */
    private long accessTokenExpirationMinutes = 15;

    /**
     * Expiration time for refresh tokens in minutes.
     */
    private long refreshTokenExpirationMinutes = 1440; // 24 hours
}