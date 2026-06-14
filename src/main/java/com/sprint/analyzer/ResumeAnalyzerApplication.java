package com.sprint.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ResumeAnalyzerApplication {

    public static void main(String[] args) {
        System.out.println("Starting Resume Analyzer Application...");
        SpringApplication.run(ResumeAnalyzerApplication.class, args);
        System.out.println("Started Resume Analyzer Application...");

    }

}
