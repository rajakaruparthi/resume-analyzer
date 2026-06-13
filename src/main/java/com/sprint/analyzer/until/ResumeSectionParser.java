package com.sprint.analyzer.until;

import com.sprint.analyzer.model.ResumeSection;

import java.util.*;
import java.util.regex.*;

public class ResumeSectionParser {

    private static final Pattern SECTION_PATTERN =
            Pattern.compile("(?im)^(professional summary|summary|technical skills|skills|professional experience|work experience|projects|education|certifications)\\s*$");

    public List<ResumeSection> parseSections(String cleanText) {
        List<ResumeSection> sections = new ArrayList<>();

        Matcher matcher = SECTION_PATTERN.matcher(cleanText);

        List<MatchResult> matches = matcher.results().toList();

        if (matches.isEmpty()) {
            sections.add(new ResumeSection(
                    "GENERAL",
                    cleanText,
                    HashUtil.sha256(cleanText)
            ));
            return sections;
        }

        for (int i = 0; i < matches.size(); i++) {
            MatchResult current = matches.get(i);

            String sectionName = current.group(1).toUpperCase();

            int start = current.end();
            int end = (i + 1 < matches.size())
                    ? matches.get(i + 1).start()
                    : cleanText.length();

            String content = cleanText.substring(start, end).trim();

            if (!content.isBlank()) {
                sections.add(new ResumeSection(
                        sectionName,
                        content,
                        HashUtil.sha256(content)
                ));
            }
        }

        return sections;
    }
}