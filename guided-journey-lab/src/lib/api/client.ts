import { appEnv, getEnvConfigurationError } from "../env";
import type {
  AdminMetrics,
  ApiErrorPayload,
  AuthSyncResponse,
  AvatarUploadResponse,
  UserRole,
  Certificate,
  CertificateDetail,
  CertificateVerification,
  CmsCourse,
  CmsLesson,
  CmsSection,
  CourseDetail,
  CreateCmsCourseRequest,
  CreateCmsLessonRequest,
  CreateCmsSectionRequest,
  CourseProgress,
  CourseSummary,
  EnrolledCourse,
  EnrollmentResponse,
  Exam,
  ExamResult,
  ExamStatus,
  ExamSubmitRequest,
  GroupDetail,
  GroupCohortAnalytics,
  GroupSummary,
  LessonDetail,
  PlatformAnalytics,
  PlatformCohortAnalytics,
  PageResponse,
  Profile,
  StudentAnalyticsSummary,
  StudentProgressTrend,
  TeacherAnalytics,
  TeacherCohortAnalytics,
  TeacherRequestSummary,
  TeacherRequestStatus,
  UpdateProfileRequest,
} from "./types";
import {
  demoEnrollInCourse,
  demoGetCertificate,
  demoGetCourseDetail,
  demoGetCourseProgress,
  demoGetLessonDetail,
  demoGetProfile,
  demoListCourses,
  demoListMyCertificates,
  demoListMyEnrollments,
  demoMarkLessonComplete,
  demoSyncAuth,
  demoUnenrollFromCourse,
  demoVerifyCertificate,
} from "./demo";

export class ApiClientError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: BodyInit | object;
  headers?: HeadersInit;
  query?: Record<string, string | number | undefined>;
  getAccessToken?: (forceRefresh?: boolean) => Promise<string | null>;
}

function buildUrl(path: string, query?: RequestOptions["query"]) {
  if (appEnv.demoMode) {
    throw new ApiClientError(500, "Network requests are disabled in website demo mode.");
  }

  const configError = getEnvConfigurationError();

  if (configError) {
    throw new ApiClientError(500, configError);
  }

  const url = new URL(path, `${appEnv.apiBaseUrl}/`);

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  return url.toString();
}

async function parseError(response: Response) {
  let payload: ApiErrorPayload | null = null;

  try {
    payload = (await response.json()) as ApiErrorPayload;
  } catch {
    payload = null;
  }

  throw new ApiClientError(
    response.status,
    payload?.message || `Request failed with status ${response.status}`,
  );
}

const REQUEST_TIMEOUT_MS = 15_000;

