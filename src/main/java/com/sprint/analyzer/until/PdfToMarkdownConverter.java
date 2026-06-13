package com.sprint.analyzer.until;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PdfToMarkdownConverter implements MarkdownConverter {


    private String convertPlainTextToMarkdown(String text) {
        StringBuilder md = new StringBuilder();
        String[] lines = text.split("\\R");

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                md.append("\n");
            } else if (line.equalsIgnoreCase("Professional Summary")
                    || line.equalsIgnoreCase("Technical Skills")
                    || line.equalsIgnoreCase("Professional Experience")
                    || line.equalsIgnoreCase("Education")
                    || line.equalsIgnoreCase("Projects")) {
                md.append("\n## ").append(line).append("\n\n");
            } else if (line.startsWith("•") || line.startsWith("-")) {
                md.append("- ").append(line.replaceFirst("^[•-]\\s*", "")).append("\n");
            } else {
                md.append(line).append("\n");
            }
        }

        return md.toString();
    }

    @Override
    public String convertToMarkdown(MultipartFile file) {
        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return convertPlainTextToMarkdown(text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert PDF to Markdown", e);
        }
    }
}
