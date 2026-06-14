package com.sprint.analyzer.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.analyzer.entity.ResumeSectionScore;
import com.sprint.analyzer.model.ResumeDetail;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ResumeLlmService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResumeLlmService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String loadApiKey() {
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey;
        }

        // Try reading from /Users/rajakaruparthi/openai_cli/.env
        File file = new File("/Users/rajakaruparthi/openai_cli/.env");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("OPENAI_API_KEY=")) {
                        return line.substring("OPENAI_API_KEY=".length()).trim();
                    }
                }
            } catch (Exception e) {
                log.error("Failed to read OPENAI_API_KEY from .env file: {}", e.getMessage());
            }
        }
        return null;
    }

    public ResumeDetail parseResumeText(String resumeText) {
        return parseResumeText(resumeText, null);
    }

    public ResumeDetail parseResumeText(String resumeText, Map<String, com.sprint.analyzer.entity.ResumeSectionScore> cachedSectionScores) {
        String apiKey = loadApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("OpenAI API Key is not set or empty in environment or .env file.");
            throw new IllegalStateException("OpenAI API Key is not configured. Please check your environment or the local .env file.");
        }

        try {
            log.info("Sending resume text to OpenAI Chat Completions API...");
            String systemPrompt = """
                    You are an expert ATS resume reviewer, recruiter, hiring manager, and resume parser.
                    
                    Analyze the provided resume text and return ONLY valid JSON.
                    Do not return markdown, explanations, code blocks, or any text outside the JSON response.
                    
                    IMPORTANT RULES:
                    - Extract only information explicitly present in the resume.
                    - Do not hallucinate names, companies, dates, skills, education, projects, certifications, or achievements.
                    - If information is missing, return empty strings, empty arrays, or null values.
                    - Determine whether the document is a valid resume.
                    - If the document is not a resume, set isResume=false and overallScore=0.
                    - Return only valid parsable JSON.
                    
                    REQUIRED RESUME SECTIONS:
                    - PROFESSIONAL SUMMARY
                    - TECHNICAL SKILLS
                    - PROFESSIONAL EXPERIENCE
                    - PROJECTS
                    - EDUCATION
                    
                    SECTION ANALYSIS RULES:
                    - Detect missing sections and add them to missingSections.
                    - Detect weak or incomplete sections and add them to weakSections.
                    - Mention all missing sections in improvements.
                    - Projects exist only if dedicated projects or clearly described projects are found.
                    - Education exists only if degree, university, school, certification, or academic information is found.
                    - Do not assume sections exist.
                    
                    STRICT SCORING RULES:
                    - Be a strict and realistic resume reviewer.
                    - Do not inflate scores.
                    - Most resumes should score between 70 and 90.
                    - Scores above 90 should be uncommon.
                    - Scores above 95 should be extremely rare.
                    - A score of 100 should only be given when a section is exceptional and has absolutely no meaningful improvements.
                    - If any improvement exists for a section, that section score must be below 95.
                    - If measurable achievements or metrics are missing, the score must be below 90.
                    - Missing sections must significantly reduce scores.
                    - Weak sections must reduce scores appropriately.
                    - Deduct points for vague accomplishments.
                    - Deduct points for repetitive wording.
                    - Deduct points for weak project descriptions.
                    - Deduct points for lack of measurable impact.
                    - Deduct points for missing technologies.
                    - Deduct points for poor organization and readability.
                    
                    CATEGORY DIFFERENTIATION RULES:
                    - Score each category independently.
                    - Do not assign the same score to multiple categories unless their quality is truly identical.
                    - Avoid repeated scores such as 85, 86, 90 across several categories.
                    - If three or more categories receive the same score, re-evaluate and adjust scores.
                    - Categories should normally vary by 2-5 points when quality differs.
                    - Strong sections should score higher than average sections.
                    - Average sections should score higher than weak sections.
                    - Missing sections should receive substantially lower scores.
                    - Every score must be justified by the strengths and improvements listed for that category.
                    
                    CATEGORY FEEDBACK RULES:
                    - Every category must contain at least 2 strengths
                    - Every category must contain at least 2 improvements
                    - Do not repeat strengths across categories.
                    - Do not repeat improvements across categories.
                    - Evaluate only the assigned category.
                    - Provide actionable improvements.
                    - Do not use generic feedback.
                    - Do not use '-' as a strength or improvement.
                    - Empty strengths and improvements are not allowed for populated sections.
                    - If a section is missing, strengths should be empty and improvements should explain what should be added.
                    - If strengths and improvements are empty, score must be 60 or lower.
                    
                    SCORE INTERPRETATION:
                    - 95-100 = Exceptional.
                    - 90-94 = Excellent.
                    - 80-89 = Strong.
                    - 70-79 = Good.
                    - 60-69 = Average.
                    - 40-59 = Weak.
                    - 0-39 = Missing or poor.
                    
                    MISSING SECTION PENALTIES:
                    - Missing PROFESSIONAL SUMMARY => score <= 50.
                    - Missing TECHNICAL SKILLS => score <= 40.
                    - Missing PROFESSIONAL EXPERIENCE => score <= 30.
                    - Missing PROJECTS => score <= 50.
                    - Missing EDUCATION => score <= 50.
                    
                    CATEGORY DEFINITIONS:
                    
                    PROFESSIONAL SUMMARY:
                    Evaluate:
                    - Career positioning
                    - Clarity
                    - Value proposition
                    - Professional branding
                    - Relevance to target roles
                    
                    TECHNICAL SKILLS:
                    Evaluate:
                    - Technology breadth
                    - Technology depth
                    - Modern technology relevance
                    - Skill organization
                    - Industry relevance
                    
                    PROFESSIONAL EXPERIENCE:
                    Evaluate:
                    - Career progression
                    - Leadership
                    - Responsibilities
                    - Business impact
                    - Technical complexity
                    - Measurable achievements
                    
                    PROJECTS:
                    Evaluate:
                    - Technical complexity
                    - Architecture
                    - Technologies used
                    - Innovation
                    - Business value
                    - Measurable outcomes
                    
                    EDUCATION:
                    Evaluate:
                    - Degrees
                    - Certifications
                    - Academic relevance
                    - Educational achievements
                    
                    overallScore MUST equal the average of all category scores.
                    
                    Return JSON using this exact structure:
                    
                    {
                      "candidateName": "",
                      "isResume": true,
                      "missingSections": [],
                      "weakSections": [],
                      "detailedFeedback": "",
                      "strengths": [],
                      "improvements": [],
                      "scoreBreakdown": [
                        {
                          "category": "PROFESSIONAL SUMMARY",
                          "score": 82,
                          "strengths": [],
                          "improvements": []
                        },
                        {
                          "category": "TECHNICAL SKILLS",
                          "score": 88,
                          "strengths": [],
                          "improvements": []
                        },
                        {
                          "category": "PROFESSIONAL EXPERIENCE",
                          "score": 84,
                          "strengths": [],
                          "improvements": []
                        },
                        {
                          "category": "PROJECTS",
                          "score": 79,
                          "strengths": [],
                          "improvements": []
                        },
                        {
                          "category": "EDUCATION",
                          "score": 86,
                          "strengths": [],
                          "improvements": []
                        }
                      ],
                      "overallScore": 83.8,
                      "resumeData": {
                        "name": "",
                        "title": "",
                        "email": "",
                        "phone": "",
                        "website": "",
                        "linkedin": "",
                        "github": "",
                        "location": "",
                        "summary": "",
                        "experience": [],
                        "projects": [],
                        "education": [],
                        "skills": []
                      }
                    }
                    
                    Return ONLY valid JSON.
                    """;


            if (cachedSectionScores != null && !cachedSectionScores.isEmpty()) {
                StringBuilder cachedInstructions = getCachedInstructions(cachedSectionScores);
                systemPrompt += cachedInstructions.toString();
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("response_format", Map.of("type", "json_object"));
            requestBody.put("temperature", 0.2);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", "Analyze the following resume text:\n\n" + resumeText));
            requestBody.put("messages", messages);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API call failed with status: {}, body: {}", response.statusCode(), response.body());
                throw new RuntimeException("OpenAI API returned status " + response.statusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            String responseContent = rootNode.path("choices").get(0).path("message").path("content").asText();

            log.info("Received response from OpenAI, parsing to ResumeDetail model.");
            return objectMapper.readValue(responseContent, ResumeDetail.class);

        } catch (Exception e) {
            log.error("Error communicating with LLM or parsing response: {}", e.getMessage(), e);
            throw new RuntimeException("LLM parsing failed: " + e.getMessage(), e);
        }
    }

    private static @NotNull StringBuilder getCachedInstructions(Map<String, ResumeSectionScore> cachedSectionScores) {
        StringBuilder cachedInstructions = new StringBuilder();
        cachedInstructions.append("\n\nCRITICAL: The following sections in this resume are unchanged from a previous upload. You MUST reuse their scores and feedback comments exactly as provided below in the 'scoreBreakdown' field, and focus your analysis and feedback on the other parts of the resume:\n");
        for (Map.Entry<String, ResumeSectionScore> entry : cachedSectionScores.entrySet()) {
            cachedInstructions.append(String.format("- Section '%s': Score = %d, Comments = '%s'\n",
                    entry.getKey(), entry.getValue().getScore(), entry.getValue().getFeedback()));
        }
        return cachedInstructions;
    }
}
