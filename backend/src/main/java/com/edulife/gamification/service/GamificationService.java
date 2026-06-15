package com.edulife.gamification.service;

import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.exams.repository.ExamAttemptRepository;
import com.edulife.gamification.dto.BadgeDto;
import com.edulife.gamification.dto.GamificationStateDto;
import com.edulife.gamification.dto.LeaderboardEntryDto;
import com.edulife.gamification.entity.GamificationXpEvent;
import com.edulife.gamification.entity.UserBadge;
import com.edulife.gamification.entity.UserGamificationState;
import com.edulife.gamification.model.BadgeCatalog;
import com.edulife.gamification.model.BadgeDefinition;
import com.edulife.gamification.model.LevelTable;
import com.edulife.gamification.model.XpEventType;
import com.edulife.gamification.repository.GamificationXpEventRepository;
import com.edulife.gamification.repository.UserBadgeRepository;
import com.edulife.gamification.repository.UserGamificationStateRepository;
import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.progress.repository.LessonProgressRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single source of truth for XP, levels, streaks, and badge unlocks. Web and
 * Android render the values returned here and must never compute progression
 * locally — that rule is part of the gamification spec in CLAUDE.md.
 *
 * Activity dates use UTC because the platform stores progress timestamps in UTC
 * and no client-supplied timezone is trusted. Per-PR-A acknowledged trade-off.
 */
@Service
public class GamificationService {

    private static final ZoneId ACTIVITY_ZONE = ZoneId.of("UTC");

    private final UserGamificationStateRepository stateRepository;
    private final GamificationXpEventRepository eventRepository;
    private final UserBadgeRepository badgeRepository;
    private final ProfileRepository profileRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CertificateRepository certificateRepository;
    private final Clock clock;

    public GamificationService(
            UserGamificationStateRepository stateRepository,
            GamificationXpEventRepository eventRepository,
            UserBadgeRepository badgeRepository,
            ProfileRepository profileRepository,
            LessonProgressRepository lessonProgressRepository,
            ExamAttemptRepository examAttemptRepository,
            CertificateRepository certificateRepository,
            Clock clock
    ) {
        this.stateRepository = stateRepository;
        this.eventRepository = eventRepository;
        this.badgeRepository = badgeRepository;
        this.profileRepository = profileRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.certificateRepository = certificateRepository;
        this.clock = clock;
    }

