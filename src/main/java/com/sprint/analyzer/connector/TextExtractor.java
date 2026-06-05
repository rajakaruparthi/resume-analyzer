package com.sprint.analyzer.connector;

import com.sprint.analyzer.model.ExtractedText;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TextExtractor {

    private final Parser parser;

    // -1 = unlimited; set to a sane cap (e.g. 10 MB of chars) for resumes
    @Value("${tika.max-chars:-1}")
    private int maxChars;

    public TextExtractor(Parser parser) {
        this.parser = parser;
    }

    public ExtractedText extract(InputStream inputStream, String fileName) {
        BodyContentHandler handler = new BodyContentHandler(maxChars);
        Metadata metadata = new Metadata();
        if (fileName != null) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        }
        ParseContext context = new ParseContext();

        try {
            parser.parse(inputStream, handler, metadata, context);

            String text = handler.toString();
            Map<String, String> meta = new HashMap<>();
            for (String name : metadata.names()) {
                meta.put(name, metadata.get(name));
            }

            log.info("Extracted {} chars from '{}' (contentType={})",
                    text.length(), fileName, metadata.get(Metadata.CONTENT_TYPE));

            return ExtractedText.builder()
                    .fileName(fileName)
                    .contentType(metadata.get(Metadata.CONTENT_TYPE))
                    .text(text)
                    .charCount(text.length())
                    .metadata(meta)
                    .build();

        } catch (IOException | SAXException | TikaException e) {
            log.error("Tika extraction failed for '{}': {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Text extraction failed: " + e.getMessage(), e);
        }
    }
}