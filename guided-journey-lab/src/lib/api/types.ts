export interface ApiErrorPayload {
  status: number;
  message: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export type UserRole = "LEARNER" | "TEACHER" | "GROUP_ADMIN" | "ADMIN";

export interface AuthSyncResponse {
  userId: string;
  role: UserRole;
}

export interface CourseSummary {
  id: string;
  slug: string;
  title: string;
  shortDescription: string;
  level: string;
  languageCode: string;
  imageUrl: string | null;
  publishedAt: string;
}

export interface LessonSummary {
  id: string;
  title: string;
  summary: string | null;
  lessonType: string;
  estimatedDurationMinutes: number | null;
  displayOrder: number | null;
  preview: boolean;
}

export interface CourseSection {
  id: string;
  title: string;
  description: string | null;
  displayOrder: number | null;
  lessons: LessonSummary[];
}

export interface CourseDetail {
  id: string;
  slug: string;
  title: string;
  shortDescription: string;
  description: string;
  level: string;
  languageCode: string;
  imageUrl: string | null;
  publishedAt: string;
  sections: CourseSection[];
}

export interface EnrollmentResponse {
  enrollmentId: string;
  courseId: string;
  enrolledAt: string;
  status: string;
}

export interface EnrolledCourse {
  enrollmentId: string;
  courseId: string;
  slug: string;
  title: string;
  shortDescription: string;
  level: string;
  languageCode: string;
  imageUrl: string | null;
  enrolledAt: string;
}

export interface Profile {
  userId: string;
  email: string;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  enrolledCourses: number;
  completedLessons: number;
  certificates: number;
}

export interface CourseProgressSection {
  sectionId: string;
  title: string;
  displayOrder: number;
  lessons: CourseProgressLesson[];
}

export interface CourseProgressLesson {
  lessonId: string;
  title: string;
  lessonType: string;
  durationMinutes: number | null;
  displayOrder: number;
  preview: boolean;
  completed: boolean;
  completedAt: string | null;
}

export interface CourseProgress {
  courseId: string;
  completedLessons: number;
  totalLessons: number;
  percentComplete: number;
  sections: CourseProgressSection[];
}

export interface LessonDetail {
  lessonId: string;
  courseId: string;
  sectionId: string;
  sectionTitle: string;
  title: string;
  summary: string | null;
  lessonType: string;
  contentUrl: string | null;
  contentBody: string | null;
  durationMinutes: number | null;
  displayOrder: number | null;
  preview: boolean;
  completed: boolean;
}

export interface Certificate {
  id: string;
  courseId: string;
  certificateNumber: string;
  courseTitle: string;
  issuedAt: string;
}

export interface CertificateDetail {
  id: string;
  courseId: string;
  certificateNumber: string;
  studentName: string;
  courseTitle: string;
  issuerName: string;
  issuedAt: string;
  verificationHash: string;
  pdfUrl: string | null;
}

export interface ExamChoice {
  choiceId: string;
  choiceText: string;
}

export interface ExamQuestion {
  questionId: string;
  questionText: string;
  orderIndex: number;
  choices: ExamChoice[];
}

export interface Exam {
  examId: string;
  courseId: string;
  title: string;
  passScore: number;
  timeLimitMinutes: number | null;
  questions: ExamQuestion[];
}

export interface ExamStatus {
  examId: string;
  passed: boolean;
  failedAttempts: number;
  maxAttemptsBeforeCooldown: number;
  inCooldown: boolean;
  cooldownEndsAt: string | null;
}

export interface ExamResult {
  examId: string;
  score: number;
  passScore: number;
  passed: boolean;
  certificateNumber: string | null;
  attemptsUsed: number;
  cooldownEndsAt: string | null;
}

export interface ExamAnswer {
  questionId: string;
  choiceId: string;
}

export interface ExamSubmitRequest {
  answers: ExamAnswer[];
}

export interface UpdateProfileRequest {
  displayName: string;
  bio: string;
}

export interface AvatarUploadResponse {
  avatarUrl: string;
}

export interface CertificateVerification {
  studentName: string;
  courseTitle: string;
  issuerName: string;
  issuedAt: string;
  certificateNumber: string;
  valid: boolean;
}
