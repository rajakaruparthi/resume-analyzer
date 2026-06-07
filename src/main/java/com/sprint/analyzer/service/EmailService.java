package com.sprint.analyzer.service;

import com.sprint.analyzer.properties.EmailVerificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailVerificationProperties props;

    @Async
    public void sendVerificationEmail(String toEmail, String userName, String verificationLink) {
        try {
            Context ctx = new Context();
            ctx.setVariable("userName", userName);
            ctx.setVariable("verificationLink", verificationLink);
            ctx.setVariable("expiryMinutes", props.getVerification().getTokenTtlMinutes());

            String html = templateEngine.process("verification-email", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(props.getMail().getFrom(), props.getMail().getFromName());
            helper.setTo(toEmail);
            helper.setSubject("Verify your email — Resume Analyzer");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            // swallow: we don't want email failures to break the registration response
        }
    }
}