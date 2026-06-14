# Task Audit - Web And Mobile Career Advisor

## Date
2026-06-13

## Task Summary
Improved the Career Goal Advisor and added the same learner-facing advisor experience to the web version.

## Files Created
- guided-journey-lab/src/lib/career/advisor.ts
- guided-journey-lab/src/routes/advisor.tsx
- docs/2026-06-13-web-and-mobile-career-advisor.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CareerAdvisorViewModel.java
- guided-journey-lab/src/components/app/AppShell.tsx
- guided-journey-lab/src/routes/dashboard.tsx
- guided-journey-lab/src/routeTree.gen.ts

## What Was Done
Added a new authenticated web route at `/advisor`. The web advisor lets a learner enter a career or skill goal, fetches the live published course catalog through the existing `listCourses` API client, analyzes the goal locally, and returns one clear best course with a second course only when it is useful.

Added the web advisor to the learner sidebar navigation and added a dashboard entry card so learners can discover it before browsing courses manually.

Improved the Android scorer with additional career signals for portfolio, design, interface, business, and entrepreneurship goals. Android explanations now tell the learner what to do next: open the outline, check lessons, and enroll if the level fits.

## Architecture Compliance
The work remains inside the existing Android `features/courses` area and the web learner routes/API layer. It reuses existing course catalog APIs and existing course detail/enrollment paths.

No backend microservice, advanced AI assistant with memory, persistent recommendation engine, payment flow, or deferred MVP module was introduced. The advisor remains an MVP-safe course discovery helper.

## Code Comments Added
Added comments explaining why the recommendation engine shows a second web result only when it supports the first choice, and why Android keeps beginner-first behavior for learners asking where to start.

## Validation / Testing
Ran Android compile:

```text
.\gradlew.bat :app:compileDebugJavaWithJavac
```

Result: build successful.

Ran web production build:

```text
npm run build
```

Result: build successful. Vite reported an existing CSS `@import` ordering warning, but no build failure.

Started the web dev server on:

```text
http://127.0.0.1:5174
```

Verified:

```text
GET http://127.0.0.1:5174/advisor -> 200
```

The in-app browser backend was unavailable (`iab` not available), so visual browser automation could not be completed in this session.

## Risks / Notes
The advisor is still deterministic and rule-based. This is appropriate for the current MVP catalog. If EduLife grows to a large catalog, the ranking should move to a backend-supported search or recommendation endpoint with tests around scoring and explainability.
