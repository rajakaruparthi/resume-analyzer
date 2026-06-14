package com.sprint.analyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_section_score")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSectionScore {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "section_name", nullable = false, length = 100)
    private String sectionName;

    @Column(name = "section_hash", length = 64)
    private String sectionHash;

    @Column(name = "score")
    private Integer score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Convert(converter = com.sprint.analyzer.until.StringListConverter.class)
    @Column(name = "strengths", columnDefinition = "TEXT")
    private java.util.List<String> strengths;

    @Convert(converter = com.sprint.analyzer.until.StringListConverter.class)
    @Column(name = "improvements", columnDefinition = "TEXT")
    private java.util.List<String> improvements;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", insertable = false, updatable = false)
    private ResumeMetadata resumeMetadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
