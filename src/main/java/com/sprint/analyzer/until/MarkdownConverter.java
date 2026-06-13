package com.sprint.analyzer.until;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface MarkdownConverter {

    String convertToMarkdown(MultipartFile text) throws IOException;
}
