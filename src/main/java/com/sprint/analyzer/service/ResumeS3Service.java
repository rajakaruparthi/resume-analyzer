package com.sprint.analyzer.service;

import com.sprint.analyzer.connector.AwsConnector;
import com.sprint.analyzer.model.S3ObjectDto;
import com.sprint.analyzer.properties.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "aws.s3", name = "bucket-name")
public class ResumeS3Service {

    private final AwsConnector awsConnector;
    private final AwsProperties awsProperties;

    public ResumeS3Service(AwsConnector awsConnector, AwsProperties awsProperties) {
        this.awsConnector = awsConnector;
        this.awsProperties = awsProperties;
    }


    public String getBucketNameForUser(UUID userId) {
        String baseBucket = awsProperties.getBucketName();
        if (baseBucket == null || baseBucket.isBlank()) {
            return userId.toString().toLowerCase();
        }
        return (baseBucket + "-" + userId.toString()).toLowerCase();
    }

    public void createBucketIfNotExists(String bucketName) {
        awsConnector.createBucketIfNotExists(bucketName);
    }

    public List<S3ObjectDto> listAllResumes(String bucketName) {
        log.info("Listing all objects in S3 bucket: {}", bucketName);

        if (bucketName == null || bucketName.isBlank()) {
            return Collections.emptyList();
        }
        List<S3Object> s3Objects = awsConnector.listObjects(bucketName);
        return s3Objects.stream()
                .map(s3Object -> S3ObjectDto.builder()
                        .key(s3Object.key())
                        .size(s3Object.size())
                        .lastModified(s3Object.lastModified())
                        .eTag(s3Object.eTag())
                        .build())
                .collect(Collectors.toList());
    }


    public String uploadFile(String bucketName, String fileName, InputStream fileStream, String contentType) {
        try {
            String s3Key = awsConnector.uploadFile(bucketName, fileName, fileStream, contentType);
            log.info("File uploaded successfully. S3 Key: {}, ContentType: {}", s3Key, contentType);
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    public String uploadResume(String bucketName, String fileName, InputStream fileStream) {
        return uploadFile(bucketName, fileName, fileStream, "application/pdf");
    }

    public String uploadJson(String bucketName, String fileName, String content) {
        try (InputStream in = new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return uploadFile(bucketName, fileName, in, "application/json");
        } catch (Exception e) {
            log.error("Failed to upload JSON to S3: {}", e.getMessage(), e);
            throw new RuntimeException("JSON upload failed: " + e.getMessage(), e);
        }
    }

    public String downloadFileContent(String bucketName, String fileName) {
        try (InputStream in = awsConnector.downloadFileStream(bucketName, fileName)) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to download file content from S3. Bucket: {}, Key: {}", bucketName, fileName, e);
            throw new RuntimeException("S3 download failed: " + e.getMessage(), e);
        }
    }

    public String getPresignedUploadUrl(String bucketName, String fileName) {
        try {
            String presignedUrl = awsConnector.generatePresignedUploadUrl(bucketName, fileName, "application/pdf");
            log.info("Presigned upload URL generated for resume: {}", fileName);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL: {}", e.getMessage(), e);
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    public String getPresignedDownloadUrl(String bucketName, String fileName) {
        try {
            String presignedUrl = awsConnector.generatePresignedDownloadUrl(bucketName, fileName);
            log.info("Presigned download URL generated for resume: {}", fileName);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL: {}", e.getMessage(), e);
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    public boolean resumeExists(String bucketName, String fileName) {
        return awsConnector.fileExists(bucketName, fileName);
    }

    public boolean deleteResume(String bucketName, String fileName) {
        return awsConnector.deleteFile(bucketName, fileName);
    }
}