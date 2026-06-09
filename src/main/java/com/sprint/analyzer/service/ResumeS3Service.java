package com.sprint.analyzer.service;

import com.sprint.analyzer.connector.AwsConnector;
import com.sprint.analyzer.model.S3ObjectDto;
import com.sprint.analyzer.properties.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.List;
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


    public List<S3ObjectDto> listAllResumes() {
        log.info("Listing all objects in S3 bucket: {}", awsProperties.getBucketName());
        List<S3Object> s3Objects = awsConnector.listObjects();

        return s3Objects.stream()
                .map(s3Object -> S3ObjectDto.builder()
                        .key(s3Object.key())
                        .size(s3Object.size())
                        .lastModified(s3Object.lastModified())
                        .eTag(s3Object.eTag())
                        .build())
                .collect(Collectors.toList());
    }


    public String uploadResume(String fileName, InputStream fileStream) {
        try {
            String s3Key = awsConnector.uploadFile(fileName, fileStream, "application/pdf");
            log.info("Resume uploaded successfully. S3 Key: {}", s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload resume: {}", e.getMessage(), e);
            throw new RuntimeException("Resume upload failed: " + e.getMessage(), e);
        }
    }

    public String getPresignedUploadUrl(String fileName) {
        try {
            String presignedUrl = awsConnector.generatePresignedUploadUrl(fileName, "application/pdf");
            log.info("Presigned upload URL generated for resume: {}", fileName);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL: {}", e.getMessage(), e);
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    public String getPresignedDownloadUrl(String fileName) {
        try {
            String presignedUrl = awsConnector.generatePresignedDownloadUrl(fileName);
            log.info("Presigned download URL generated for resume: {}", fileName);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL: {}", e.getMessage(), e);
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }

    public boolean resumeExists(String fileName) {
        return awsConnector.fileExists(fileName);
    }

    public boolean deleteResume(String fileName) {
        return awsConnector.deleteFile(fileName);
    }
}