async function makeRequest<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers(options.headers);
  let token = options.getAccessToken ? await options.getAccessToken(false) : null;

  async function executeRequest(accessToken: string | null) {
    const requestHeaders = new Headers(headers);

    if (accessToken) {
      requestHeaders.set("Authorization", `Bearer ${accessToken}`);
    }

    let body = options.body;

    if (body && !(body instanceof FormData) && typeof body === "object") {
      requestHeaders.set("Content-Type", "application/json");
      body = JSON.stringify(body);
    }

    let response: Response;
    try {
      response = await fetch(buildUrl(path, options.query), {
        method: options.method ?? "GET",
        headers: requestHeaders,
        body,
        signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
      });
    } catch (e) {
      if (e instanceof DOMException && e.name === "TimeoutError") {
        throw new ApiClientError(
          503,
          "The server is taking too long to respond. Please try again in a moment.",
        );
      }
      if (e instanceof TypeError) {
        throw new ApiClientError(
          503,
          "Cannot reach the server. Check your connection and try again.",
        );
      }
      throw e;
    }
    return response;
  }

  let response = await executeRequest(token);

  if (response.status === 401 && options.getAccessToken) {
    // Protected EduLife endpoints rely on Firebase ID tokens, so retry once after a forced
    // refresh before surfacing a login failure to the user.
    token = await options.getAccessToken(true);
    response = await executeRequest(token);
  }

  if (!response.ok) {
    await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function syncAuth(
  getAccessToken: RequestOptions["getAccessToken"],
  intendedRole?: UserRole,
) {
  if (appEnv.demoMode) {
    return demoSyncAuth();
  }

  return makeRequest<AuthSyncResponse>("api/v1/auth/sync", {
    method: "POST",
    body: intendedRole ? { intendedRole } : undefined,
    getAccessToken,
  });
}

export function getProfile(getAccessToken: NonNullable<RequestOptions["getAccessToken"]>) {
  if (appEnv.demoMode) {
    return demoGetProfile();
  }

  return makeRequest<Profile>("api/v1/profile", { getAccessToken });
}

export function getAdminMetrics(getAccessToken: NonNullable<RequestOptions["getAccessToken"]>) {
  if (appEnv.demoMode) {
    return Promise.resolve<AdminMetrics>({
      totalLearners: 0,
      totalTeachers: 0,
      totalGroupAdmins: 0,
      totalCoursesDraft: 0,
      totalCoursesPublished: 0,
      totalCoursesArchived: 0,
      totalEnrollmentsActive: 0,
      totalCertificates: 0,
      pendingTeacherRequests: 0,
    });
  }

  // This endpoint is intentionally admin-only; callers should gate it by the synced backend role.
  return makeRequest<AdminMetrics>("api/v1/admin/metrics", { getAccessToken });
}

function emptyFunnel() {
  return { enrolled: 0, started: 0, completed: 0, passed: 0, certified: 0 };
}

// ── Analytics endpoints (server-scoped by authenticated user / role) ──────────

export function getStudentAnalyticsSummary(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
) {
  if (appEnv.demoMode) {
    return Promise.resolve<StudentAnalyticsSummary>({
      activeEnrollments: 0,
      lessonsCompleted: 0,
      examAttempts: 0,
      examsPassed: 0,
      certificatesEarned: 0,
    });
  }

  // The backend resolves the student from the Firebase token; no user id is sent from the web app.
  return makeRequest<StudentAnalyticsSummary>("api/v1/analytics/me/summary", { getAccessToken });
}

export function getStudentProgressTrend(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
) {
  if (appEnv.demoMode) {
    return Promise.resolve<StudentProgressTrend>({ totalLessons: 0, lessonsByMonth: [] });
  }

  return makeRequest<StudentProgressTrend>("api/v1/analytics/me/progress-trend", {
    getAccessToken,
  });
}

export function getTeacherAnalytics(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
) {
  if (appEnv.demoMode) {
    return Promise.resolve<TeacherAnalytics>({ totalCourses: 0, courses: [] });
  }

  // Teacher ownership is enforced server-side from the resolved user; the client sends no teacherId.
  return makeRequest<TeacherAnalytics>("api/v1/analytics/teacher/courses", { getAccessToken });
}

export function getTeacherCohortAnalytics(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
) {
  if (appEnv.demoMode) {
    return Promise.resolve<TeacherCohortAnalytics>({
      courseCount: 0,
      funnel: emptyFunnel(),
      enrollmentCohorts: [],
    });
  }

  return makeRequest<TeacherCohortAnalytics>("api/v1/analytics/teacher/cohorts", {
    getAccessToken,
  });
}

export function getGroupCohortAnalytics(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  groupId: string,
) {
  if (appEnv.demoMode) {
    return Promise.resolve<GroupCohortAnalytics>({
      groupId,
      groupName: "Demo group",
      memberCount: 0,
      courseCount: 0,
      funnel: emptyFunnel(),
    });
  }

  // Backend re-checks group ownership, so a foreign groupId still returns 403.
  return makeRequest<GroupCohortAnalytics>(`api/v1/analytics/group/${groupId}/cohorts`, {
    getAccessToken,
  });
}

export function getPlatformAnalytics(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
) {
  if (appEnv.demoMode) {
    return Promise.resolve<PlatformAnalytics>({
      learners: 0,
      teachers: 0,
      groupAdmins: 0,
      admins: 0,
      coursesDraft: 0,
      coursesPublished: 0,
      coursesArchived: 0,
      activeEnrollments: 0,
      totalExamAttempts: 0,
      totalExamsPassed: 0,
      totalCertificates: 0,
    });
  }

  return makeRequest<PlatformAnalytics>("api/v1/analytics/platform", { getAccessToken });
}

export function getPlatformCohortAnalytics(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
) {
  if (appEnv.demoMode) {
    return Promise.resolve<PlatformCohortAnalytics>({
      funnel: emptyFunnel(),
      enrollmentCohorts: [],
      certificateTrend: [],
    });
  }

  return makeRequest<PlatformCohortAnalytics>("api/v1/analytics/platform/cohorts", {
    getAccessToken,
  });
}

export function updateProfile(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  payload: UpdateProfileRequest,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Profile editing is not available in website demo mode.");
  }

  return makeRequest<Profile>("api/v1/profile", {
    method: "PUT",
    body: payload,
    getAccessToken,
  });
}

