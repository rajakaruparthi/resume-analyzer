package com.sprint.analyzer.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AWS S3 configuration properties.
 * Binds to 'aws.s3.*' properties in application.yaml.
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class AwsProperties {

    private String bucketName;
    private String region;
    private Long presignedUrlExpirationMinutes = 15L; // Default 15 minutes
    private String accessKeyId;
    private String secretAccessKey;

}

