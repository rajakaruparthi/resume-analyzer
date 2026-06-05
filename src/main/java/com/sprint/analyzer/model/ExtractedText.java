package com.sprint.analyzer.model;


import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ExtractedText {
    private String fileName;
    private String contentType;
    private String text;
    private int charCount;
    private Map<String, String> metadata;
}