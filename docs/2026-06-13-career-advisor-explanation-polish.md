# Task Audit - Career Advisor Explanation Polish

## Date
2026-06-13

## Task Summary
Improved the learner Career Goal Advisor so it gives fewer, clearer recommendations and explains the result in a more conversational way.

## Files Created
- docs/2026-06-13-career-advisor-explanation-polish.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CareerAdvisorViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CareerRecommendationAdapter.java
- app/src/main/res/layout/item_career_recommendation.xml
- app/src/main/res/values/strings.xml

## What Was Done
Changed the advisor from showing up to three recommendations into a focused result that shows one course by default and a second course only when the second match is useful enough.

The assistant response now explains that it checked the current EduLife catalog against the learner goal, names the best course, explains why it was chosen, and clarifies whether a second course is a supporting option.

The recommendation cards now use learner-facing labels such as "Best", "Next", "Start here", and "Second" instead of exposing raw match scores.

## Architecture Compliance
The change stays inside the Android `features/courses` area because the advisor is part of course discovery. It preserves the existing MVVM split:

- Ranking and explanation logic remains in `CareerAdvisorViewModel`.
- Card rendering remains in `CareerRecommendationAdapter`.
- Text and layout changes remain in Android resources.

No backend recommendation engine, AI assistant, microservice, or deferred MVP feature was introduced.

## Code Comments Added
Added comments explaining why raw scoring is hidden from learners and why the advisor only shows a second result when it is close enough to be useful.

## Validation / Testing
Ran:

```text
.\gradlew.bat :app:compileDebugJavaWithJavac
```

Result: build successful.

## Risks / Notes
The matching is still deterministic and rule-based. It is better for the current small MVP catalog, but a larger catalog should eventually move this ranking to a backend-supported search or recommendation endpoint.
