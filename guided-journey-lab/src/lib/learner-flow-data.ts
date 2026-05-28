export type CourseStatus = "in-progress" | "completed" | "not-started";

export interface LearnerCourseCard {
  id: number;
  title: string;
  subject: string;
  subjectIcon: "code" | "language" | "business" | "design";
  instructor: string;
  instructorInitials: string;
  instructorColor: string;
  thumbnail: string;
  lessons: number;
  total: number;
  nextLesson: string;
  estimatedMin: number;
  xpEarned: number;
  xpTotal: number;
  status: CourseStatus;
  rating: number;
  gradientFrom: string;
  gradientTo: string;
  accentText: string;
  accentBg: string;
  accentBorder: string;
  duration: string;
}

export interface LearnerLesson {
  id: string;
  title: string;
  duration: string;
  completed: boolean;
  locked: boolean;
  type: "video" | "reading" | "quiz";
}

export interface LearnerCourseDetail {
  id: string;
  title: string;
  subject: string;
  description: string;
  instructor: string;
  instructorInitials: string;
  instructorBio: string;
  thumbnail: string;
  rating: number;
  enrolled: number;
  duration: string;
  level: string;
  language: string;
  xp: number;
  completedLessons: number;
  totalLessons: number;
  gradientFrom: string;
  gradientTo: string;
  accentText: string;
  accentBg: string;
  accentBorder: string;
  lessons: LearnerLesson[];
}

export interface LearnerCertificate {
  id: number;
  courseTitle: string;
  subject: string;
  instructor: string;
  issuedAt: string;
  score: number;
  grade: string;
  xpEarned: number;
  duration: string;
  lessonCount: number;
  gradientFrom: string;
  gradientTo: string;
  accentText: string;
  accentBg: string;
  accentBorder: string;
}

// This file acts as the temporary contract for the web learner journey until the
// backend course, exam, and certificate endpoints are connected to the site.
export const learnerCourseCards: LearnerCourseCard[] = [
  {
    id: 1,
    title: "Web Development Fundamentals",
    subject: "Technology",
    subjectIcon: "code",
    instructor: "Khalid Moussaoui",
    instructorInitials: "KM",
    instructorColor: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 0,
    total: 8,
    nextLesson: "Introduction to HTML",
    estimatedMin: 12,
    xpEarned: 0,
    xpTotal: 700,
    status: "not-started",
    rating: 4.8,
    gradientFrom: "oklch(0.38 0.16 145)",
    gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary",
    accentBg: "bg-primary/8",
    accentBorder: "border-primary/20",
    duration: "18h",
  },
  {
    id: 2,
    title: "Business Communication in Arabic",
    subject: "Language",
    subjectIcon: "language",
    instructor: "Fatima Tahiri",
    instructorInitials: "FT",
    instructorColor: "bg-gold",
    thumbnail: "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 9,
    total: 30,
    nextLesson: "Professional Email Writing",
    estimatedMin: 22,
    xpEarned: 180,
    xpTotal: 600,
    status: "in-progress",
    rating: 4.6,
    gradientFrom: "oklch(0.78 0.14 80)",
    gradientTo: "oklch(0.68 0.16 70)",
    accentText: "text-gold",
    accentBg: "bg-gold/8",
    accentBorder: "border-gold/20",
    duration: "22h",
  },
  {
    id: 3,
    title: "Data Analysis with Excel",
    subject: "Business",
    subjectIcon: "business",
    instructor: "Youssef Kettani",
    instructorInitials: "YK",
    instructorColor: "bg-teal",
    thumbnail: "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 2,
    total: 20,
    nextLesson: "Introduction to Pivot Tables",
    estimatedMin: 25,
    xpEarned: 40,
    xpTotal: 400,
    status: "in-progress",
    rating: 4.5,
    gradientFrom: "oklch(0.72 0.10 200)",
    gradientTo: "oklch(0.58 0.14 190)",
    accentText: "text-teal",
    accentBg: "bg-teal/8",
    accentBorder: "border-teal/20",
    duration: "14h",
  },
  {
    id: 4,
    title: "Introduction to Python",
    subject: "Technology",
    subjectIcon: "code",
    instructor: "Omar Bennis",
    instructorInitials: "OB",
    instructorColor: "bg-primary",
    thumbnail: "https://images.unsplash.com/photo-1526379095098-d400fd0bf935?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 18,
    total: 18,
    nextLesson: "-",
    estimatedMin: 0,
    xpEarned: 360,
    xpTotal: 360,
    status: "completed",
    rating: 4.9,
    gradientFrom: "oklch(0.38 0.16 145)",
    gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary",
    accentBg: "bg-primary/8",
    accentBorder: "border-primary/20",
    duration: "12h",
  },
  {
    id: 5,
    title: "Darija for Professionals",
    subject: "Language",
    subjectIcon: "language",
    instructor: "Nadia Alami",
    instructorInitials: "NA",
    instructorColor: "bg-gold",
    thumbnail: "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 12,
    total: 12,
    nextLesson: "-",
    estimatedMin: 0,
    xpEarned: 240,
    xpTotal: 240,
    status: "completed",
    rating: 4.7,
    gradientFrom: "oklch(0.78 0.14 80)",
    gradientTo: "oklch(0.68 0.16 70)",
    accentText: "text-gold",
    accentBg: "bg-gold/8",
    accentBorder: "border-gold/20",
    duration: "9h",
  },
  {
    id: 6,
    title: "Graphic Design Basics",
    subject: "Design",
    subjectIcon: "design",
    instructor: "Salma Chraibi",
    instructorInitials: "SC",
    instructorColor: "bg-violet-500",
    thumbnail: "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=600&h=280&fit=crop&auto=format&q=80",
    lessons: 0,
    total: 22,
    nextLesson: "Design Principles",
    estimatedMin: 20,
    xpEarned: 0,
    xpTotal: 440,
    status: "not-started",
    rating: 4.4,
    gradientFrom: "oklch(0.55 0.22 290)",
    gradientTo: "oklch(0.45 0.20 280)",
    accentText: "text-violet-500",
    accentBg: "bg-violet-50 dark:bg-violet-500/10",
    accentBorder: "border-violet-200 dark:border-violet-500/20",
    duration: "16h",
  },
];

