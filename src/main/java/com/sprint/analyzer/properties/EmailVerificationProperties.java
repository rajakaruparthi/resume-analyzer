package com.sprint.analyzer.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class EmailVerificationProperties {

    private Mail mail = new Mail();
    private Verification verification = new Verification();

    @Data
    public static class Mail {
        private String from;
        private String fromName;
    }

    @Data
    public static class Verification {
        private int tokenTtlMinutes = 60;
        private String baseUrl;
        private String frontendUrl;
    }
}