export function uploadAvatar(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  file: File,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Avatar uploads are not available in website demo mode.");
  }

  const body = new FormData();
  body.append("file", file);

  return makeRequest<AvatarUploadResponse>("api/v1/profile/avatar", {
    method: "POST",
    body,
    getAccessToken,
  });
}

export function listCourses(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  query: { q?: string; category?: string; page?: number; size?: number } = {},
) {
  if (appEnv.demoMode) {
    return demoListCourses(query) as Promise<PageResponse<CourseSummary>>;
  }

  return makeRequest<PageResponse<CourseSummary>>("api/v1/courses", {
    getAccessToken,
    query,
  });
}

export function getCourseDetail(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
) {
  if (appEnv.demoMode) {
    return demoGetCourseDetail(courseId);
  }

  return makeRequest<CourseDetail>(`api/v1/courses/${courseId}`, {
    getAccessToken,
  });
}

export function listMyEnrollments(getAccessToken: NonNullable<RequestOptions["getAccessToken"]>) {
  if (appEnv.demoMode) {
    return demoListMyEnrollments();
  }

  return makeRequest<EnrolledCourse[]>("api/v1/enrollments/me", { getAccessToken });
}

export function enrollInCourse(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
) {
  if (appEnv.demoMode) {
    return demoEnrollInCourse(courseId);
  }

  return makeRequest<EnrollmentResponse>("api/v1/enrollments", {
    method: "POST",
    body: { courseId },
    getAccessToken,
  });
}

export function unenrollFromCourse(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  enrollmentId: string,
) {
  if (appEnv.demoMode) {
    return demoUnenrollFromCourse(enrollmentId);
  }

  return makeRequest<void>(`api/v1/enrollments/${enrollmentId}`, {
    method: "DELETE",
    getAccessToken,
  });
}

export function getCourseProgress(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
) {
  if (appEnv.demoMode) {
    return demoGetCourseProgress(courseId);
  }

  return makeRequest<CourseProgress>(`api/v1/progress/courses/${courseId}`, {
    getAccessToken,
  });
}

export function getLessonDetail(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
  lessonId: string,
) {
  if (appEnv.demoMode) {
    return demoGetLessonDetail(courseId, lessonId);
  }

  return makeRequest<LessonDetail>(`api/v1/courses/${courseId}/lessons/${lessonId}`, {
    getAccessToken,
  });
}

export function markLessonComplete(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
  lessonId: string,
) {
  if (appEnv.demoMode) {
    return demoMarkLessonComplete(courseId, lessonId);
  }

  return makeRequest<void>(`api/v1/courses/${courseId}/lessons/${lessonId}/complete`, {
    method: "POST",
    getAccessToken,
  });
}

export function listMyCertificates(getAccessToken: NonNullable<RequestOptions["getAccessToken"]>) {
  if (appEnv.demoMode) {
    return demoListMyCertificates();
  }

  return makeRequest<Certificate[]>("api/v1/certificates/me", {
    getAccessToken,
  });
}

