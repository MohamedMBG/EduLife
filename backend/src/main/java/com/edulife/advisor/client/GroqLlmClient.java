package com.edulife.advisor.client;

import com.edulife.advisor.config.AdvisorProperties;
import com.edulife.advisor.dto.AdvisorLlmResult;
import com.edulife.advisor.dto.CourseContextDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class GroqLlmClient implements LlmClient {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Locked system prompt — never altered at runtime
    private static final String SYSTEM_PROMPT = """
            You are EduLife course advisor.

            You only answer about course recommendations from the provided EduLife course catalog.

            You must refuse anything unrelated, including:
            - general chat
            - code help
            - personal advice
            - politics
            - medical questions
            - legal questions
            - anything outside EduLife course guidance

            Output strict JSON only:

            {
              "message": "<short reason in user language>",
              "picks": [
                {
                  "courseId": "<id from catalog>",
                  "reason": "<one line>"
                }
              ]
            }

            Rules:
            - Max 2 picks.
            - Never invent courseIds.
            - Only use courseIds from the provided catalog.
            - If no good match exists, return empty picks.
            - Do not recommend unpublished courses.
            - Do not answer questions outside EduLife course recommendations.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AdvisorProperties properties;

    public GroqLlmClient(RestClient.Builder builder, ObjectMapper objectMapper, AdvisorProperties properties) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AdvisorLlmResult recommend(String goal, List<CourseContextDto> catalog) {
        String catalogJson = serializeCatalog(catalog);
        String userMessage = "USER GOAL:\n" + goal + "\n\nCATALOG:\n" + catalogJson;

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", properties.getMaxTokens(),
                "temperature", 0.3
        );

        String raw = restClient.post()
                .uri(GROQ_URL)
                .header("Authorization", "Bearer " + properties.getGroqApiKey())
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
