package com.sprint.analyzer.service;

import com.sprint.analyzer.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signInKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signInKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    /**
     * Extracts the username (subject) from a JWT.
     *
     * @param token The JWT.
     * @return The username.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from a JWT.
     *
     * @param token          The JWT.
     * @param claimsResolver Function to resolve the desired claim.
     * @param <T>            Type of the claim.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates an access token for a given UserDetails.
     *
     * @param userDetails The user details.
     * @return The generated access token.
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtProperties.getAccessTokenExpirationMinutes());
    }

    /**
     * Generates a refresh token for a given UserDetails.
     *
     * @param userDetails The user details.
     * @return The generated refresh token.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtProperties.getRefreshTokenExpirationMinutes());
    }

    /**
     * Generates a JWT with extra claims, subject, and expiration.
     *
     * @param extraClaims       Additional claims to include.
     * @param userDetails       The user details (subject).
     * @param expirationMinutes Expiration time in minutes.
     * @return The generated JWT.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMinutes) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + expirationMinutes * 60L * 1000L)
                )
                .signWith(signInKey)
                .compact();
    }

    /**
     * Validates if a JWT is valid for a given UserDetails.
     *
     * @param token       The JWT.
     * @param userDetails The user details.
     * @return True if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks if a JWT has expired.
     *
     * @param token The JWT.
     * @return True if the token has expired, false otherwise.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT.
     *
     * @param token The JWT.
     * @return The expiration date.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) signInKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token");
        }
    }
}