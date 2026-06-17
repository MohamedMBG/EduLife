import type { CourseSummary } from "../api/types";

export interface CourseRecommendation {
  course: CourseSummary;
  score: number;
  reason: string;
  matchedSkills?: string[];
  source?: "groq" | "deterministic-fallback";
}

export interface AdvisorResult {
  message: string;
  recommendations: CourseRecommendation[];
}

const MAX_RECOMMENDATIONS = 2;

const STOP_WORDS = new Set([
  "a",
  "an",
  "and",
  "are",
  "be",
  "become",
  "for",
  "from",
  "i",
  "in",
  "it",
  "learn",
  "me",
  "my",
  "of",
  "on",
  "or",
  "the",
  "to",
  "want",
  "with",
]);

const CAREER_SIGNALS = new Map<string, string[]>([
  ["developer", ["digital", "productivity", "skills", "study", "web", "portfolio"]],
  ["programmer", ["digital", "productivity", "skills", "study", "web", "portfolio"]],
  ["software", ["digital", "productivity", "skills", "study", "web", "portfolio"]],
  ["app", ["digital", "productivity", "skills", "study", "web"]],
  ["web", ["digital", "productivity", "skills", "study", "web"]],
  ["computer", ["digital", "productivity", "skills", "study"]],
  ["technology", ["digital", "productivity", "skills", "study"]],
  ["english", ["english", "communication", "reading", "listening", "career"]],
  ["communication", ["english", "communication", "reading", "listening", "career"]],
  ["international", ["english", "communication", "reading", "listening", "career"]],
  ["tourism", ["english", "communication", "reading", "listening", "career"]],
  ["french", ["french", "expression", "writing", "revision"]],
  ["francais", ["french", "expression", "writing", "revision"]],
  ["writing", ["french", "expression", "writing", "revision"]],
  ["engineer", ["math", "algebra", "sciences", "bac", "physics"]],
  ["engineering", ["math", "algebra", "sciences", "bac", "physics"]],
  ["data", ["math", "algebra", "sciences", "bac", "digital"]],
  ["math", ["math", "algebra", "sciences", "bac"]],
  ["mechanical", ["physics", "motion", "forces", "mechanics"]],
  ["physics", ["physics", "motion", "forces", "mechanics"]],
  ["robotics", ["physics", "motion", "forces", "mechanics", "digital"]],
  ["designer", ["ui", "design", "interface", "clarity", "student apps"]],
  ["design", ["ui", "design", "interface", "clarity", "student apps"]],
  ["business", ["productivity", "communication", "planning", "career"]],
  ["entrepreneur", ["productivity", "communication", "planning", "career"]],
]);

export function analyzeCareerGoal(goal: string, courses: CourseSummary[]): AdvisorResult {
  const trimmedGoal = goal.trim();

  if (trimmedGoal.length < 4) {
    return {
      message: "Write a clearer career goal first. Mention the career, school subject, or skill you want to build.",
      recommendations: [],
    };
  }

  if (courses.length === 0) {
    return {
      message: "I checked the catalog, but there are no published courses to compare right now.",
      recommendations: [],
    };
  }

  const recommendations = rankCourses(trimmedGoal, courses);

  return {
    message: buildAdvisorMessage(trimmedGoal, recommendations),
    recommendations,
  };
}

function rankCourses(goal: string, courses: CourseSummary[]) {
  const goalTokens = tokenize(goal);
  const expandedSignals = expandCareerSignals(goalTokens);
  const ranked: CourseRecommendation[] = [];

  for (const course of courses) {
    const scored = scoreCourse(goalTokens, expandedSignals, course);

    if (scored.score > 0) {
      ranked.push({ course, score: scored.score, reason: scored.reason });
    }
  }

  if (ranked.length === 0) {
    for (const course of courses) {
      ranked.push({
        course,
        score: course.level?.toUpperCase() === "BEGINNER" ? 8 : 4,
        reason:
          "This is the closest structured starting point in the current catalog. Use it to build momentum while EduLife adds more exact career paths.",
      });
    }
  }

  ranked.sort((left, right) => right.score - left.score);
  return strongestOneOrTwo(ranked);
}

