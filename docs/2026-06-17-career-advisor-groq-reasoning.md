# Career Advisor: Groq Reasoning Pipeline Fix

## Goal

Fix Career Advisor giving bad recommendations (e.g., recommending "Science Math Bac" for "I want to make Android apps") by implementing a proper recommendation pipeline combining deterministic matching with Groq LLM reranking.

## Root Cause

1. **No pre-filtering**: All published courses sent to Groq with equal weight — LLM had no signal about which domain matched
2. **Weak system prompt**: No emphasis on direct domain match over general academic relevance
3. **Empty tags**: `CourseContextBuilder` always sent `List.of()` for tags
4. **Missing context**: Only `shortDescription` sent — full `description` and lesson titles omitted
5. **Score always 0.0**: No confidence scoring from LLM result
6. **Fallback returned nothing**: On Groq failure, response had empty recommendations
7. **No intent expansion**: "android apps" not expanded to related terms like "mobile", "kotlin", etc.

## What Changed

### New Backend Services

- **`IntentExtractor`**: Normalizes user goal, tokenizes, expands keywords via synonym groups (Android/mobile, web, AI/data, bac/math, etc.), detects language (English/French/Darija)
- **`DeterministicRanker`**: Weighted scoring of courses against intent — title match (15pts), tag match (12pts), description match (6pts), lesson title match (6pts), language (4pts), level (3pts). Produces ranked shortlist (top 5)

### New Recommendation Pipeline (AdvisorService)

```
User Goal
  → IntentExtractor (keyword extraction + synonym expansion + language detection)
  → CourseContextBuilder (full catalog with descriptions + lesson titles)
  → DeterministicRanker (weighted shortlist of top 5)
  → GroqLlmClient (reranks shortlist only, not full catalog)
  → Validation (real course IDs, capped confidence, real matchedSkills)
  → If Groq fails → deterministic fallback with scored recommendations
```

### Enriched Course Context

`CourseContextDto` now includes `description` (full text) and `lessonTitles` (fetched via sections → lessons). This gives both the deterministic ranker and Groq richer matching signals.

### Improved Groq Prompt

- Explicit rule: "PRIORITIZE DIRECT DOMAIN MATCH over general academic relevance"
- Examples: "If learner asks about Android apps, pick Android course, NOT math"
- JSON response format with `confidence` (0.0-1.0) and `matchedSkills`
- Temperature lowered to 0.2 for consistency
- `response_format: json_object` for structured output

### Response DTO Changes (backward-compatible)

- `AdvisorRecommendationDto`: added `matchedSkills: List<String>`
- `AdvisorResponse`: added `source: String` ("groq" or "deterministic-fallback")
- Score now reflects actual LLM confidence or normalized deterministic score

### Configuration

- `GROQ_BASE_URL` env var (default: `https://api.groq.com/openai/v1`)
- `GROQ_MODEL` default: `llama-3.3-70b-versatile`
- Removed hardcoded API key from `application.yaml`

## Files Touched

### Backend — New
- `backend/src/main/java/com/edulife/advisor/service/IntentExtractor.java`
- `backend/src/main/java/com/edulife/advisor/service/DeterministicRanker.java`
- `backend/src/test/java/com/edulife/advisor/DeterministicRankerTest.java`

### Backend — Modified
- `backend/src/main/java/com/edulife/advisor/service/AdvisorService.java`
- `backend/src/main/java/com/edulife/advisor/service/CourseContextBuilder.java`
- `backend/src/main/java/com/edulife/advisor/client/GroqLlmClient.java`
- `backend/src/main/java/com/edulife/advisor/dto/CourseContextDto.java`
- `backend/src/main/java/com/edulife/advisor/dto/AdvisorLlmResult.java`
- `backend/src/main/java/com/edulife/advisor/dto/AdvisorRecommendationDto.java`
- `backend/src/main/java/com/edulife/advisor/dto/AdvisorResponse.java`
- `backend/src/main/java/com/edulife/advisor/config/AdvisorProperties.java`
- `backend/src/main/resources/application.yaml`
- `backend/src/test/java/com/edulife/advisor/AdvisorServiceTest.java`
- `backend/src/test/java/com/edulife/advisor/CourseContextBuilderTest.java`
- `backend/src/test/java/com/edulife/advisor/AdvisorPropertiesTest.java`

### Web — Modified
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/career/advisor.ts`
- `guided-journey-lab/src/routes/advisor.tsx`

### Android — Modified
- `app/src/main/java/com/baghdad/edulife/features/advisor/model/AdvisorRecommendation.java`
- `app/src/main/java/com/baghdad/edulife/features/advisor/model/AdvisorResponse.java`

## Backend Impact

- API contract extended (new fields added, no fields removed — backward-compatible)
- Recommendation quality significantly improved for domain-specific queries
- Groq failures now produce useful deterministic recommendations instead of empty results
- Hardcoded API key removed from application.yaml

## Android Impact

- Model classes updated with optional `matchedSkills` and `source` fields
- Gson/Retrofit deserialization handles new fields transparently
- No UI changes needed — existing display works with improved data

## Web Impact

- API response type updated with `matchedSkills` and `source`
- Matched skills displayed as pills in analysis section
- Source indicator shows "AI-powered" vs "keyword analysis" in footer
- TypeScript compiles clean

## Architecture Compliance

- Business logic in service layer
- DTOs for API I/O
- No entities exposed
- Security: Groq API key backend-only, no learner PII sent to Groq
- Backward-compatible DTO changes

## Tests / Verification

- 42 advisor tests pass (backend)
- TypeScript typecheck passes (web)
- Key test cases:
  - "I want to make Android apps" → Android course ranked first
  - "I wanna build mobile apps" → Android course ranked first
  - "je veux créer des applications android" → Android course ranked first
  - "bghit ndir app android" → Android course ranked first
  - "I need help with Bac math algebra" → Math course ranked first
  - "I want to become a web developer" → Web course ranked first
  - Groq failure → deterministic fallback with real recommendations
  - Fake course IDs dropped
  - Picks capped at 2

## Fallback Behavior

If Groq is unavailable (network error, 401, 429, 500, timeout, parse failure):
1. Log warning with error message
2. Use deterministic ranking from IntentExtractor + DeterministicRanker
3. Return top 1-2 courses with `source: "deterministic-fallback"`
4. Never return empty recommendations if courses exist

## Risks / Notes

- Synonym groups are hardcoded — adding new course domains requires code change
- Course tags field still empty (`List.of()`) — when tags are added to courses, ranking quality improves further
- Lesson title fetching adds N+1 queries per course (sections → lessons) — acceptable for small catalogs, may need optimization for 100+ courses
- Darija detection is keyword-based — may miss some expressions
