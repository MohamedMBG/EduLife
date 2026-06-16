package com.baghdad.edulife.features.analytics.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.analytics.model.AnalyticsInsight;
import com.baghdad.edulife.features.analytics.model.CourseProgressAnalytics;
import com.baghdad.edulife.features.analytics.model.DayStudyActivity;
import com.baghdad.edulife.features.analytics.model.ExamPerformanceSummary;
import com.baghdad.edulife.features.analytics.model.LearningStreakSummary;
import com.baghdad.edulife.features.analytics.model.PlatformAnalytics;
import com.baghdad.edulife.features.analytics.model.PlatformCohortAnalytics;
import com.baghdad.edulife.features.analytics.model.StudentAnalyticsSummary;
import com.baghdad.edulife.features.analytics.model.StudentProgressTrend;
import com.baghdad.edulife.features.analytics.model.StudyAnalytics;
import com.baghdad.edulife.features.analytics.model.TeacherAnalytics;
import com.baghdad.edulife.features.analytics.model.WeeklyStudyActivity;
import com.baghdad.edulife.features.courses.model.CourseProgressSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.gamification.model.GamificationStateResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Data access for the read-only analytics endpoints. Mirrors the existing AdminRepository pattern:
 * Retrofit calls via the shared ApiClient, results delivered through typed callbacks.
 *
 * Auth: the Firebase Bearer token is injected by FirebaseAuthInterceptor and a single 401
 * refresh-and-retry is performed by FirebaseTokenAuthenticator at the OkHttp layer, so no token
 * handling is needed here. A persistent 401/403 surfaces as an error message to the ViewModel.
 */
public class AnalyticsRepository {

    public interface StudentCallback {
        void onSuccess(StudentAnalyticsSummary summary);
        void onError(String message);
    }

    public interface TeacherCallback {
        void onSuccess(TeacherAnalytics analytics);
        void onError(String message);
    }

    public interface PlatformCallback {
        void onSuccess(PlatformAnalytics analytics);
        void onError(String message);
    }

    public interface StudentTrendCallback {
        void onSuccess(StudentProgressTrend trend);
        void onError(String message);
    }

    public interface PlatformCohortCallback {
        void onSuccess(PlatformCohortAnalytics analytics);
        void onError(String message);
    }

    public interface StudyAnalyticsCallback {
        void onSuccess(StudyAnalytics analytics);
        void onError(String message);
    }

    private final ApiService apiService;

