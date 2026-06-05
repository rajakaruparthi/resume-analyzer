package com.sprint.analyzer.connector;

import com.sprint.analyzer.properties.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;


@Component
@Slf4j
public class AwsConnector {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private final S3Presigner s3Presigner;

    public AwsConnector(S3Client s3Client, AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.awsProperties = awsProperties;
        this.s3Presigner = S3Presigner.builder()
                .region(software.amazon.awssdk.regions.Region.of(
                        awsProperties.getRegion() != null ? awsProperties.getRegion() : "us-east-1"
                ))
                .build();
    }


    public String uploadFile(String fileName, InputStream inputStream, String contentType) throws IOException {
        String bucketName = awsProperties.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("AWS S3 bucket name not configured");
        }

        try {
            byte[] fileContent = inputStream.readAllBytes();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));

            log.info("File uploaded successfully to S3. Bucket: {}, Key: {}", bucketName, fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to upload file to S3. Bucket: {}, Key: {}, Error: {}",
                    bucketName, fileName, e.getMessage(), e);
            throw new IOException("S3 upload failed: " + e.getMessage(), e);
        }
    }


    public String generatePresignedUploadUrl(String fileName, String contentType) {
        String bucketName = awsProperties.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("AWS S3 bucket name not configured");
        }

        try {
            long expirationMinutes = awsProperties.getPresignedUrlExpirationMinutes() != null
                    ? awsProperties.getPresignedUrlExpirationMinutes()
                    : 15L;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build();

            String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

            log.info("Presigned upload URL generated. Bucket: {}, Key: {}, Expiration: {} minutes",
                    bucketName, fileName, expirationMinutes);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL. Bucket: {}, Key: {}, Error: {}",
                    bucketName, fileName, e.getMessage(), e);
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }


    public String generatePresignedDownloadUrl(String fileName) {
        String bucketName = awsProperties.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("AWS S3 bucket name not configured");
        }

        try {
            long expirationMinutes = awsProperties.getPresignedUrlExpirationMinutes() != null
                    ? awsProperties.getPresignedUrlExpirationMinutes()
                    : 15L;

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();

            log.info("Presigned download URL generated. Bucket: {}, Key: {}, Expiration: {} minutes",
                    bucketName, fileName, expirationMinutes);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL. Bucket: {}, Key: {}, Error: {}",
                    bucketName, fileName, e.getMessage(), e);
            throw new RuntimeException("Presigned URL generation failed: " + e.getMessage(), e);
        }
    }


    public boolean fileExists(String fileName) {
        String bucketName = awsProperties.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("AWS S3 bucket name not configured");
        }

        try {
            s3Client.headObject(builder -> builder.bucket(bucketName).key(fileName));
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            log.debug("File not found in S3. Bucket: {}, Key: {}", bucketName, fileName);
            return false;
        } catch (Exception e) {
            log.error("Error checking if file exists in S3. Bucket: {}, Key: {}, Error: {}",
                    bucketName, fileName, e.getMessage(), e);
            return false;
        }
    }


    public boolean deleteFile(String fileName) {
        String bucketName = awsProperties.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("AWS S3 bucket name not configured");
        }

        try {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(fileName));
            log.info("File deleted from S3. Bucket: {}, Key: {}", bucketName, fileName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3. Bucket: {}, Key: {}, Error: {}",
                    bucketName, fileName, e.getMessage(), e);
            return false;
        }
    }


    public InputStream downloadFileStream(String fileName) {
        String bucketName = awsProperties.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("AWS S3 bucket name not configured");
        }
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            return s3Client.getObject(request); // ResponseInputStream<GetObjectResponse>
        } catch (Exception e) {
            log.error("Failed to download from S3. Bucket: {}, Key: {}", bucketName, fileName, e);
            throw new RuntimeException("S3 download failed: " + e.getMessage(), e);
        }
    }


}
