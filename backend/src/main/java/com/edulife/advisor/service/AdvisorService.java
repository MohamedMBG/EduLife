package com.edulife.advisor.service;

import com.edulife.advisor.client.LlmClient;
import com.edulife.advisor.config.AdvisorProperties;
import com.edulife.advisor.dto.AdvisorLlmResult;
import com.edulife.advisor.dto.AdvisorRecommendationDto;
import com.edulife.advisor.dto.AdvisorRequest;
import com.edulife.advisor.dto.AdvisorResponse;
import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.advisor.entity.AdvisorLog;
import com.edulife.advisor.repository.AdvisorLogRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdvisorService {

    private static final int MAX_PICKS = 2;

    private final UserRepository userRepository;
    private final AdvisorLogRepository advisorLogRepository;
    private final CourseContextBuilder courseContextBuilder;
    private final LlmClient llmClient;
    private final AdvisorProperties properties;
    private final ObjectMapper objectMapper;

    public AdvisorService(
            UserRepository userRepository,
            AdvisorLogRepository advisorLogRepository,
            CourseContextBuilder courseContextBuilder,
            LlmClient llmClient,
            AdvisorProperties properties,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.advisorLogRepository = advisorLogRepository;
        this.courseContextBuilder = courseContextBuilder;
        this.llmClient = llmClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AdvisorResponse recommend(AdvisorRequest request) {
        User user = resolveCurrentUser();
        String sanitizedGoal = sanitizeGoal(request.goal());

        long start = System.currentTimeMillis();
        List<CourseContextDto> catalog = courseContextBuilder.build(sanitizedGoal);

        AdvisorResponse response;
        if (catalog.isEmpty()) {
            response = new AdvisorResponse("No courses are currently available.", List.of());
        } else {
            try {
                AdvisorLlmResult llmResult = llmClient.recommend(sanitizedGoal, catalog);
                response = validate(llmResult, catalog);
            } catch (Exception ex) {
                response = fallback();
            }
        }

        int latencyMs = (int) (System.currentTimeMillis() - start);
        advisorLogRepository.save(new AdvisorLog(
                user.getId(),
                sanitizedGoal,
                toJson(response),
                properties.getProvider(),
                properties.getModel(),
                latencyMs
        ));

        return response;
    }

    private AdvisorResponse validate(AdvisorLlmResult result, List<CourseContextDto> catalog) {
        if (result == null || result.picks() == null) {
            return fallback();
        }

        Set<UUID> allowedIds = catalog.stream()
                .map(CourseContextDto::id)
                .collect(Collectors.toSet());

        List<AdvisorRecommendationDto> recs = result.picks().stream()
                .filter(pick -> pick.courseId() != null && pick.reason() != null)
                .flatMap(pick -> {
                    try {
                        UUID id = UUID.fromString(pick.courseId());
                        if (allowedIds.contains(id)) {
                            return Stream.of(new AdvisorRecommendationDto(id, pick.reason(), 0.0));
                        }
                        return Stream.empty();
                    } catch (IllegalArgumentException e) {
                        return Stream.empty();
                    }
                })
                .limit(MAX_PICKS)
                .toList();

        String message = (result.message() != null) ? result.message() : "";
        return new AdvisorResponse(message, recs);
    }

    private AdvisorResponse fallback() {
        return new AdvisorResponse(
                "Unable to process your request at this time. Please try again later.",
                List.of()
        );
    }

    private String sanitizeGoal(String input) {
        return input.replaceAll("[\\x00-\\x1F\\x7F]", " ").strip();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required.");
        }
        String firebaseUid = firebaseAuth.getFirebaseUid();
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found. Call /auth/sync first."));
    }
}
