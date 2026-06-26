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
import com.edulife.advisor.service.DeterministicRanker.ScoredCourse;
import com.edulife.advisor.service.IntentExtractor.IntentResult;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orchestrates the course recommendation pipeline: intent extraction, deterministic ranking,
 * LLM-based recommendation (with deterministic fallback), and audit logging.
 */
@Service
public class AdvisorService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);
    private static final int MAX_PICKS = 2;

    private final UserRepository userRepository;
    private final AdvisorLogRepository advisorLogRepository;
    private final CourseContextBuilder courseContextBuilder;
    private final IntentExtractor intentExtractor;
    private final DeterministicRanker deterministicRanker;
    private final LlmClient llmClient;
    private final AdvisorProperties properties;
    private final ObjectMapper objectMapper;

    public AdvisorService(
            UserRepository userRepository,
            AdvisorLogRepository advisorLogRepository,
            CourseContextBuilder courseContextBuilder,
            IntentExtractor intentExtractor,
            DeterministicRanker deterministicRanker,
            LlmClient llmClient,
            AdvisorProperties properties,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.advisorLogRepository = advisorLogRepository;
        this.courseContextBuilder = courseContextBuilder;
        this.intentExtractor = intentExtractor;
        this.deterministicRanker = deterministicRanker;
        this.llmClient = llmClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes a recommendation request for the authenticated user: sanitizes the goal,
     * runs the recommendation pipeline, logs the result, and returns the response.
     */
    public AdvisorResponse recommend(AdvisorRequest request) {
        User user = resolveCurrentUser();
        String sanitizedGoal = sanitizeGoal(request.goal());

        long start = System.currentTimeMillis();
        List<CourseContextDto> catalog = courseContextBuilder.build(sanitizedGoal);

        AdvisorResponse response;
        if (catalog.isEmpty()) {
            response = new AdvisorResponse("No courses are currently available.", List.of());
        } else {
            response = runPipeline(sanitizedGoal, catalog);
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

    /** Runs intent extraction, deterministic shortlisting, then LLM call with deterministic fallback. */
    private AdvisorResponse runPipeline(String goal, List<CourseContextDto> catalog) {
        IntentResult intent = intentExtractor.extract(goal);
        List<ScoredCourse> shortlist = deterministicRanker.shortlist(catalog, intent);

        List<CourseContextDto> shortlistCourses = shortlist.stream()
                .map(ScoredCourse::course)
                .toList();

        try {
            AdvisorLlmResult llmResult = llmClient.recommend(goal, shortlistCourses);
            AdvisorResponse groqResponse = validateLlmResult(llmResult, catalog);
            if (!groqResponse.recommendations().isEmpty()) {
                return new AdvisorResponse(groqResponse.message(), groqResponse.recommendations(), "groq");
            }
        } catch (Exception ex) {
            log.warn("Groq call failed, using deterministic fallback: {}", ex.getMessage());
        }

        return buildDeterministicResponse(goal, shortlist);
    }

    /** Validates LLM picks against the catalog, filtering out unknown course IDs and clamping confidence. */
    private AdvisorResponse validateLlmResult(AdvisorLlmResult result, List<CourseContextDto> catalog) {
        if (result == null || result.picks() == null) {
            return new AdvisorResponse("", List.of());
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
                            double confidence = pick.confidence() != null
                                    ? Math.min(1.0, Math.max(0.0, pick.confidence()))
                                    : 0.0;
                            List<String> skills = pick.matchedSkills() != null
                                    ? pick.matchedSkills()
                                    : List.of();
                            return Stream.of(new AdvisorRecommendationDto(id, pick.reason(), confidence, skills));
                        }
                        return Stream.empty();
                    } catch (IllegalArgumentException e) {
                        return Stream.empty();
                    }
                })
                .limit(MAX_PICKS)
                .toList();

        String message = (result.message() != null) ? result.message() : "";
        return new AdvisorResponse(message, recs, "groq");
    }

    /** Builds a recommendation response using keyword-based scores when the LLM is unavailable. */
    private AdvisorResponse buildDeterministicResponse(String goal, List<ScoredCourse> shortlist) {
        if (shortlist.isEmpty()) {
            return new AdvisorResponse(
                    "I could not find a course matching your goal. Try being more specific.",
                    List.of());
        }

        List<AdvisorRecommendationDto> recs = new ArrayList<>();
        ScoredCourse best = shortlist.get(0);
        double bestScore = normalizeScore(best.score(), shortlist);
        recs.add(new AdvisorRecommendationDto(
                best.course().id(),
                buildReason(best),
                bestScore,
                List.copyOf(best.matchedKeywords())
        ));

        if (shortlist.size() > 1) {
            ScoredCourse second = shortlist.get(1);
            if (second.score() >= best.score() / 2) {
                double secondScore = normalizeScore(second.score(), shortlist);
                recs.add(new AdvisorRecommendationDto(
                        second.course().id(),
                        buildReason(second),
                        secondScore,
                        List.copyOf(second.matchedKeywords())
                ));
            }
        }

        String message = String.format(
                "Based on your goal \"%s\", I recommend \"%s\" as the best match from the current catalog.",
                goal, best.course().title());

        return new AdvisorResponse(message, recs, "deterministic-fallback");
    }

    /** Normalizes a raw keyword score to a 0.0-1.0 confidence range relative to the best score. */
    private double normalizeScore(int rawScore, List<ScoredCourse> shortlist) {
        int maxPossible = shortlist.stream().mapToInt(ScoredCourse::score).max().orElse(1);
        if (maxPossible <= 0) return 0.5;
        return Math.min(1.0, (double) rawScore / maxPossible);
    }

    private String buildReason(ScoredCourse scored) {
        if (scored.matchedKeywords().isEmpty()) {
            return "This is the closest match in the current catalog.";
        }
        String keywords = scored.matchedKeywords().stream().limit(4).collect(Collectors.joining(", "));
        return String.format("Matches your interest in %s. %s level.",
                keywords, scored.course().level() != null ? scored.course().level() : "Current");
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
