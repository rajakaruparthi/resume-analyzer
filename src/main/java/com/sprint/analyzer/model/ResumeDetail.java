package com.sprint.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDetail {

    private String id;
    private String originalFilename;
    private String s3Key;
    private String status;
    private String uploadedAt;
    private Integer overallScore;

    private String candidateName;
    private String detailedFeedback;
    private List<String> strengths;
    private List<String> improvements;
    private List<ScoreBreakdown> scoreBreakdown;

    private Boolean isResume;
    private List<String> missingSections;
    private List<String> weakSections;

    private ResumeData resumeData;
    private List<SectionScoreDto> sectionScores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private String category;
        private int score;
        private List<String> strengths;
        private List<String> improvements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionScoreDto {
        private String sectionName;
        private String sectionHash;
        private Integer score;
        private String feedback;
        private List<String> strengths;
        private List<String> improvements;
    }
}
