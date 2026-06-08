package com.sprint.analyzer.config;

import com.sprint.analyzer.service.JwtBlacklistService; // Import new service
import com.sprint.analyzer.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService; // Inject JwtBlacklistService

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Check for Authorization header and Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Extract JWT string

        // 2. Check if token is blacklisted
        if (jwtBlacklistService.isTokenBlacklisted(jwt)) {
            log.warn("Attempted to use a blacklisted JWT: {}", jwt);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            response.getWriter().write("Token has been invalidated.");
            return;
        }

        userEmail = jwtService.extractUsername(jwt); // Extract username (email) from JWT

        // 3. If username is found and no authentication is currently set in SecurityContext
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 4. Validate the token (signature and expiration)
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // If valid, create an authentication token and set it in the SecurityContext
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Credentials are null for JWT (already validated)
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user: {}", userEmail);
            } else {
                log.warn("Invalid JWT token for user: {}", userEmail);
                // No need to set 401 here, as invalid tokens will be caught by Spring Security's exception handling
            }
        }

        // 5. Continue the filter chain
        filterChain.doFilter(request, response);
    }
}