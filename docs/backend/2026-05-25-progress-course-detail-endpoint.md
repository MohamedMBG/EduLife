# Task Audit - P3: GET /api/v1/progress/courses/{courseId}

## Date
2026-05-25

## GitHub
- Issue: #254
- PR: #297 — feat(progress): GET /api/v1/progress/courses/{courseId} with per-lesson completedAt
- Branch: feat/progress-course-detail-endpoint

## Task Summary
Implemented the course progress detail endpoint for authenticated learners. Returns full progress breakdown by section and lesson, including per-lesson completion timestamps. Fixes DTO contract mismatches from the initial progress module scaffold.

---

## Files Created

### Backend
- `backend/src/main/java/com/edulife/progress/controller/ProgressQueryController.java`
  - `GET /api/v1/progress/courses/{courseId}` — returns `CourseProgressDto`
  - 403 if user is not actively enrolled
  - Auth required (Firebase token)

---

## Files Modified

### `progress/dto/CourseProgressDto.java`
| Field | Before | After |
|-------|--------|-------|
| `percentageComplete` (int) | old | removed |
| `percentComplete` (double) | — | added — supports values like `33.3`, `50.0` |
| `SectionProgressDto.sectionTitle` | old | renamed to `title` |
| `LessonProgressDto.completedAt` | missing | added as `Instant` (null when incomplete) |

### `progress/repository/LessonProgressRepository.java`
- Removed `@Query findCompletedLessonIdsByUserIdAndCourseId` returning `Set<UUID>`
- Added derived query `findAllByUserIdAndCourseId(UUID userId, UUID courseId)` returning `List<LessonProgress>`
- Returns full entity objects so `completedAt` is available in one query, no N+1

### `progress/service/ProgressService.java`
- `getCourseProgress`: replaced `Set<UUID> completedIds` with `Map<UUID, LessonProgress> completedMap`
- Each lesson now receives `lp.getCompletedAt()` when completed, `null` otherwise
- `percentComplete` computed as `Math.round((completed * 1000.0) / total) / 10.0` — one decimal precision
- Zero-lesson course returns `0.0` safely (no division by zero)

---

## API Contract

```
GET /api/v1/progress/courses/{courseId}
Authorization: Bearer <firebase-token>

200 OK
{
  "courseId": "uuid",
  "completedLessons": 2,
  "totalLessons": 8,
  "percentComplete": 25.0,
  "sections": [
    {
      "sectionId": "uuid",
      "title": "Section title",
      "displayOrder": 1,
      "lessons": [
        {
          "lessonId": "uuid",
          "title": "Lesson title",
          "lessonType": "VIDEO",
          "durationMinutes": 12,
          "displayOrder": 1,
          "preview": false,
          "completed": true,
          "completedAt": "2026-05-25T10:00:00Z"
        }
      ]
    }
  ]
}

403 Forbidden — user not actively enrolled
401 Unauthorized — missing or invalid Firebase token
```

---

## Design decisions
- `GET /api/v1/courses/{courseId}/progress` (on `ProgressController`) kept intact — existing route not broken.
- `ProgressQueryController` lives at `/api/v1/progress` to match issue contract URL.
- `completedAt` is `null` in JSON for incomplete lessons (Jackson default behavior).
- Single `findAllByUserIdAndCourseId` DB call for all completion records; join in-memory — avoids per-lesson query.
