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

    private ResumeData resumeData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private String category;
        private int score;
        private String comments;
    }
}
