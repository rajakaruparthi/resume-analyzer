package com.sprint.analyzer.service;

import com.sprint.analyzer.entity.EmailVerificationToken;
import com.sprint.analyzer.entity.User;
import com.sprint.analyzer.properties.EmailVerificationProperties;
import com.sprint.analyzer.repo.EmailVerificationTokenRepository;
import com.sprint.analyzer.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailVerificationService {

    public static final String PURPOSE = "EMAIL_VERIFICATION";

    private final EmailVerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final EmailVerificationProperties props;

    public void sendVerificationFor(User user) {
        tokenRepo.deleteAllByUserAndPurpose(user.getId(), PURPOSE);

        String tokenValue = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");  // 64-char opaque token

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(tokenValue)
                .purpose(PURPOSE)
                .expiresAt(LocalDateTime.now().plusMinutes(props.getVerification().getTokenTtlMinutes()))
                .build();
        tokenRepo.save(token);

        String link = props.getVerification().getFrontendUrl()
                + "/verify-email?token=" + tokenValue;

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), link);
        log.info("Verification token issued for user {}", user.getEmail());
    }

    public void verify(String tokenValue) {
        EmailVerificationToken token = tokenRepo.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (!token.isValid()) {
            throw new IllegalArgumentException(
                    token.isExpired() ? "Verification link has expired" : "Verification link already used");
        }

        User user = userRepo.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEmailVerified()) {
            // idempotent: mark token used and return success
            token.setUsedAt(LocalDateTime.now());
            tokenRepo.save(token);
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepo.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepo.save(token);

        log.info("Email verified for user {}", user.getEmail());
    }

    public void resend(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email already verified");
        }
        sendVerificationFor(user);
    }
}