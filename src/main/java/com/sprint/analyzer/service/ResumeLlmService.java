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

            String systemPrompt = "You are an expert resume parser and reviewer. Analyze the provided resume text and return a structured JSON response.\n" +
                    "You MUST respond with a valid JSON object matching this schema:\n" +
                    "{\n" +
                    "  \"candidateName\": \"Full Name of Candidate\",\n" +
                    "  \"detailedFeedback\": \"Detailed, professional feedback about the resume structure, content, and formatting\",\n" +
                    "  \"strengths\": [\n" +
                    "    \"strength 1\",\n" +
                    "    \"strength 2\"\n" +
                    "  ],\n" +
                    "  \"improvements\": [\n" +
                    "    \"improvement 1\",\n" +
                    "    \"improvement 2\"\n" +
                    "  ],\n" +
                    "  \"scoreBreakdown\": [\n" +
                    "    {\n" +
                    "      \"category\": \"Content & Experience\",\n" +
                    "      \"score\": 85,\n" +
                    "      \"comments\": \"Comments on content\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"category\": \"Impact & Achievements\",\n" +
                    "      \"score\": 75,\n" +
                    "      \"comments\": \"Comments on impact\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"category\": \"Education & Skills\",\n" +
                    "      \"score\": 90,\n" +
                    "      \"comments\": \"Comments on skills\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"category\": \"Formatting & Design\",\n" +
                    "      \"score\": 80,\n" +
                    "      \"comments\": \"Comments on design\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"overallScore\": 83,\n" +
                    "  \"resumeData\": {\n" +
                    "    \"name\": \"Candidate Name\",\n" +
                    "    \"title\": \"Professional Title (e.g. Senior Software Engineer)\",\n" +
                    "    \"email\": \"Email Address\",\n" +
                    "    \"phone\": \"Phone Number\",\n" +
                    "    \"website\": \"Website/Portfolio URL\",\n" +
                    "    \"location\": \"Location (e.g. San Francisco, CA)\",\n" +
                    "    \"summary\": \"Brief summary statement\",\n" +
                    "    \"experience\": [\n" +
                    "      {\n" +
                    "        \"company\": \"Company Name\",\n" +
                    "        \"role\": \"Role/Job Title\",\n" +
                    "        \"duration\": \"Duration (e.g. 2022 - Present)\",\n" +
                    "        \"bullets\": [\n" +
                    "          \"bullet point 1\",\n" +
                    "          \"bullet point 2\"\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"education\": [\n" +
                    "      {\n" +
                    "        \"school\": \"School Name\",\n" +
                    "        \"degree\": \"Degree (e.g. B.S. in Computer Science)\",\n" +
                    "        \"duration\": \"Duration (e.g. 2016 - 2020)\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"skills\": [\n" +
                    "      \"Skill 1\",\n" +
                    "      \"Skill 2\"\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}\n\n" +
                    "The scoreBreakdown MUST contain 4 categories: \"Content & Experience\", \"Impact & Achievements\", \"Education & Skills\", and \"Formatting & Design\", each with a score between 0 and 100.\n" +
                    "The overallScore MUST be the average of the 4 breakdown scores.\n" +
                    "Ensure you extract the values accurately from the resume text. Do NOT hallucinate names, emails, companies or details. Only extract what is present in the resume text. If something is missing, leave the field empty or use empty array/null.";

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
