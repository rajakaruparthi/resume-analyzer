package com.sprint.analyzer.service;

import com.sprint.analyzer.connector.AwsConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
@Slf4j
public class ResumeS3Service {

    private final AwsConnector awsConnector;

    public ResumeS3Service(AwsConnector awsConnector) {
        this.awsConnector = awsConnector;
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