export function getCertificate(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  certificateId: string,
) {
  if (appEnv.demoMode) {
    return demoGetCertificate(certificateId);
  }

  return makeRequest<CertificateDetail>(`api/v1/certificates/${certificateId}`, {
    getAccessToken,
  });
}

export async function downloadCertificate(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  certificateId: string,
): Promise<Blob> {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Certificate downloads are not available in website demo mode.");
  }

  const headers = new Headers();
  let token = await getAccessToken(false);

  async function executeRequest(accessToken: string | null) {
    const requestHeaders = new Headers(headers);
    if (accessToken) {
      requestHeaders.set("Authorization", `Bearer ${accessToken}`);
    }
    return fetch(buildUrl(`api/v1/certificates/${certificateId}/download`), {
      method: "GET",
      headers: requestHeaders,
    });
  }

  let response = await executeRequest(token);

  if (response.status === 401) {
    token = await getAccessToken(true);
    response = await executeRequest(token);
  }

  if (!response.ok) {
    await parseError(response);
  }

  return response.blob();
}

export function getExam(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Exams are not available in website demo mode.");
  }

  return makeRequest<Exam>(`api/v1/courses/${courseId}/exam`, {
    getAccessToken,
  });
}

export function getExamStatus(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Exams are not available in website demo mode.");
  }

  return makeRequest<ExamStatus>(`api/v1/courses/${courseId}/exam/status`, {
    getAccessToken,
  });
}

export function submitExam(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
  payload: ExamSubmitRequest,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Exams are not available in website demo mode.");
  }

  return makeRequest<ExamResult>(`api/v1/courses/${courseId}/exam/submit`, {
    method: "POST",
    body: payload,
    getAccessToken,
  });
}

export interface AdvisorApiResponse {
  message: string;
  recommendations: Array<{ courseId: string; reason: string; score: number }>;
}

export function requestAdvisorRecommendation(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  goal: string,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "AI Advisor is not available in demo mode.");
  }

  return makeRequest<AdvisorApiResponse>("api/v1/advisor/recommend", {
    method: "POST",
    body: { goal },
    getAccessToken,
  });
}

export function verifyCertificate(hash: string) {
  if (appEnv.demoMode) {
    return demoVerifyCertificate(hash);
  }

  return makeRequest<CertificateVerification>(
    `api/v1/certificates/verify/${encodeURIComponent(hash)}`,
  );
}

// ── Admin endpoints (ADMIN role only) ────────────────────────────────────────

export function listAdminTeacherRequests(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  status: TeacherRequestStatus = "PENDING",
  page = 0,
  size = 50,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Admin endpoints not available in demo mode.");
  }

  return makeRequest<PageResponse<TeacherRequestSummary>>("api/v1/admin/teacher-requests", {
    getAccessToken,
    query: { status, page, size },
  });
}

export function approveTeacherRequest(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  requestId: string,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Admin endpoints not available in demo mode.");
  }

  return makeRequest<TeacherRequestSummary>(`api/v1/admin/teacher-requests/${requestId}/approve`, {
    method: "PUT",
    getAccessToken,
  });
}

export function rejectTeacherRequest(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  requestId: string,
  adminNote?: string,
) {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Admin endpoints not available in demo mode.");
  }

  return makeRequest<TeacherRequestSummary>(`api/v1/admin/teacher-requests/${requestId}/reject`, {
    method: "PUT",
    body: adminNote ? { adminNote } : {},
    getAccessToken,
  });
}

// ── CMS endpoints (TEACHER / GROUP_ADMIN / ADMIN; ownership enforced server-side) ──

function assertCmsAvailable() {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Course management is not available in website demo mode.");
  }
}

export function listCmsCourses(getAccessToken: NonNullable<RequestOptions["getAccessToken"]>) {
  assertCmsAvailable();

  return makeRequest<CmsCourse[]>("api/v1/cms/courses", { getAccessToken });
}

