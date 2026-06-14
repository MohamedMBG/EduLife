# Task Audit - Career Goal Advisor

## Date
2026-06-13

## Task Summary
Added an MVP-compatible learner career goal advisor in the Android app. The learner can describe what they want to achieve, the app compares that goal against the currently published course catalog, and it recommends the best matching courses with reasons.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/courses/model/CareerAdvisorUiState.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CareerCourseRecommendation.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CareerAdvisorViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CareerAdvisorFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CareerRecommendationAdapter.java
- app/src/main/res/layout/fragment_career_advisor.xml
- app/src/main/res/layout/item_career_recommendation.xml
- docs/2026-06-13-career-goal-advisor.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/features/courses/data/CourseRepository.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java
- app/src/main/res/layout/fragment_home.xml
- app/src/main/res/navigation/nav_graph.xml
- app/src/main/res/values/strings.xml

## What Was Done
Implemented a new Career Goal Advisor screen under the existing Android `features/courses` area. The screen accepts a free-form learner goal, displays a chat-style assistant response, ranks published courses from the existing catalog endpoint, and shows the top three recommendations with match reasons.

The ranking is rule-based for MVP discipline. It tokenizes the learner goal, expands simple career signals such as software, English, French, engineering, math, and physics, compares them against course title, description, level, and language, then routes the learner to the existing course detail screen.

Added a Home dashboard entry point so learners can start the advisor before applying catalog filters.

## Architecture Compliance
The work stays inside the Android course discovery feature because the advisor helps learners choose a course from the catalog. It follows the existing Java/XML MVVM pattern:

- UI logic: `features/courses/ui/`
- State and matching logic: `features/courses/viewmodel/`
- API access: `features/courses/data/`
- Data models: `features/courses/model/`

No backend microservice, AI assistant with memory, personalized recommendation engine, payments, CMS work, or deferred MVP module was added. The advisor reuses the existing published course API and course detail navigation.

## Code Comments Added
Added comments explaining:

- Why the advisor loads the full published catalog instead of the active Home filter.
- Why the ranking is local rule-based MVP logic.
- Why beginner-level weighting exists for "where do I start" goals.
- Why the advisor opens the existing course detail screen instead of creating a separate enrollment path.
- Why the Home CTA belongs before enrollment as a discovery helper.

## Validation / Testing
Ran:

```text
.\gradlew.bat :app:compileDebugJavaWithJavac
```

Result: build successful.

## Risks / Notes
The advisor currently evaluates the first 50 published courses, which is enough for the current small catalog but should become a backend-supported recommendation/search endpoint if the catalog grows. It is intentionally not an AI assistant or persistent recommendation engine because those are excluded from the current MVP execution path.
