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

export interface AuthSyncResponse {
  userId: string;
  role: string;
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
  certificateId: string;
  courseId: string;
  certificateNumber: string;
  issuedAt: string;
}