export function createCmsCourse(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  payload: CreateCmsCourseRequest,
) {
  assertCmsAvailable();

  return makeRequest<CmsCourse>("api/v1/cms/courses", {
    method: "POST",
    body: payload,
    getAccessToken,
  });
}

export function publishCmsCourse(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
) {
  assertCmsAvailable();

  // GROUP_ADMIN may only publish courses from teachers in their groups; ADMIN may publish any.
  return makeRequest<CmsCourse>(`api/v1/cms/courses/${courseId}/publish`, {
    method: "PUT",
    getAccessToken,
  });
}

export function listCmsSections(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
) {
  assertCmsAvailable();

  return makeRequest<CmsSection[]>(`api/v1/cms/courses/${courseId}/sections`, { getAccessToken });
}

export function createCmsSection(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
  payload: CreateCmsSectionRequest,
) {
  assertCmsAvailable();

  return makeRequest<CmsSection>(`api/v1/cms/courses/${courseId}/sections`, {
    method: "POST",
    body: payload,
    getAccessToken,
  });
}

export function deleteCmsSection(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  courseId: string,
  sectionId: string,
) {
  assertCmsAvailable();

  return makeRequest<void>(`api/v1/cms/courses/${courseId}/sections/${sectionId}`, {
    method: "DELETE",
    getAccessToken,
  });
}

export function listCmsLessons(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  sectionId: string,
) {
  assertCmsAvailable();

  return makeRequest<CmsLesson[]>(`api/v1/cms/sections/${sectionId}/lessons`, { getAccessToken });
}

export function createCmsLesson(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  sectionId: string,
  payload: CreateCmsLessonRequest,
) {
  assertCmsAvailable();

  return makeRequest<CmsLesson>(`api/v1/cms/sections/${sectionId}/lessons`, {
    method: "POST",
    body: payload,
    getAccessToken,
  });
}

// ── Groups endpoints (TEACHER / GROUP_ADMIN / ADMIN; owner-scoped server-side) ──

function assertGroupsAvailable() {
  if (appEnv.demoMode) {
    throw new ApiClientError(501, "Group management is not available in website demo mode.");
  }
}

export function listMyGroups(getAccessToken: NonNullable<RequestOptions["getAccessToken"]>) {
  assertGroupsAvailable();

  return makeRequest<GroupSummary[]>("api/v1/groups", { getAccessToken });
}

export function getGroupDetail(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  groupId: string,
) {
  assertGroupsAvailable();

  return makeRequest<GroupDetail>(`api/v1/groups/${groupId}`, { getAccessToken });
}

export function createGroup(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  name: string,
) {
  assertGroupsAvailable();

  // The create endpoint returns the bare GroupDto (no counts) — callers refetch the list.
  return makeRequest<{ id: string; name: string; createdAt: string }>("api/v1/groups", {
    method: "POST",
    body: { name },
    getAccessToken,
  });
}

export function addGroupMember(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  groupId: string,
  email: string,
) {
  assertGroupsAvailable();

  return makeRequest<unknown>(`api/v1/groups/${groupId}/members`, {
    method: "POST",
    body: { email },
    getAccessToken,
  });
}

export function removeGroupMember(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  groupId: string,
  userId: string,
) {
  assertGroupsAvailable();

  return makeRequest<void>(`api/v1/groups/${groupId}/members/${userId}`, {
    method: "DELETE",
    getAccessToken,
  });
}

export function attachGroupCourse(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  groupId: string,
  courseId: string,
) {
  assertGroupsAvailable();

  return makeRequest<unknown>(`api/v1/groups/${groupId}/courses`, {
    method: "POST",
    body: { courseId },
    getAccessToken,
  });
}

export function deleteCmsLesson(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
  sectionId: string,
  lessonId: string,
) {
  assertCmsAvailable();

  return makeRequest<void>(`api/v1/cms/sections/${sectionId}/lessons/${lessonId}`, {
    method: "DELETE",
    getAccessToken,
  });
}
