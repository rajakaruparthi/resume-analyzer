package com.sprint.analyzer.config;

import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TikaConfig {

    public Parser tikaParser() {
        return new AutoDetectParser();
    }
}