function scoreCourse(
  goalTokens: Set<string>,
  expandedSignals: Set<string>,
  course: CourseSummary,
) {
  const courseText = normalize(
    `${course.title} ${course.shortDescription} ${course.level} ${course.languageCode}`,
  );
  const courseTokens = tokenize(courseText);
  const matched = new Set<string>();
  let score = 0;

  for (const token of goalTokens) {
    if (courseTokens.has(token)) {
      score += 12;
      matched.add(token);
    }
  }

  for (const signal of expandedSignals) {
    if (courseText.includes(signal)) {
      score += 8;
      matched.add(signal);
    }
  }

  if (
    course.level?.toUpperCase() === "BEGINNER" &&
    containsAny(goalTokens, ["start", "beginner", "new", "first"])
  ) {
    // Learners asking where to start need a first step, not the hardest keyword match.
    score += 6;
    matched.add("beginner level");
  }

  if (
    course.languageCode?.toLowerCase() === "en" &&
    containsAny(goalTokens, ["english", "international", "global"])
  ) {
    score += 6;
    matched.add("English");
  }

  if (
    course.languageCode?.toLowerCase() === "fr" &&
    containsAny(goalTokens, ["french", "francais", "morocco", "maroc"])
  ) {
    score += 6;
    matched.add("French");
  }

  if (
    course.languageCode?.toLowerCase() === "darija" &&
    containsAny(goalTokens, ["darija", "morocco", "maroc", "local"])
  ) {
    score += 6;
    matched.add("Darija");
  }

  return {
    score,
    reason: buildReason(matched, course),
  };
}

function buildAdvisorMessage(goal: string, ranked: CourseRecommendation[]) {
  if (ranked.length === 0) {
    return `I checked the current catalog, but I could not find a useful course match for "${goal}". Try naming the career or skill more directly.`;
  }

  const best = ranked[0];
  let message = `I checked the current EduLife courses against your goal: "${goal}".\n\n`;
  message += `Start with "${best.course.title}". ${best.reason}`;

  if (ranked.length > 1) {
    const second = ranked[1];
    message += `\n\nAfter that, consider "${second.course.title}" as a supporting course. It is not the main path, but it can strengthen a related skill.`;
  } else {
    message += "\n\nI am showing one course because it is the clearest match from the current catalog.";
  }

  return message;
}

function buildReason(matched: Set<string>, course: CourseSummary) {
  if (matched.size === 0) {
    return "It gives you a structured path instead of leaving you to browse randomly.";
  }

  const signals = Array.from(matched).slice(0, 3).join(", ");
  const level = course.level?.toLowerCase() || "current";

  return `I picked it because your goal connects with ${signals}. The course is ${level}, so the next step is realistic: open the outline, check the lessons, then enroll if the level feels right.`;
}

function strongestOneOrTwo(ranked: CourseRecommendation[]) {
  if (ranked.length <= 1) {
    return ranked;
  }

  const result = [ranked[0]];

  // A second result is useful only when it is close enough to support the first choice.
  if (ranked[1].score >= Math.max(8, Math.floor(ranked[0].score / 2))) {
    result.push(ranked[1]);
  }

  return result.slice(0, MAX_RECOMMENDATIONS);
}

function tokenize(text: string) {
  const tokens = new Set<string>();

  for (const part of normalize(text).split(/[^a-z0-9]+/)) {
    if (part.length >= 3 && !STOP_WORDS.has(part)) {
      tokens.add(part);
    }
  }

  return tokens;
}

function expandCareerSignals(goalTokens: Set<string>) {
  const signals = new Set<string>();

  for (const token of goalTokens) {
    for (const signal of CAREER_SIGNALS.get(token) ?? []) {
      signals.add(signal);
    }
  }

  return signals;
}

function containsAny(tokens: Set<string>, values: string[]) {
  return values.some((value) => tokens.has(value));
}

function normalize(value: string) {
  return value
    .toLowerCase()
    .replace(/\u00e9/g, "e")
    .replace(/\u00e8/g, "e")
    .replace(/\u00ea/g, "e")
    .replace(/\u00e0/g, "a")
    .replace(/\u00e7/g, "c");
}
