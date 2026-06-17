package com.edulife.advisor;

import com.edulife.advisor.client.LlmClient;
import com.edulife.advisor.config.AdvisorProperties;
import com.edulife.advisor.dto.AdvisorLlmResult;
import com.edulife.advisor.dto.AdvisorRecommendationDto;
import com.edulife.advisor.dto.AdvisorRequest;
import com.edulife.advisor.dto.AdvisorResponse;
import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.advisor.entity.AdvisorLog;
import com.edulife.advisor.repository.AdvisorLogRepository;
import com.edulife.advisor.service.AdvisorService;
import com.edulife.advisor.service.CourseContextBuilder;
import com.edulife.advisor.service.DeterministicRanker;
import com.edulife.advisor.service.IntentExtractor;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdvisorServiceTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COURSE_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FAKE_ID  = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    @Mock private UserRepository userRepository;
    @Mock private AdvisorLogRepository advisorLogRepository;
    @Mock private CourseContextBuilder courseContextBuilder;
    @Mock private LlmClient llmClient;

    private AdvisorService service;

    @BeforeEach
    void setUp() {
        AdvisorProperties props = new AdvisorProperties();
        props.setProvider("groq");
        props.setModel("llama-3.3-70b-versatile");

        IntentExtractor intentExtractor = new IntentExtractor();
        DeterministicRanker deterministicRanker = new DeterministicRanker();

        service = new AdvisorService(
                userRepository, advisorLogRepository, courseContextBuilder,
                intentExtractor, deterministicRanker,
                llmClient, props, new ObjectMapper()
        );

        mockFirebaseAuth();
        mockUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validLlmResultReturnsRecommendations() {
        List<CourseContextDto> catalog = catalogWith(COURSE_A, COURSE_B);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList())).willReturn(
                new AdvisorLlmResult("Here are your picks", List.of(
                        new AdvisorLlmResult.Pick(COURSE_A.toString(), "Great for beginners", 0.9, List.of("Java")),
                        new AdvisorLlmResult.Pick(COURSE_B.toString(), "Covers the fundamentals", 0.7, List.of("OOP"))
                ))
        );

        AdvisorResponse response = service.recommend(new AdvisorRequest("I want to learn Java"));

        assertThat(response.message()).isEqualTo("Here are your picks");
        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.recommendations().get(0).courseId()).isEqualTo(COURSE_A);
        assertThat(response.recommendations().get(1).courseId()).isEqualTo(COURSE_B);
        assertThat(response.source()).isEqualTo("groq");
    }

    @Test
    void fakeCourseIdIsDropped() {
        List<CourseContextDto> catalog = catalogWith(COURSE_A);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList())).willReturn(
                new AdvisorLlmResult("Result", List.of(
                        new AdvisorLlmResult.Pick(COURSE_A.toString(), "Valid", 0.9, List.of()),
                        new AdvisorLlmResult.Pick(FAKE_ID.toString(), "Invented", 0.5, List.of())
                ))
        );

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).courseId()).isEqualTo(COURSE_A);
    }

    @Test
    void moreThanTwoPicksAreCappedAtTwo() {
        UUID courseC = UUID.fromString("33333333-3333-3333-3333-333333333333");
        List<CourseContextDto> catalog = catalogWith(COURSE_A, COURSE_B, courseC);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList())).willReturn(
                new AdvisorLlmResult("Many picks", List.of(
                        new AdvisorLlmResult.Pick(COURSE_A.toString(), "First", 0.9, List.of()),
                        new AdvisorLlmResult.Pick(COURSE_B.toString(), "Second", 0.8, List.of()),
                        new AdvisorLlmResult.Pick(courseC.toString(), "Third", 0.6, List.of())
                ))
        );

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));

        assertThat(response.recommendations()).hasSize(2);
    }

    @Test
    void invalidJsonFromLlmTriggersDeterministicFallback() {
        List<CourseContextDto> catalog = catalogWith(COURSE_A);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList()))
                .willThrow(new IllegalStateException("Failed to parse Groq response"));

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));

        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.source()).isEqualTo("deterministic-fallback");
    }

    @Test
    void llmExceptionTriggersDeterministicFallback() {
        List<CourseContextDto> catalog = catalogWith(COURSE_A);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList()))
                .willThrow(new RuntimeException("Connection timeout"));

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));

        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.source()).isEqualTo("deterministic-fallback");
    }

    @Test
    void emptyCatalogReturnsEmptyRecommendationsWithoutCallingLlm() {
        given(courseContextBuilder.build(anyString())).willReturn(List.of());

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));

        assertThat(response.recommendations()).isEmpty();
        verify(llmClient, never()).recommend(anyString(), anyList());
    }

    @Test
    void nonGarbageStringCourseIdIsDropped() {
        List<CourseContextDto> catalog = catalogWith(COURSE_A);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList())).willReturn(
                new AdvisorLlmResult("Result", List.of(
                        new AdvisorLlmResult.Pick("not-a-valid-uuid", "some reason", 0.5, List.of())
                ))
        );

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));

        assertThat(response.source()).isEqualTo("deterministic-fallback");
    }

    @Test
    void eachRequestIsLogged() {
        given(courseContextBuilder.build(anyString())).willReturn(List.of());

        service.recommend(new AdvisorRequest("I want to learn Python"));

        ArgumentCaptor<AdvisorLog> logCaptor = ArgumentCaptor.forClass(AdvisorLog.class);
        verify(advisorLogRepository).save(logCaptor.capture());

        AdvisorLog log = logCaptor.getValue();
        assertThat(log.getUserId()).isEqualTo(USER_ID);
        assertThat(log.getGoal()).isEqualTo("I want to learn Python");
        assertThat(log.getProvider()).isEqualTo("groq");
        assertThat(log.getModel()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(log.getLatencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(log.getResponseJson()).isNotBlank();
    }

    @Test
    void recommendationScoreReflectsLlmConfidence() {
        List<CourseContextDto> catalog = catalogWith(COURSE_A);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList())).willReturn(
                new AdvisorLlmResult("ok", List.of(
                        new AdvisorLlmResult.Pick(COURSE_A.toString(), "good fit", 0.92, List.of("Android"))
                ))
        );

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));

        AdvisorRecommendationDto rec = response.recommendations().get(0);
        assertThat(rec.score()).isEqualTo(0.92);
        assertThat(rec.matchedSkills()).contains("Android");
    }

    @Test
    void responseIncludesSourceField() {
        List<CourseContextDto> catalog = catalogWith(COURSE_A);
        given(courseContextBuilder.build(anyString())).willReturn(catalog);
        given(llmClient.recommend(anyString(), anyList())).willReturn(
                new AdvisorLlmResult("ok", List.of(
                        new AdvisorLlmResult.Pick(COURSE_A.toString(), "match", 0.85, List.of())
                ))
        );

        AdvisorResponse response = service.recommend(new AdvisorRequest("goal"));
        assertThat(response.source()).isEqualTo("groq");
    }

    private List<CourseContextDto> catalogWith(UUID... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> new CourseContextDto(id, "Course " + id, "Desc", "Full description",
                        "BEGINNER", "en", List.of(), List.of()))
                .toList();
    }

    private void mockUser() {
        User user = mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        given(userRepository.findByFirebaseUid(anyString())).willReturn(Optional.of(user));
    }

    private void mockFirebaseAuth() {
        FirebaseAuthentication firebaseAuth = mock(FirebaseAuthentication.class);
        given(firebaseAuth.getFirebaseUid()).willReturn("firebase-uid-123");

        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(firebaseAuth);
        SecurityContextHolder.setContext(securityContext);
    }
}
