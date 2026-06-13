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
public class ResumeData {
    private String name;
    private String title;
    private String email;
    private String phone;
    private String website;
    private String location;
    private String summary;
    private List<WorkExperience> experience;
    private List<Education> education;
    private List<String> skills;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperience {
        private String company;
        private String role;
        private String duration;
        private List<String> bullets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String school;
        private String degree;
        private String duration;
    }
}