    public AnalyticsRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    /** Loads the caller's own student summary. */
    public void loadStudentSummary(StudentCallback callback) {
        apiService.getMyAnalyticsSummary().enqueue(new Callback<StudentAnalyticsSummary>() {
            @Override
            public void onResponse(@NonNull Call<StudentAnalyticsSummary> call,
                                   @NonNull Response<StudentAnalyticsSummary> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load your stats. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<StudentAnalyticsSummary> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads the caller's owned-course analytics (server scopes to the teacher's own courses). */
    public void loadTeacherAnalytics(TeacherCallback callback) {
        apiService.getTeacherAnalytics().enqueue(new Callback<TeacherAnalytics>() {
            @Override
            public void onResponse(@NonNull Call<TeacherAnalytics> call,
                                   @NonNull Response<TeacherAnalytics> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load course analytics. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<TeacherAnalytics> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads global platform analytics (ADMIN only, enforced server-side). */
    public void loadPlatformAnalytics(PlatformCallback callback) {
        apiService.getPlatformAnalytics().enqueue(new Callback<PlatformAnalytics>() {
            @Override
            public void onResponse(@NonNull Call<PlatformAnalytics> call,
                                   @NonNull Response<PlatformAnalytics> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load platform analytics. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<PlatformAnalytics> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads the caller's own lessons-completed-per-month trend. */
    public void loadStudentTrend(StudentTrendCallback callback) {
        apiService.getMyProgressTrend().enqueue(new Callback<StudentProgressTrend>() {
            @Override
            public void onResponse(@NonNull Call<StudentProgressTrend> call,
                                   @NonNull Response<StudentProgressTrend> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load progress trend. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<StudentProgressTrend> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /** Loads global cohort analytics (ADMIN only, enforced server-side). */
    public void loadPlatformCohorts(PlatformCohortCallback callback) {
        apiService.getPlatformCohorts().enqueue(new Callback<PlatformCohortAnalytics>() {
            @Override
            public void onResponse(@NonNull Call<PlatformCohortAnalytics> call,
                                   @NonNull Response<PlatformCohortAnalytics> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load cohort analytics. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<PlatformCohortAnalytics> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    // ── Study Analytics screen ──────────────────────────────────────────────
    // Every section on the redesigned learner screen is sourced from the real backend:
    //   • totals + exam avg/best          → GET /analytics/me/summary
    //   • per-course progress + weekly    → GET /enrollments/me + GET /progress/courses/{id}
    //   • streak                          → GET /gamification/me
    // The screen still renders on partial failures: only a hard /summary failure surfaces an error
    // (it is what populates the hero block). Enrollments and gamification failures degrade to empty
    // sections rather than blocking the whole view.

    /**
     * Loads the aggregate Study Analytics model. Fires summary + enrollments + gamification in
     * parallel, then per-course progress for each enrolled course, then assembles a single
     * {@link StudyAnalytics} for the UI.
     */
    public void loadStudyAnalytics(StudyAnalyticsCallback callback) {
        AtomicReference<StudentAnalyticsSummary> summaryRef = new AtomicReference<>();
        AtomicReference<List<EnrolledCourse>> enrollmentsRef =
                new AtomicReference<>(Collections.emptyList());
        AtomicReference<GamificationStateResponse> gamificationRef = new AtomicReference<>();
        AtomicReference<String> hardError = new AtomicReference<>();
        AtomicInteger pending = new AtomicInteger(3);

        Runnable onOneDone = () -> {
            if (pending.decrementAndGet() != 0) return;
            if (hardError.get() != null) {
                callback.onError(hardError.get());
                return;
            }
            loadProgressAndDeliver(
                    summaryRef.get(), enrollmentsRef.get(), gamificationRef.get(), callback);
        };

        apiService.getMyAnalyticsSummary().enqueue(new Callback<StudentAnalyticsSummary>() {
            @Override
            public void onResponse(@NonNull Call<StudentAnalyticsSummary> call,
                                   @NonNull Response<StudentAnalyticsSummary> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    hardError.compareAndSet(null, "Failed to load your stats. Status: "
                            + response.code());
                } else {
                    summaryRef.set(response.body());
                }
                onOneDone.run();
            }

            @Override
            public void onFailure(@NonNull Call<StudentAnalyticsSummary> call,
                                  @NonNull Throwable t) {
                hardError.compareAndSet(null,
                        t.getMessage() != null ? t.getMessage() : "Network error");
                onOneDone.run();
            }
        });

        apiService.getMyEnrollments().enqueue(new Callback<List<EnrolledCourse>>() {
            @Override
            public void onResponse(@NonNull Call<List<EnrolledCourse>> call,
                                   @NonNull Response<List<EnrolledCourse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    enrollmentsRef.set(response.body());
                }
                onOneDone.run();
            }

            @Override
            public void onFailure(@NonNull Call<List<EnrolledCourse>> call,
                                  @NonNull Throwable t) {
                onOneDone.run();
            }
        });

        apiService.getMyGamificationState().enqueue(new Callback<GamificationStateResponse>() {
            @Override
            public void onResponse(@NonNull Call<GamificationStateResponse> call,
                                   @NonNull Response<GamificationStateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    gamificationRef.set(response.body());
                }
                onOneDone.run();
            }

            @Override
            public void onFailure(@NonNull Call<GamificationStateResponse> call,
                                  @NonNull Throwable t) {
                onOneDone.run();
            }
        });
    }

    /** Fans out a /progress/courses/{id} call per enrolled course, then assembles the model. */
    private void loadProgressAndDeliver(StudentAnalyticsSummary summary,
                                        List<EnrolledCourse> enrollments,
                                        @Nullable GamificationStateResponse gamification,
                                        StudyAnalyticsCallback callback) {
        if (enrollments.isEmpty()) {
            callback.onSuccess(buildStudyAnalytics(
                    summary, enrollments, Collections.emptyMap(), gamification));
            return;
        }

        Map<String, CourseProgressSummary> byCourse =
                Collections.synchronizedMap(new HashMap<>(enrollments.size()));
        AtomicInteger left = new AtomicInteger(enrollments.size());

        for (EnrolledCourse e : enrollments) {
            final String courseId = e.courseId;
            apiService.getCourseProgress(courseId).enqueue(new Callback<CourseProgressSummary>() {
                @Override
                public void onResponse(@NonNull Call<CourseProgressSummary> call,
                                       @NonNull Response<CourseProgressSummary> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        byCourse.put(courseId, response.body());
                    }
                    if (left.decrementAndGet() == 0) {
                        callback.onSuccess(buildStudyAnalytics(
                                summary, enrollments, byCourse, gamification));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CourseProgressSummary> call,
                                      @NonNull Throwable t) {
                    if (left.decrementAndGet() == 0) {
                        callback.onSuccess(buildStudyAnalytics(
                                summary, enrollments, byCourse, gamification));
                    }
                }
            });
        }
    }

    /** Assembles the final {@link StudyAnalytics} from the four backend responses. */
    private StudyAnalytics buildStudyAnalytics(StudentAnalyticsSummary s,
                                               List<EnrolledCourse> enrollments,
                                               Map<String, CourseProgressSummary> byCourse,
                                               @Nullable GamificationStateResponse gamification) {

        int overall = 0;
        int totalCompleted = 0;
        int totalLessons = 0;
        for (EnrolledCourse e : enrollments) {
            CourseProgressSummary p = byCourse.get(e.courseId);
            if (p == null) continue;
            totalCompleted += p.completedLessons;
            totalLessons += p.totalLessons;
        }
        if (totalLessons > 0) {
            overall = Math.max(0, Math.min(100,
                    Math.round((totalCompleted * 100f) / totalLessons)));
        }

        List<CourseProgressAnalytics> courses = new ArrayList<>(enrollments.size());
        for (EnrolledCourse e : enrollments) {
            CourseProgressSummary p = byCourse.get(e.courseId);
            int completed = p != null ? p.completedLessons : 0;
            int total = p != null ? p.totalLessons : 0;
            courses.add(new CourseProgressAnalytics(
                    e.title, completed, total, lastActivityLabel(p, e.enrolledAt)));
        }

        WeeklyStudyActivity weekly = buildWeekly(byCourse.values());

        ExamPerformanceSummary exam = new ExamPerformanceSummary(
                s.averageExamScore, (int) s.examsPassed, s.bestExamScore);

        int currentStreak = gamification != null ? gamification.currentStreak : 0;
        int longestStreak = gamification != null ? gamification.longestStreak : 0;
        LearningStreakSummary streak = new LearningStreakSummary(
                currentStreak, longestStreak, weekly.daysStudiedThisWeek);

        List<AnalyticsInsight> insights = buildInsights(courses, weekly);

        return new StudyAnalytics(
                overall,
                "Current learning path",
                s.lessonsCompleted,
                s.activeEnrollments,
                s.certificatesEarned,
                weekly,
                courses,
                exam,
                streak,
                insights);
    }

    // Buckets every completed-lesson timestamp into the last seven days, Mon→Sun.
    private WeeklyStudyActivity buildWeekly(Iterable<CourseProgressSummary> progresses) {
        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int[] counts = new int[7];

        LocalDate today = LocalDate.now();
        int todayIdx = today.getDayOfWeek().getValue() - 1; // Mon=1 → 0 … Sun=7 → 6
        LocalDate windowStart = today.minusDays(6);

        for (CourseProgressSummary p : progresses) {
            if (p == null || p.sections == null) continue;
            for (CourseProgressSummary.SectionProgressSummary sec : p.sections) {
                if (sec == null || sec.lessons == null) continue;
                for (CourseProgressSummary.LessonProgressSummary lesson : sec.lessons) {
                    if (lesson == null || !lesson.completed || lesson.completedAt == null) continue;
                    LocalDate day = parseDate(lesson.completedAt);
                    if (day == null) continue;
                    if (day.isBefore(windowStart) || day.isAfter(today)) continue;
                    counts[day.getDayOfWeek().getValue() - 1]++;
                }
            }
        }

        List<DayStudyActivity> days = new ArrayList<>(7);
        int total = 0, studied = 0;
        for (int i = 0; i < 7; i++) {
            days.add(new DayStudyActivity(labels[i], counts[i], i == todayIdx));
            total += counts[i];
            if (counts[i] > 0) studied++;
        }
        return new WeeklyStudyActivity(days, total, studied);
    }

    private static String lastActivityLabel(@Nullable CourseProgressSummary progress,
                                            @Nullable String enrolledAt) {
        Instant latest = latestCompletion(progress);
        if (latest != null) return relativeDays(latest);
        if (enrolledAt == null) return "Not started";
        Instant en = parseInstant(enrolledAt);
        return en != null ? "Enrolled " + relativeDays(en).toLowerCase() : "Not started";
    }

    private static @Nullable Instant latestCompletion(@Nullable CourseProgressSummary p) {
        if (p == null || p.sections == null) return null;
        Instant latest = null;
        for (CourseProgressSummary.SectionProgressSummary sec : p.sections) {
            if (sec == null || sec.lessons == null) continue;
            for (CourseProgressSummary.LessonProgressSummary lesson : sec.lessons) {
                if (lesson == null || !lesson.completed || lesson.completedAt == null) continue;
                Instant t = parseInstant(lesson.completedAt);
                if (t == null) continue;
                if (latest == null || t.isAfter(latest)) latest = t;
            }
        }
        return latest;
    }

    private static String relativeDays(Instant when) {
        LocalDate then = when.atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(then, LocalDate.now());
        if (days <= 0) return "Today";
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        if (days < 14) return "Last week";
        if (days < 30) return (days / 7) + " weeks ago";
        return "Over a month ago";
    }

    private static @Nullable LocalDate parseDate(String iso) {
        Instant i = parseInstant(iso);
        return i == null ? null : i.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // Lesson timestamps come back as offset-date-time; older rows may serialise as plain Instant.
    private static @Nullable Instant parseInstant(String iso) {
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.parse(iso);
            } catch (DateTimeParseException ignored2) {
                return null;
            }
        }
    }

    /**
     * Derives a few honest insight lines from the real data. Kept simple on purpose — no analytics
     * engine, no predictions.
     */
    private List<AnalyticsInsight> buildInsights(List<CourseProgressAnalytics> courses,
                                                 WeeklyStudyActivity weekly) {
        List<AnalyticsInsight> insights = new ArrayList<>();

        CourseProgressAnalytics best = null;
        for (CourseProgressAnalytics c : courses) {
            int pct = c.progressPercent();
            if (pct < 100 && (best == null || pct > best.progressPercent())) best = c;
        }
        if (best != null && best.progressPercent() >= 60) {
            insights.add(new AnalyticsInsight(
                    "You're close to finishing " + best.courseTitle + " — keep going!"));
        }

        DayStudyActivity peak = null;
        for (DayStudyActivity d : weekly.days) {
            if (peak == null || d.lessonsCompleted > peak.lessonsCompleted) peak = d;
        }
        if (peak != null && peak.lessonsCompleted > 0) {
            insights.add(new AnalyticsInsight("You study best on " + peak.label + "."));
        }

        CourseProgressAnalytics next = null;
        for (CourseProgressAnalytics c : courses) {
            int remaining = c.totalLessons - c.lessonsCompleted;
            if (remaining > 0 && (next == null
                    || remaining < (next.totalLessons - next.lessonsCompleted))) {
                next = c;
            }
        }
        if (next != null) {
            int remaining = next.totalLessons - next.lessonsCompleted;
            if (remaining <= 3) {
                insights.add(new AnalyticsInsight(
                        "Complete " + remaining + " more lesson" + (remaining == 1 ? "" : "s")
                                + " to finish " + next.courseTitle + "."));
            }
        }

        if (insights.isEmpty()) {
            insights.add(new AnalyticsInsight(
                    "Start a lesson today to begin building your streak."));
        }
        return insights;
    }
}
