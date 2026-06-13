package com.sprint.analyzer.service;

import com.sprint.analyzer.connector.AwsConnector;
import com.sprint.analyzer.connector.TextExtractor;
import com.sprint.analyzer.model.ExtractedText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aws.s3", name = "bucket-name")
public class ResumeTextExtractionService {

    private final AwsConnector awsConnector;
    private final TextExtractor textExtractor;

    public ExtractedText extractFromS3(String bucketName, String s3Key) {
        log.info("Starting text extraction for S3 key: {} in bucket: {}", s3Key, bucketName);
        try (InputStream in = awsConnector.downloadFileStream(bucketName, s3Key)) {
            return textExtractor.extract(in, s3Key);
        } catch (Exception e) {
            log.error("Extraction pipeline failed for '{}' in bucket '{}': {}", s3Key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Resume extraction failed: " + e.getMessage(), e);
        }
    }

    public ExtractedText extractFromStream(String fileName, InputStream stream) {
        return textExtractor.extract(stream, fileName);
    }
}