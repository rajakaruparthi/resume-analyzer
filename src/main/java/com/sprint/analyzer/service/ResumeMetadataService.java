package com.sprint.analyzer.service;

import com.sprint.analyzer.entity.ResumeMetadata;
import com.sprint.analyzer.repo.ResumeMetadataRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class ResumeMetadataService {

    private final ResumeMetadataRepository resumeMetadataRepository;

    public ResumeMetadata createResumeMetadata(ResumeMetadata resumeMetadata) {
        log.info("Creating resume metadata for user: {}, filename: {}",
                resumeMetadata.getUserId(), resumeMetadata.getOriginalFilename());
        return resumeMetadataRepository.save(resumeMetadata);
    }

    public Optional<ResumeMetadata> getResumeMetadataById(UUID id) {
        log.info("Retrieving resume metadata by ID: {}", id);
        return resumeMetadataRepository.findById(id);
    }

    public List<ResumeMetadata> getResumeMetadataByUserId(UUID userId) {
        log.info("Retrieving all resumes for user: {}", userId);
        return resumeMetadataRepository.findByUserId(userId);
    }

    public Optional<ResumeMetadata> getResumeMetadataByS3Key(String s3Key) {
        log.info("Retrieving resume metadata by S3 key: {}", s3Key);
        return resumeMetadataRepository.findByS3Key(s3Key);
    }


    public long getUserResumeCount(UUID userId) {
        log.info("Counting resumes for user: {}", userId);
        return resumeMetadataRepository.countByUserId(userId);
    }

    public List<ResumeMetadata> getResumesByUserIdAndStatus(UUID userId, String status) {
        log.info("Retrieving resumes for user: {} with status: {}", userId, status);
        return resumeMetadataRepository.findByUserIdAndStatus(userId, status);
    }


    public List<ResumeMetadata> getResumesByStatus(String status) {
        log.info("Retrieving all resumes with status: {}", status);
        return resumeMetadataRepository.findByStatus(status);
    }


    public Optional<ResumeMetadata> getLatestResumeForUser(UUID userId) {
        log.info("Retrieving latest resume for user: {}", userId);
        return resumeMetadataRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    public ResumeMetadata updateResumeMetadata(UUID id, ResumeMetadata updatedMetadata) {
        ResumeMetadata metadata = resumeMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume metadata not found with ID: " + id));

        if (updatedMetadata.getStatus() != null) {
            metadata.setStatus(updatedMetadata.getStatus());
        }
        if (updatedMetadata.getTokenCount() != null) {
            metadata.setTokenCount(updatedMetadata.getTokenCount());
        }
        if (updatedMetadata.getS3Key() != null) {
            metadata.setS3Key(updatedMetadata.getS3Key());
        }

        log.info("Updating resume metadata with ID: {}", id);
        return resumeMetadataRepository.save(metadata);
    }


    public ResumeMetadata updateResumeStatus(UUID id, String status) {
        ResumeMetadata metadata = resumeMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume metadata not found with ID: " + id));

        metadata.setStatus(status);
        log.info("Updating resume status for ID: {} to: {}", id, status);
        return resumeMetadataRepository.save(metadata);
    }


    public ResumeMetadata updateResumeTokenCount(UUID id, Integer tokenCount) {
        ResumeMetadata metadata = resumeMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume metadata not found with ID: " + id));

        metadata.setTokenCount(tokenCount);
        log.info("Updating resume token count for ID: {} to: {}", id, tokenCount);
        return resumeMetadataRepository.save(metadata);
    }

    public boolean deleteResumeMetadata(UUID id) {
        if (resumeMetadataRepository.existsById(id)) {
            log.info("Deleting resume metadata with ID: {}", id);
            resumeMetadataRepository.deleteById(id);
            return true;
        }
        log.warn("Resume metadata not found for deletion with ID: {}", id);
        return false;
    }

    public long deleteResumesByUserId(UUID userId) {
        List<ResumeMetadata> resumes = resumeMetadataRepository.findByUserId(userId);
        log.info("Deleting {} resumes for user: {}", resumes.size(), userId);
        resumeMetadataRepository.deleteAll(resumes);
        return resumes.size();
    }

    public boolean resumeExists(String originalFilename, UUID userId) {
        return resumeMetadataRepository.existsByOriginalFilenameAndUserId(originalFilename, userId);
    }


    public List<ResumeMetadata> getResumesByMinTokenCount(Integer minTokens) {
        log.info("Retrieving resumes with token count >= {}", minTokens);
        return resumeMetadataRepository.findByTokenCountGreaterThanEqual(minTokens);
    }

    public long getTotalResumeCount() {
        return resumeMetadataRepository.count();
    }

}

