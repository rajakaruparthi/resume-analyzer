package com.sprint.analyzer.repo;

import com.sprint.analyzer.entity.ResumeSectionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeSectionScoreRepository extends JpaRepository<ResumeSectionScore, UUID> {

    List<ResumeSectionScore> findByResumeId(UUID resumeId);

    List<ResumeSectionScore> findByUserId(UUID userId);

    java.util.Optional<ResumeSectionScore> findFirstByUserIdAndSectionHashOrderByCreatedAtDesc(UUID userId, String sectionHash);

    void deleteByResumeId(UUID resumeId);
}
