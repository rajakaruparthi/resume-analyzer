package com.sprint.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectDto {
    private String key;
    private Long size;
    private Instant lastModified;
    private String eTag;
}