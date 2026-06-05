package com.sprint.analyzer.controller;

import com.sprint.analyzer.model.ExtractedText;
import com.sprint.analyzer.service.ResumeS3Service;
import com.sprint.analyzer.service.ResumeTextExtractionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/resumes")
@AllArgsConstructor
@Slf4j
public class ResumeUploadController {

    private final ResumeS3Service resumeS3Service;

    private final ResumeTextExtractionService resumeTextExtractionService;

    // Extract text from an already-uploaded resume
    @GetMapping("/{s3Key}/extract")
    public ResponseEntity<ExtractedText> extractText(@PathVariable String s3Key) {
        return ResponseEntity.ok(resumeTextExtractionService.extractFromS3(s3Key));
    }

    // Upload + extract in one call (handy for testing)
    @PostMapping("/extract")
    public ResponseEntity<ExtractedText> uploadAndExtract(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(
                resumeTextExtractionService.extractFromStream(file.getOriginalFilename(), file.getInputStream())
        );
    }


    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getPresignedUploadUrl(@RequestParam String fileName) {
        try {
            log.info("Generating presigned upload URL for file: {}", fileName);
            String presignedUrl = resumeS3Service.getPresignedUploadUrl(fileName);

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
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }

            String fileName = file.getOriginalFilename();
            log.info("Uploading resume file: {}", fileName);

            String s3Key = resumeS3Service.uploadResume(fileName, file.getInputStream());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Resume uploaded successfully");
            response.put("fileName", fileName);
            response.put("s3Key", s3Key);
            response.put("fileSize", file.getSize());
            response.put("contentType", file.getContentType());

            log.info("Resume uploaded successfully. S3 Key: {}", s3Key);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            log.error("Error uploading resume: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }


    @GetMapping("/{fileName}/download-url")
    public ResponseEntity<Map<String, String>> getPresignedDownloadUrl(@PathVariable String fileName) {
        try {
            log.info("Generating presigned download URL for file: {}", fileName);
            String presignedUrl = resumeS3Service.getPresignedDownloadUrl(fileName);

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
    public ResponseEntity<Map<String, Object>> checkResumeExists(@PathVariable String fileName) {
        try {
            log.info("Checking if resume exists: {}", fileName);
            boolean exists = resumeS3Service.resumeExists(fileName);

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
    public ResponseEntity<Map<String, Object>> deleteResume(@PathVariable String fileName) {
        try {
            log.info("Deleting resume file: {}", fileName);
            boolean deleted = resumeS3Service.deleteResume(fileName);

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
    public ResponseEntity<Map<String, String>> getDownloadLink(@PathVariable String fileName) {
        try {
            log.info("Generating download link for resume: {}", fileName);
            String presignedUrl = resumeS3Service.getPresignedDownloadUrl(fileName);

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