export const learnerCourseDetails: Record<string, LearnerCourseDetail> = {
  "1": {
    id: "1",
    title: "Web Development Fundamentals",
    subject: "Technology",
    description: "Enroll, watch the lesson videos, read the lesson PDF, complete the quiz, and finish with the final exam to unlock your certificate.",
    instructor: "Khalid Moussaoui",
    instructorInitials: "KM",
    instructorBio: "Senior web engineer in Casablanca focused on practical frontend delivery for beginner developers.",
    thumbnail: "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=900&h=400&fit=crop&auto=format&q=80",
    rating: 4.8,
    enrolled: 1240,
    duration: "18h",
    level: "Beginner",
    language: "English",
    xp: 700,
    completedLessons: 0,
    totalLessons: 8,
    gradientFrom: "oklch(0.38 0.16 145)",
    gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary",
    accentBg: "bg-primary/8",
    accentBorder: "border-primary/20",
    lessons: [
      { id: "1", title: "Introduction to HTML", duration: "12m", completed: false, locked: false, type: "video" },
      { id: "2", title: "HTML Elements & Attributes", duration: "18m", completed: false, locked: true, type: "video" },
      { id: "3", title: "Semantic HTML5", duration: "15m", completed: false, locked: true, type: "reading" },
      { id: "4", title: "CSS Fundamentals", duration: "20m", completed: false, locked: true, type: "video" },
      { id: "5", title: "Selectors & Specificity", duration: "16m", completed: false, locked: true, type: "reading" },
      { id: "6", title: "Box Model Deep Dive", duration: "22m", completed: false, locked: true, type: "quiz" },
      { id: "7", title: "CSS Flexbox & Grid", duration: "18m", completed: false, locked: true, type: "video" },
      { id: "8", title: "Responsive Design", duration: "25m", completed: false, locked: true, type: "video" },
    ],
  },
  "2": {
    id: "2",
    title: "Business Communication in Arabic",
    subject: "Language",
    description: "Master written and spoken formal Arabic for the workplace. Write clear emails, lead meetings, and negotiate effectively in Modern Standard Arabic.",
    instructor: "Fatima Tahiri",
    instructorInitials: "FT",
    instructorBio: "Linguist and corporate trainer with 12 years helping Moroccan professionals communicate in Arabic.",
    thumbnail: "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=900&h=400&fit=crop&auto=format&q=80",
    rating: 4.6,
    enrolled: 870,
    duration: "22h",
    level: "Intermediate",
    language: "Arabic",
    xp: 600,
    completedLessons: 9,
    totalLessons: 30,
    gradientFrom: "oklch(0.74 0.17 77)",
    gradientTo: "oklch(0.64 0.20 68)",
    accentText: "text-gold",
    accentBg: "bg-gold/8",
    accentBorder: "border-gold/20",
    lessons: [
      { id: "1", title: "Arabic in the Workplace", duration: "15m", completed: true, locked: false, type: "video" },
      { id: "2", title: "Professional Email Writing", duration: "20m", completed: false, locked: false, type: "video" },
      { id: "3", title: "Meeting Vocabulary", duration: "18m", completed: false, locked: true, type: "reading" },
    ],
  },
};

export const learnerCertificates: LearnerCertificate[] = [
  {
    id: 1,
    courseTitle: "Introduction to Python",
    subject: "Technology",
    instructor: "Omar Bennis",
    issuedAt: "2026-04-15",
    score: 94,
    grade: "Distinction",
    xpEarned: 360,
    duration: "12h",
    lessonCount: 18,
    gradientFrom: "oklch(0.38 0.16 145)",
    gradientTo: "oklch(0.52 0.20 142)",
    accentText: "text-primary",
    accentBg: "bg-primary/8",
    accentBorder: "border-primary/20",
  },
  {
    id: 2,
    courseTitle: "Darija for Professionals",
    subject: "Language",
    instructor: "Nadia Alami",
    issuedAt: "2026-03-02",
    score: 88,
    grade: "Merit",
    xpEarned: 240,
    duration: "9h",
    lessonCount: 12,
    gradientFrom: "oklch(0.74 0.17 77)",
    gradientTo: "oklch(0.64 0.20 68)",
    accentText: "text-gold",
    accentBg: "bg-gold/8",
    accentBorder: "border-gold/20",
  },
];

export function getLearnerCourseDetail(courseId: string) {
  return learnerCourseDetails[courseId] ?? learnerCourseDetails["1"];
}

export function getLearnerNextLesson(course: LearnerCourseDetail) {
  return course.lessons.find((lesson) => !lesson.completed && !lesson.locked) ?? null;
}
