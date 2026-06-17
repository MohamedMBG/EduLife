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

export interface AdminMetrics {
  totalLearners: number;
  totalTeachers: number;
  totalGroupAdmins: number;
  totalCoursesDraft: number;
  totalCoursesPublished: number;
  totalCoursesArchived: number;
  totalEnrollmentsActive: number;
  totalCertificates: number;
  pendingTeacherRequests: number;
}

export interface StudentAnalyticsSummary {
  activeEnrollments: number;
  lessonsCompleted: number;
  examAttempts: number;
  examsPassed: number;
  certificatesEarned: number;
}

export interface TeacherCourseAnalytics {
  courseId: string;
  title: string;
  status: string;
  activeEnrollments: number;
  learnersWithProgress: number;
  learnersCompleted: number;
  completionRatePercent: number;
  examAttempts: number;
  examsPassed: number;
  passRatePercent: number;
  certificatesIssued: number;
}

export interface TeacherAnalytics {
  totalCourses: number;
  courses: TeacherCourseAnalytics[];
}

export interface PlatformAnalytics {
  learners: number;
  teachers: number;
  groupAdmins: number;
  admins: number;
  coursesDraft: number;
  coursesPublished: number;
  coursesArchived: number;
  activeEnrollments: number;
  totalExamAttempts: number;
  totalExamsPassed: number;
  totalCertificates: number;
}

export interface Funnel {
  enrolled: number;
  started: number;
  completed: number;
  passed: number;
  certified: number;
}

export interface MonthCount {
  month: string;
  count: number;
}

export interface StudentProgressTrend {
  totalLessons: number;
  lessonsByMonth: MonthCount[];
}

export interface TeacherCohortAnalytics {
  courseCount: number;
  funnel: Funnel;
  enrollmentCohorts: MonthCount[];
}

export interface GroupCohortAnalytics {
  groupId: string;
  groupName: string;
  memberCount: number;
  courseCount: number;
  funnel: Funnel;
}

export interface PlatformCohortAnalytics {
  funnel: Funnel;
  enrollmentCohorts: MonthCount[];
  certificateTrend: MonthCount[];
}

export type TeacherRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface TeacherRequestSummary {
  id: string;
  userId: string;
  userEmail: string;
  status: TeacherRequestStatus;
  motivation: string | null;
  adminNote: string | null;
  requestedAt: string;
  reviewedAt: string | null;
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
  learnerName: string;
  teacherName: string;
  courseTitle: string;
  courseLevel: string;
  issuedAt: string;
  verificationHash: string;
}

export interface CertificateDetail {
  id: string;
  courseId: string;
  certificateNumber: string;
  learnerName: string;
  teacherName: string;
  courseTitle: string;
  courseLevel: string;
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

export interface CourseCoverUploadResponse {
  courseId: string;
  coverImageUrl: string;
  message: string;
}

export type CourseStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export type CmsLessonType = "VIDEO" | "ARTICLE" | "RESOURCE";

export interface CmsCourse {
  id: string;
  slug: string;
  title: string;
  shortDescription: string | null;
  description: string;
  languageCode: string;
  level: string | null;
  imageUrl: string | null;
  status: CourseStatus;
  publishedAt: string | null;
  createdByUserId: string;
  createdByEmail: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCmsCourseRequest {
  title: string;
  shortDescription?: string;
  description: string;
  languageCode: string;
  level?: string;
  imageUrl?: string;
}

export interface CmsSection {
  id: string;
  courseId: string;
  title: string;
  description: string | null;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCmsSectionRequest {
  title: string;
  description?: string;
  displayOrder: number;
}

export interface CmsLesson {
  id: string;
  courseSectionId: string;
  title: string;
  summary: string | null;
  lessonType: string;
  estimatedDurationMinutes: number | null;
  displayOrder: number;
  preview: boolean;
  contentUrl: string | null;
  contentBody: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCmsLessonRequest {
  title: string;
  summary?: string;
  lessonType: CmsLessonType;
  estimatedDurationMinutes?: number;
  displayOrder: number;
  preview: boolean;
  contentUrl?: string;
  contentBody?: string;
}

export interface GroupSummary {
  id: string;
  name: string;
  createdAt: string;
  memberCount: number;
  courseCount: number;
}

export interface GroupMemberDetail {
  userId: string;
  email: string;
  role: UserRole | null;
  addedAt: string;
}

export interface GroupCourseDetail {
  courseId: string;
  title: string;
  status: CourseStatus | null;
  attachedAt: string;
}

export interface GroupDetail {
  id: string;
  name: string;
  createdAt: string;
  members: GroupMemberDetail[];
  courses: GroupCourseDetail[];
}

export interface CertificateVerification {
  learnerName: string;
  teacherName: string;
  courseTitle: string;
  courseLevel: string;
  issuedAt: string;
  certificateNumber: string;
  verificationHash: string;
  valid: boolean;
}

// ── CMS Exam types (teacher/admin only — includes correct-answer flags) ──────

export interface CmsExamChoiceRequest {
  choiceText: string;
  correct: boolean;
}

export interface CmsExamQuestionRequest {
  questionText: string;
  orderIndex: number;
  choices: CmsExamChoiceRequest[];
}

export interface CreateCmsExamRequest {
  title: string;
  passScore: number;
  timeLimitMinutes?: number;
  questions: CmsExamQuestionRequest[];
}

export interface CmsExamChoice {
  id: string;
  choiceText: string;
  correct: boolean;
}

export interface CmsExamQuestion {
  id: string;
  questionText: string;
  orderIndex: number;
  choices: CmsExamChoice[];
}

export interface CmsExamAdmin {
  id: string;
  courseId: string;
  title: string;
  passScore: number;
  timeLimitMinutes: number | null;
  questions: CmsExamQuestion[];
}
