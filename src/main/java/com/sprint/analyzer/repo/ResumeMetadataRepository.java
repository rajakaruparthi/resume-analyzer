package com.sprint.analyzer.repo;

import com.sprint.analyzer.entity.ResumeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeMetadataRepository extends JpaRepository<ResumeMetadata, UUID> {

    List<ResumeMetadata> findByUserId(UUID userId);

    Optional<ResumeMetadata> findByS3Key(String s3Key);

    Optional<ResumeMetadata> findByOriginalFilenameAndUserId(String originalFilename, UUID userId);

    List<ResumeMetadata> findByUserIdAndStatus(UUID userId, String status);

    List<ResumeMetadata> findByStatus(String status);

    long countByUserId(UUID userId);

    boolean existsByOriginalFilenameAndUserId(String originalFilename, UUID userId);

    List<ResumeMetadata> findByTokenCountGreaterThanEqual(Integer minTokens);

    Optional<ResumeMetadata> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

}

