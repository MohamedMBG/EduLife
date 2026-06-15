package com.edulife.gamification;

import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.exams.repository.ExamAttemptRepository;
import com.edulife.gamification.dto.GamificationStateDto;
import com.edulife.gamification.entity.GamificationXpEvent;
import com.edulife.gamification.entity.UserBadge;
import com.edulife.gamification.entity.UserGamificationState;
import com.edulife.gamification.model.BadgeCatalog;
import com.edulife.gamification.model.XpEventType;
import com.edulife.gamification.repository.GamificationXpEventRepository;
import com.edulife.gamification.repository.UserBadgeRepository;
import com.edulife.gamification.repository.UserGamificationStateRepository;
import com.edulife.gamification.service.GamificationService;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.progress.repository.LessonProgressRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock private UserGamificationStateRepository stateRepository;
    @Mock private GamificationXpEventRepository eventRepository;
    @Mock private UserBadgeRepository badgeRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private ExamAttemptRepository examAttemptRepository;
    @Mock private CertificateRepository certificateRepository;

    private Clock clock;
    private GamificationService service;
    private Map<UUID, UserGamificationState> stateStore;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));
        stateStore = new HashMap<>();

        lenient().when(stateRepository.findById(any(UUID.class)))
                .thenAnswer(inv -> Optional.ofNullable(stateStore.get(inv.getArgument(0))));
        lenient().when(stateRepository.save(any(UserGamificationState.class)))
                .thenAnswer(inv -> {
                    UserGamificationState s = inv.getArgument(0);
                    stateStore.put(s.getUserId(), s);
                    return s;
                });
        lenient().when(badgeRepository.findBadgeIdsByUserId(any(UUID.class)))
                .thenReturn(new java.util.HashSet<>());

        service = new GamificationService(
                stateRepository, eventRepository, badgeRepository, profileRepository,
                lessonProgressRepository, examAttemptRepository, certificateRepository, clock
        );
    }

    @Test
    void firstLessonAwardsXpAndUnlocksFirstFlame() {
        UUID lessonId = UUID.randomUUID();
        given(eventRepository.existsByDedupKey(anyString())).willReturn(false);
        given(lessonProgressRepository.countByUserId(USER_ID)).willReturn(1L);

        service.onLessonCompleted(USER_ID, lessonId);

        UserGamificationState state = stateStore.get(USER_ID);
        assertThat(state.getTotalXp()).isEqualTo(XpEventType.LESSON_COMPLETED.xp());
        assertThat(state.getCurrentStreak()).isEqualTo(1);
        assertThat(state.getLastActivityDate()).isEqualTo(LocalDate.parse("2026-06-15"));

        verify(badgeRepository, times(1))
                .saveAndFlush(argThatBadgeIs(BadgeCatalog.FIRST_FLAME));
    }

    @Test
    void duplicateDedupKeySkipsXpAndStreak() {
        UUID lessonId = UUID.randomUUID();
        given(eventRepository.existsByDedupKey(anyString())).willReturn(true);

        service.onLessonCompleted(USER_ID, lessonId);

        assertThat(stateStore).doesNotContainKey(USER_ID);
        verify(eventRepository, never()).saveAndFlush(any(GamificationXpEvent.class));
    }

    @Test
    void streakBonusThreeFiresOnceAfterThreeConsecutiveDays() {
        given(eventRepository.existsByDedupKey(anyString())).willReturn(false);
        given(lessonProgressRepository.countByUserId(USER_ID)).willReturn(0L);

        advanceClockTo("2026-06-13T10:00:00Z");
        service.onDailyLogin(USER_ID);
        advanceClockTo("2026-06-14T10:00:00Z");
        service.onDailyLogin(USER_ID);
        advanceClockTo("2026-06-15T10:00:00Z");
        service.onDailyLogin(USER_ID);

        UserGamificationState state = stateStore.get(USER_ID);
        assertThat(state.getCurrentStreak()).isEqualTo(3);
        assertThat(state.isStreakBonus3Awarded()).isTrue();
        // 3 daily logins (5 xp each) + one 3-day bonus (30 xp)
        assertThat(state.getTotalXp()).isEqualTo(3 * 5 + 30);
    }

    @Test
    void streakResetsAfterMissedDay() {
        given(eventRepository.existsByDedupKey(anyString())).willReturn(false);
        given(lessonProgressRepository.countByUserId(USER_ID)).willReturn(0L);

        advanceClockTo("2026-06-10T10:00:00Z");
        service.onDailyLogin(USER_ID);
        advanceClockTo("2026-06-15T10:00:00Z"); // 5-day gap → reset
        service.onDailyLogin(USER_ID);

        UserGamificationState state = stateStore.get(USER_ID);
        assertThat(state.getCurrentStreak()).isEqualTo(1);
        assertThat(state.isStreakBonus3Awarded()).isFalse();
        assertThat(state.isStreakBonus7Awarded()).isFalse();
    }

    @Test
    void getStateReturnsLevelMetadataAndBadgeCatalog() {
        UserGamificationState seeded = new UserGamificationState(USER_ID);
        seeded.addXp(700);
        seeded.setLevel(3);
        stateStore.put(USER_ID, seeded);

        GamificationStateDto dto = service.getState(USER_ID);
        assertThat(dto.totalXp()).isEqualTo(700);
        assertThat(dto.level()).isEqualTo(3);
        assertThat(dto.levelName()).isEqualTo("Explorer");
        assertThat(dto.currentLevelXp()).isEqualTo(600);
        assertThat(dto.nextLevelXp()).isEqualTo(1100);
        assertThat(dto.xpIntoLevel()).isEqualTo(100);
        assertThat(dto.xpForNextLevel()).isEqualTo(500);
        assertThat(dto.badges()).hasSize(BadgeCatalog.all().size());
    }

    @Test
    void enrollmentAwardsTenXp() {
        UUID enrollmentId = UUID.randomUUID();
        given(eventRepository.existsByDedupKey(anyString())).willReturn(false);
        given(lessonProgressRepository.countByUserId(USER_ID)).willReturn(0L);

        service.onEnrollment(USER_ID, enrollmentId);

        assertThat(stateStore.get(USER_ID).getTotalXp()).isEqualTo(XpEventType.ENROLLMENT.xp());
    }

    private void advanceClockTo(String iso) {
        clock = Clock.fixed(Instant.parse(iso), ZoneId.of("UTC"));
        service = new GamificationService(
                stateRepository, eventRepository, badgeRepository, profileRepository,
                lessonProgressRepository, examAttemptRepository, certificateRepository, clock
        );
    }

    private static org.mockito.ArgumentMatcher<UserBadge> badgeMatcher(String badgeId) {
        return badge -> badge != null && badgeId.equals(badge.getBadgeId());
    }

    private static UserBadge argThatBadgeIs(String badgeId) {
        return org.mockito.ArgumentMatchers.argThat(badgeMatcher(badgeId));
    }
}