    // -- Emission hooks ------------------------------------------------------

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollment(UUID userId, UUID enrollmentId) {
        String dedupKey = "enrollment:" + enrollmentId;
        awardXpInternal(userId, XpEventType.ENROLLMENT, enrollmentId.toString(), dedupKey);
        evaluateBadges(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLessonCompleted(UUID userId, UUID lessonId) {
        String dedupKey = "lesson:" + lessonId + ":" + userId;
        boolean awarded = awardXpInternal(userId, XpEventType.LESSON_COMPLETED, lessonId.toString(), dedupKey);
        if (awarded) {
            applyDailyActivity(userId);
        }
        evaluateBadges(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCourseCompleted(UUID userId, UUID courseId) {
        String dedupKey = "course:" + courseId + ":" + userId;
        awardXpInternal(userId, XpEventType.COURSE_COMPLETED, courseId.toString(), dedupKey);
        evaluateBadges(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExamPassed(UUID userId, UUID examAttemptId) {
        String dedupKey = "exam:" + examAttemptId;
        awardXpInternal(userId, XpEventType.EXAM_PASSED, examAttemptId.toString(), dedupKey);
        evaluateBadges(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCertificateEarned(UUID userId, UUID certificateId) {
        String dedupKey = "certificate:" + certificateId;
        awardXpInternal(userId, XpEventType.CERTIFICATE_EARNED, certificateId.toString(), dedupKey);
        evaluateBadges(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDailyLogin(UUID userId) {
        LocalDate today = LocalDate.now(clock.withZone(ACTIVITY_ZONE));
        String dedupKey = "login:" + userId + ":" + today;
        boolean awarded = awardXpInternal(userId, XpEventType.DAILY_LOGIN, today.toString(), dedupKey);
        if (awarded) {
            applyDailyActivity(userId);
        }
        evaluateBadges(userId);
    }

    // -- Reads ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public GamificationStateDto getState(UUID userId) {
        UserGamificationState state = stateRepository.findById(userId)
                .orElseGet(() -> new UserGamificationState(userId));

        Set<String> unlocked = state.getUserId() == null
                ? Set.of()
                : badgeRepository.findBadgeIdsByUserId(userId);
        Map<String, Instant> unlockedAt = new HashMap<>();
        if (!unlocked.isEmpty()) {
            for (UserBadge ub : badgeRepository.findAllByIdUserIdOrderByUnlockedAtAsc(userId)) {
                unlockedAt.put(ub.getBadgeId(), ub.getUnlockedAt());
            }
        }

        List<BadgeDto> badges = new ArrayList<>(BadgeCatalog.all().size());
        for (BadgeDefinition def : BadgeCatalog.all()) {
            boolean isUnlocked = unlocked.contains(def.id());
            badges.add(new BadgeDto(
                    def.id(),
                    def.label(),
                    def.rarity().name(),
                    def.unlockDescription(),
                    isUnlocked,
                    isUnlocked ? unlockedAt.get(def.id()) : null
            ));
        }

        int level = state.getLevel();
        int currentLevelXp = LevelTable.xpForLevel(level);
        boolean atMax = level >= LevelTable.MAX_LEVEL;
        int nextLevelXp = atMax ? currentLevelXp : LevelTable.xpForLevel(level + 1);
        int xpIntoLevel = state.getTotalXp() - currentLevelXp;
        int xpForNextLevel = atMax ? 0 : nextLevelXp - currentLevelXp;

        return new GamificationStateDto(
                state.getTotalXp(),
                level,
                LevelTable.nameForLevel(level),
                currentLevelXp,
                nextLevelXp,
                xpIntoLevel,
                xpForNextLevel,
                state.getCurrentStreak(),
                state.getLongestStreak(),
                state.getLastActivityDate(),
                badges
        );
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        List<UserGamificationState> top = stateRepository
                .findAllByOrderByTotalXpDescUpdatedAtAsc(PageRequest.of(0, safeLimit));

        if (top.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> displayNames = new HashMap<>();
        List<UUID> userIds = top.stream().map(UserGamificationState::getUserId).toList();
        for (Profile p : profileRepository.findAllByUserIdIn(userIds)) {
            String name = p.getDisplayName();
            displayNames.put(p.getUserId(), (name == null || name.isBlank()) ? "Learner" : name);
        }

        List<LeaderboardEntryDto> result = new ArrayList<>(top.size());
        for (int i = 0; i < top.size(); i++) {
            UserGamificationState s = top.get(i);
            result.add(new LeaderboardEntryDto(
                    i + 1,
                    s.getUserId(),
                    displayNames.getOrDefault(s.getUserId(), "Learner"),
                    s.getTotalXp(),
                    s.getLevel(),
                    LevelTable.nameForLevel(s.getLevel())
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<BadgeDto> listBadgeDefinitions() {
        List<BadgeDto> out = new ArrayList<>(BadgeCatalog.all().size());
        for (BadgeDefinition def : BadgeCatalog.all()) {
            out.add(new BadgeDto(def.id(), def.label(), def.rarity().name(), def.unlockDescription(), false, null));
        }
        return out;
    }

    // -- Internals -----------------------------------------------------------

    private boolean awardXpInternal(UUID userId, XpEventType type, String sourceRef, String dedupKey) {
        if (eventRepository.existsByDedupKey(dedupKey)) {
            return false;
        }
        try {
            eventRepository.saveAndFlush(new GamificationXpEvent(userId, type, type.xp(), sourceRef, dedupKey));
        } catch (DataIntegrityViolationException dup) {
            // Concurrent emitter beat us to the same dedup_key. Treat as no-op.
            return false;
        }

        UserGamificationState state = stateRepository.findById(userId)
                .orElseGet(() -> stateRepository.save(new UserGamificationState(userId)));
        state.addXp(type.xp());
        state.setLevel(LevelTable.levelFor(state.getTotalXp()));
        stateRepository.save(state);
        return true;
    }

    private void applyDailyActivity(UUID userId) {
        UserGamificationState state = stateRepository.findById(userId)
                .orElseGet(() -> stateRepository.save(new UserGamificationState(userId)));

        LocalDate today = LocalDate.now(clock.withZone(ACTIVITY_ZONE));
        LocalDate last = state.getLastActivityDate();

        if (today.equals(last)) {
            return;
        }

        int newStreak;
        if (last == null) {
            newStreak = 1;
            resetStreakBonusFlags(state);
        } else {
            long days = ChronoUnit.DAYS.between(last, today);
            if (days == 1) {
                newStreak = state.getCurrentStreak() + 1;
            } else {
                newStreak = 1;
                resetStreakBonusFlags(state);
            }
        }

        state.setCurrentStreak(newStreak);
        state.setLastActivityDate(today);
        stateRepository.save(state);

        if (newStreak >= 3 && !state.isStreakBonus3Awarded()) {
            String key = "streak3:" + userId + ":" + today;
            if (awardXpInternal(userId, XpEventType.STREAK_BONUS_3, key, key)) {
                state.setStreakBonus3Awarded(true);
                stateRepository.save(state);
            }
        }
        if (newStreak >= 7 && !state.isStreakBonus7Awarded()) {
            String key = "streak7:" + userId + ":" + today;
            if (awardXpInternal(userId, XpEventType.STREAK_BONUS_7, key, key)) {
                state.setStreakBonus7Awarded(true);
                stateRepository.save(state);
            }
        }
    }

    private void resetStreakBonusFlags(UserGamificationState state) {
        state.setStreakBonus3Awarded(false);
        state.setStreakBonus7Awarded(false);
    }

    private void evaluateBadges(UUID userId) {
        Set<String> already = badgeRepository.findBadgeIdsByUserId(userId);
        UserGamificationState state = stateRepository.findById(userId)
                .orElseGet(() -> new UserGamificationState(userId));

        long lessonsTotal = lessonProgressRepository.countByUserId(userId);
        long examsPassed = examAttemptRepository.countByUserIdAndPassedTrue(userId);
        long certificates = certificateRepository.countByUserId(userId);

        Instant now = Instant.now(clock);
        long lessonsToday = eventRepository.countByUserIdAndEventTypeAndCreatedAtAfter(
                userId, XpEventType.LESSON_COMPLETED,
                now.minus(1, ChronoUnit.DAYS));
        long lessonsWeek = eventRepository.countByUserIdAndEventTypeAndCreatedAtAfter(
                userId, XpEventType.LESSON_COMPLETED,
                now.minus(7, ChronoUnit.DAYS));

        if (lessonsTotal >= 1) unlockBadge(userId, BadgeCatalog.FIRST_FLAME, already);
        if (lessonsTotal >= 10) unlockBadge(userId, BadgeCatalog.BOOKWORM, already);
        if (lessonsToday >= 3) unlockBadge(userId, BadgeCatalog.SPEED_RUN, already);
        if (examsPassed >= 1) unlockBadge(userId, BadgeCatalog.SHARP_MIND, already);
        if (certificates >= 1) unlockBadge(userId, BadgeCatalog.GRADUATE, already);
        if (certificates >= 3) unlockBadge(userId, BadgeCatalog.TROPHY_HUNTER, already);
        if (lessonsWeek >= 5) unlockBadge(userId, BadgeCatalog.ON_A_ROLL, already);
        if (state.getCurrentStreak() >= 14) unlockBadge(userId, BadgeCatalog.DEDICATED, already);
        if (state.getCurrentStreak() >= 30) unlockBadge(userId, BadgeCatalog.STAR_LEARNER, already);
        if (state.getCurrentStreak() >= 60) unlockBadge(userId, BadgeCatalog.INFERNO, already);
        if (state.getLevel() >= 7) unlockBadge(userId, BadgeCatalog.SCHOLAR, already);
        if (state.getLevel() >= 10) unlockBadge(userId, BadgeCatalog.MASTER, already);
    }

    private void unlockBadge(UUID userId, String badgeId, Set<String> already) {
        if (already.contains(badgeId)) {
            return;
        }
        try {
            badgeRepository.saveAndFlush(new UserBadge(userId, badgeId));
            already.add(badgeId);
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent unlocker won — treat as already unlocked.
            already.add(badgeId);
        }
    }
}
