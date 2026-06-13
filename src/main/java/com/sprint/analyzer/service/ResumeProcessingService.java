package com.sprint.analyzer.service;

import com.sprint.analyzer.entity.ResumeMetadata;
import com.sprint.analyzer.entity.ResumeSectionScore;
import com.sprint.analyzer.entity.User;
import com.sprint.analyzer.model.ResumeDetail;
import com.sprint.analyzer.model.ResumeSection;
import com.sprint.analyzer.repo.ResumeSectionScoreRepository;
import com.sprint.analyzer.until.HashUtil;
import com.sprint.analyzer.until.ResumeSectionParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class ResumeProcessingService {

    private final ResumeS3Service resumeS3Service;
    private final ResumeMarkdownConverterService resumeMarkdownConverterService;
    private final ResumeLlmService resumeLlmService;
    private final ResumeMetadataService resumeMetadataService;
    private final UserService userService;
    private final ResumeSectionScoreRepository resumeSectionScoreRepository;

    public Map<String, Object> processAndSaveResume(MultipartFile file, User userInfo) throws IOException {
        String bucketName = resumeS3Service.getBucketNameForUser(userInfo.getId());
        resumeS3Service.createBucketIfNotExists(bucketName);

        String fileName = file.getOriginalFilename();
        log.info("Uploading resume file: {} for user: {} to bucket: {}", fileName, userInfo.getId(), bucketName);

        String s3Key = resumeS3Service.uploadResume(bucketName, fileName, file.getInputStream());
        userService.updateBucketName(bucketName, userInfo);

        String text = resumeMarkdownConverterService.convertToText(file);

        // 1. Save ResumeMetadata
        ResumeMetadata metadata = new ResumeMetadata();
        metadata.setUserId(userInfo.getId());
        metadata.setOriginalFilename(fileName);
        metadata.setS3Key(s3Key);

        String fileType = "";
        if (fileName != null && fileName.contains(".")) {
            fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
            if (fileType.length() > 20) {
                fileType = fileType.substring(0, 20);
            }
        } else if (file.getContentType() != null) {
            fileType = file.getContentType();
            if (fileType.length() > 20) {
                fileType = fileType.substring(0, 20);
            }
        }
        metadata.setFileType(fileType);
        metadata.setTokenCount(text.split("\\s+").length);
        metadata.setStatus("COMPLETED");


        // 1. Parse sections using ResumeSectionParser and check database for matches
        ResumeSectionParser sectionParser = new ResumeSectionParser();
        List<ResumeSection> sections = sectionParser.parseSections(text);

        Map<String, ResumeSectionScore> cachedSectionScores = new HashMap<>();
        for (ResumeSection section : sections) {
            java.util.Optional<ResumeSectionScore> existingScoreOpt =
                    resumeSectionScoreRepository.findFirstByUserIdAndSectionHashOrderByCreatedAtDesc(userInfo.getId(), section.hash());
            if (existingScoreOpt.isPresent()) {
                cachedSectionScores.put(section.sectionName(), existingScoreOpt.get());
                log.info("Found cached score/feedback for section: {}, hash: {}, score: {}",
                        section.sectionName(), section.hash(), existingScoreOpt.get().getScore());
            }
        }

        // 2. Call LLM with the cached section info passed in the prompt
        ResumeDetail resumeDetail = resumeLlmService.parseResumeText(text, cachedSectionScores);
        metadata.setOverallScore(resumeDetail.getOverallScore());

        // Save resume hash
        String resumeHash = HashUtil.sha256(text);
        metadata.setResumeHash(resumeHash);

        ResumeMetadata savedMetadata = resumeMetadataService.createResumeMetadata(metadata);

        // 3. Save section scores (using cached or LLM returned values)
        int overallScore = resumeDetail.getOverallScore() != null ? resumeDetail.getOverallScore() : 0;
        String detailedFeedback = resumeDetail.getDetailedFeedback() != null ? resumeDetail.getDetailedFeedback() : "";

        for (ResumeSection section : sections) {
            int score;
            String feedback;

            if (cachedSectionScores.containsKey(section.sectionName())) {
                ResumeSectionScore cached = cachedSectionScores.get(section.sectionName());
                score = cached.getScore();
                feedback = cached.getFeedback();
                log.info("Reusing cached score and feedback for section: {}", section.sectionName());
            } else {
                score = getMappedScore(section.sectionName(), resumeDetail.getScoreBreakdown(), overallScore);
                feedback = getMappedFeedback(section.sectionName(), resumeDetail.getScoreBreakdown(), detailedFeedback);
            }

            ResumeSectionScore sectionScore = ResumeSectionScore.builder()
                    .resumeId(savedMetadata.getId())
                    .userId(userInfo.getId())
                    .sectionName(section.sectionName())
                    .sectionHash(section.hash())
                    .score(score)
                    .feedback(feedback)
                    .build();
            resumeSectionScoreRepository.save(sectionScore);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Resume uploaded successfully");
        response.put("fileName", fileName);
        response.put("s3Key", s3Key);
        response.put("fileSize", file.getSize());
        response.put("contentType", file.getContentType());
        response.put("id", savedMetadata.getId().toString());

        return response;
    }

    private int getMappedScore(String sectionName, List<ResumeDetail.ScoreBreakdown> breakdowns, int defaultScore) {
        if (breakdowns == null) return defaultScore;
        String search = sectionName.toUpperCase();
        for (var b : breakdowns) {
            String cat = b.getCategory().toUpperCase();
            if (search.equals("EXPERIENCE") && (cat.contains("EXPERIENCE") || cat.contains("WORK")))
                return b.getScore();
            if (search.equals("SKILLS") && cat.contains("SKILLS")) return b.getScore();
            if (search.equals("EDUCATION") && cat.contains("EDUCATION")) return b.getScore();
            if (search.equals("SUMMARY") && (cat.contains("CONTENT") || cat.contains("SUMMARY"))) return b.getScore();
            if (search.equals("PROJECTS") && (cat.contains("IMPACT") || cat.contains("PROJECTS"))) return b.getScore();
            if (search.equals("CERTIFICATIONS") && (cat.contains("EDUCATION") || cat.contains("CERTIFICATION")))
                return b.getScore();
            if (search.equals("GENERAL") && cat.contains("FORMATTING")) return b.getScore();
        }
        return defaultScore;
    }

    private String getMappedFeedback(String sectionName, List<ResumeDetail.ScoreBreakdown> breakdowns, String defaultFeedback) {
        if (breakdowns == null) return defaultFeedback;
        String search = sectionName.toUpperCase();
        for (var b : breakdowns) {
            String cat = b.getCategory().toUpperCase();
            if (search.equals("EXPERIENCE") && (cat.contains("EXPERIENCE") || cat.contains("WORK")))
                return b.getComments();
            if (search.equals("SKILLS") && cat.contains("SKILLS")) return b.getComments();
            if (search.equals("EDUCATION") && cat.contains("EDUCATION")) return b.getComments();
            if (search.equals("SUMMARY") && (cat.contains("CONTENT") || cat.contains("SUMMARY")))
                return b.getComments();
            if (search.equals("PROJECTS") && (cat.contains("IMPACT") || cat.contains("PROJECTS")))
                return b.getComments();
            if (search.equals("CERTIFICATIONS") && (cat.contains("EDUCATION") || cat.contains("CERTIFICATION")))
                return b.getComments();
            if (search.equals("GENERAL") && cat.contains("FORMATTING")) return b.getComments();
        }
        return defaultFeedback;
    }
}
