package com.sprint.analyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "resume_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeMetadata {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_filename", nullable = false, columnDefinition = "TEXT")
    private String originalFilename;

    @Column(name = "s3_key", columnDefinition = "TEXT")
    private String s3Key;

    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

}


