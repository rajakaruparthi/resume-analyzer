package com.sprint.analyzer.service;

import com.sprint.analyzer.connector.AwsConnector;
import com.sprint.analyzer.connector.TextExtractor;
import com.sprint.analyzer.until.MarkdownConverter;
import com.sprint.analyzer.until.PdfToMarkdownConverter;

import com.sprint.analyzer.until.WordToMarkdownConverter;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; // Keep conditional if needed
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
// Make this service conditional on S3 being enabled, as it depends on AwsConnector
@ConditionalOnProperty(prefix = "aws.s3", name = "bucket-name")
public class ResumeMarkdownConverterService {

    private final AwsConnector awsConnector;
    private final TextExtractor textExtractor;

    // Constructor for HtmlConverter (flexmark-java)
    public ResumeMarkdownConverterService() {
        this.awsConnector = null; // Will be injected by Lombok
        this.textExtractor = null; // Will be injected by Lombok
    }

    /**
     * Converts a resume document stored in S3 to Markdown format.
     *
     * @return The content of the resume in Markdown format.
     * @throws RuntimeException if conversion fails.
     */

    public String convertToText(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            MarkdownConverter converter = null;
            if (fileName == null) {
                throw new RuntimeException("File name is missing");
            }

            String lowerCaseFileName = fileName.toLowerCase();
            if (lowerCaseFileName.endsWith(".pdf")) {
                log.info("Converting PDF resume to Markdown: {}", fileName);
                converter = new PdfToMarkdownConverter();
                return converter.convertToMarkdown(file);
            } else if (lowerCaseFileName.endsWith(".docx") || lowerCaseFileName.endsWith(".doc")) {
                converter = new WordToMarkdownConverter();
                log.info("Extracting text from Word document: {}", fileName);
                return converter.convertToMarkdown(file);
            } else {
                throw new RuntimeException("Unsupported file type: " + fileName);
            }
        } catch (Exception e) {
            log.error("Failed to convert resume to Markdown: {}", e.getMessage(), e);
            throw new RuntimeException("Resume conversion failed: " + e.getMessage(), e);
        }
    }
    
}