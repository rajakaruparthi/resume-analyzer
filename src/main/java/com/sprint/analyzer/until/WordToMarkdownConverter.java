package com.sprint.analyzer.until;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class WordToMarkdownConverter implements MarkdownConverter {

    public String convertToMarkdown(MultipartFile multipartFile) throws IOException {
        try (XWPFDocument document =
                     new XWPFDocument(multipartFile.getInputStream());
             XWPFWordExtractor extractor =
                     new XWPFWordExtractor(document)) {

            return extractor.getText();
        }
    }
}