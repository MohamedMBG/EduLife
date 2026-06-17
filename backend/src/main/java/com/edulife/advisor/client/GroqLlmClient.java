package com.edulife.advisor.client;

import com.edulife.advisor.config.AdvisorProperties;
import com.edulife.advisor.dto.AdvisorLlmResult;
import com.edulife.advisor.dto.CourseContextDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class GroqLlmClient implements LlmClient {

    private static final String SYSTEM_PROMPT = """
            You are the EduLife course advisor. Your job is to pick the single best course
            from the SHORTLIST below for the learner's stated goal.

            CRITICAL RULES — read carefully:
            1. You MUST choose exactly one courseId from the SHORTLIST. Never invent a courseId.
            2. PRIORITIZE DIRECT DOMAIN MATCH over general academic relevance.
               - If the learner asks about "Android apps" or "mobile apps", pick an Android/mobile
                 development course, NOT a math or science course.
               - If the learner asks about "web development", pick a web/frontend/backend course.
               - If the learner asks about "Bac math", pick a math/bac course.
               - A course is relevant ONLY if its topic directly addresses the learner's goal.
            3. Do NOT pick a course just because it is "generally useful" or "foundational".
               A math course is NOT relevant to someone who wants to build Android apps,
               even if math is generally useful in programming.
            4. If no course in the shortlist is a good match, return empty picks.
            5. Only answer about course recommendations. Refuse anything else.

            OUTPUT FORMAT — strict JSON only, no markdown fences:
            {
              "message": "<1-2 sentence explanation in the learner's language>",
              "picks": [
                {
                  "courseId": "<exact id from shortlist>",
                  "reason": "<why this course matches the goal>",
                  "confidence": <0.0 to 1.0>,
                  "matchedSkills": ["skill1", "skill2"]
                }
              ]
            }

            Max 2 picks. First pick = best match. Second pick = alternative only if close.
            confidence: 0.9+ = exact domain match, 0.7-0.9 = related, below 0.5 = weak match.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AdvisorProperties properties;

    public GroqLlmClient(RestClient.Builder builder, ObjectMapper objectMapper, AdvisorProperties properties) {
        this.restClient = builder
                .defaultHeader("Authorization", "Bearer " + properties.getGroqApiKey())
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AdvisorLlmResult recommend(String goal, List<CourseContextDto> catalog) {
        String catalogJson = serializeCatalog(catalog);
        String userMessage = "LEARNER GOAL:\n" + goal
                + "\n\nSHORTLIST (choose ONLY from these courseIds):\n" + catalogJson;

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", properties.getMaxTokens(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        String raw = restClient.post()
                .uri(properties.getGroqBaseUrl() + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseGroqResponse(raw);
    }

    private AdvisorLlmResult parseGroqResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            content = stripMarkdownFences(content);
            return objectMapper.readValue(content, AdvisorLlmResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Groq response: " + e.getMessage(), e);
        }
    }

    private String serializeCatalog(List<CourseContextDto> catalog) {
        try {
            return objectMapper.writeValueAsString(catalog);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return "";
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).stripTrailing();
            }
        }
        return trimmed;
    }
}
