package com.sprint.analyzer.controller;

import com.sprint.analyzer.entity.User;
import com.sprint.analyzer.model.ExtractedText;
import com.sprint.analyzer.model.S3ObjectDto;
import com.sprint.analyzer.model.ResumeDetail;
import com.sprint.analyzer.entity.ResumeMetadata;
import com.sprint.analyzer.service.*;

import java.util.*;

import com.sprint.analyzer.until.GlobalConstants;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/api/resumes")
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "aws.s3", name = "bucket-name")
public class ResumeUploadController {

    private final ResumeS3Service resumeS3Service;
    private final ResumeTextExtractionService resumeTextExtractionService;
    private final ResumeLlmService resumeLlmService;
    private final ResumeMetadataService resumeMetadataService;
    private final UserService userService;
    private final ResumeProcessingService resumeProcessingService;


    private User getLoggedInUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated.");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userService.getUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database."));
    }

    @GetMapping
    @Operation(summary = "List all resume files in the S3 bucket")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<S3ObjectDto>> listAllResumes(Authentication authentication) {
        try {
            log.info("Listing all resume files.");
            User userInfo = getLoggedInUserId(authentication);
            if (userInfo == null) {
                log.warn("User not found for ID: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
            }
            return ResponseEntity.ok(resumeS3Service.listAllResumes(userInfo.getBucketName()));
        } catch (RuntimeException e) {
            log.error("Error listing resume files: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/{s3Key}/extract")
    public ResponseEntity<ExtractedText> extractText(@PathVariable String s3Key, Authentication authentication) {
        User userInfo = getLoggedInUserId(authentication);
        String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());
        return ResponseEntity.ok(resumeTextExtractionService.extractFromS3(bucketName, s3Key));
    }

    @GetMapping("/{s3Key}/details")
    @Operation(summary = "Get structured resume detail parsed by LLM")
    public ResponseEntity<ResumeDetail> getResumeDetail(@PathVariable String s3Key, Authentication authentication) {
        log.info("Request to get details for resume: {}", s3Key);

        User userInfo = getLoggedInUserId(authentication);
        String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());

        // 1. Extract text from S3
        ExtractedText extracted = resumeTextExtractionService.extractFromS3(bucketName, s3Key);

        // 2. Parse using LLM service
        ResumeDetail detail = resumeLlmService.parseResumeText(extracted.getText());

        // 3. Populate metadata from database
        Optional<ResumeMetadata> metaOpt = resumeMetadataService.getResumeMetadataByS3Key(s3Key);
        if (metaOpt.isPresent()) {
            ResumeMetadata meta = metaOpt.get();
            detail.setId(meta.getId().toString());
            detail.setOriginalFilename(meta.getOriginalFilename());
            detail.setS3Key(meta.getS3Key());
            detail.setStatus(meta.getStatus());
            detail.setUploadedAt(meta.getCreatedAt().toString());
        } else {
            detail.setId(s3Key);
            detail.setOriginalFilename(s3Key);
            detail.setS3Key(s3Key);
            detail.setStatus("COMPLETED");
            detail.setUploadedAt(java.time.LocalDateTime.now().toString());
        }

        return ResponseEntity.ok(detail);
    }

    @PostMapping("/extract")
    public ResponseEntity<ExtractedText> uploadAndExtract(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(
                resumeTextExtractionService.extractFromStream(file.getOriginalFilename(), file.getInputStream())
        );
    }


    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getPresignedUploadUrl(@RequestParam String fileName, Authentication authentication) {
        try {
            log.info("Generating presigned upload URL for file: {}", fileName);
            User userInfo = getLoggedInUserId(authentication);
            String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());
            resumeS3Service.createBucketIfNotExists(bucketName);
            String presignedUrl = resumeS3Service.getPresignedUploadUrl(bucketName, fileName);

            Map<String, String> response = new HashMap<>();
            response.put("presignedUrl", presignedUrl);
            response.put("fileName", fileName);
            response.put("method", "PUT");
            response.put("expiresIn", "15 minutes");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error generating presigned upload URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to generate presigned URL: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }
            if (Arrays.stream(GlobalConstants.allowedFileTypes).noneMatch(type -> type.equalsIgnoreCase(file.getContentType()))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Unsupported file type: " + file.getContentType()));
            }
            User userInfo = getLoggedInUserId(authentication);
            Map<String, Object> response = resumeProcessingService.processAndSaveResume(file, userInfo);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            log.error("Error uploading resume: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }


    @GetMapping("/{fileName}/download-url")
    public ResponseEntity<Map<String, String>> getPresignedDownloadUrl(@PathVariable String fileName, Authentication authentication) {
        try {
            log.info("Generating presigned download URL for file: {}", fileName);
            User userInfo = getLoggedInUserId(authentication);
            String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());
            String presignedUrl = resumeS3Service.getPresignedDownloadUrl(bucketName, fileName);

            Map<String, String> response = new HashMap<>();
            response.put("presignedUrl", presignedUrl);
            response.put("fileName", fileName);
            response.put("method", "GET");
            response.put("expiresIn", "15 minutes");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error generating presigned download URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to generate presigned URL: " + e.getMessage()));
        }
    }


    @GetMapping("/{fileName}/exists")
    public ResponseEntity<Map<String, Object>> checkResumeExists(@PathVariable String fileName, Authentication authentication) {
        try {
            log.info("Checking if resume exists: {}", fileName);
            User userInfo = getLoggedInUserId(authentication);
            String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());
            boolean exists = resumeS3Service.resumeExists(bucketName, fileName);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("exists", exists);
            response.put("status", exists ? "File found" : "File not found");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error checking file existence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to check file existence: " + e.getMessage()));
        }
    }


    @DeleteMapping("/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteResume(@PathVariable String fileName, Authentication authentication) {
        try {
            log.info("Deleting resume file: {}", fileName);
            User userInfo = getLoggedInUserId(authentication);
            String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());
            boolean deleted = resumeS3Service.deleteResume(bucketName, fileName);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("deleted", deleted);
            response.put("message", deleted ? "Resume deleted successfully" : "Failed to delete resume");

            return deleted ? ResponseEntity.ok(response)
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (RuntimeException e) {
            log.error("Error deleting resume: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete resume: " + e.getMessage()));
        }
    }


    @GetMapping("/{fileName}/download")
    public ResponseEntity<Map<String, String>> getDownloadLink(@PathVariable String fileName, Authentication authentication) {
        try {
            log.info("Generating download link for resume: {}", fileName);
            User userInfo = getLoggedInUserId(authentication);
            String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());
            String presignedUrl = resumeS3Service.getPresignedDownloadUrl(bucketName, fileName);

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", presignedUrl);
            response.put("fileName", fileName);
            response.put("instruction", "Click the URL to download or use in redirect response");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error generating download link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found or cannot be downloaded: " + e.getMessage()));
        }
    }

}
