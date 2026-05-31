import { appEnv, getEnvConfigurationError } from "../env";
import type {
  ApiErrorPayload,
  AuthSyncResponse,
  UserRole,
  CourseDetail,
  Certificate,
  CourseProgress,
  CourseSummary,
  EnrolledCourse,
  EnrollmentResponse,
  LessonDetail,
  PageResponse,
  Profile,
} from "./types";
import {
  demoEnrollInCourse,
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

    return fetch(buildUrl(path, options.query), {
      method: options.method ?? "GET",
      headers: requestHeaders,
      body,
    });
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

export function listMyCertificates(
  getAccessToken: NonNullable<RequestOptions["getAccessToken"]>,
) {
  if (appEnv.demoMode) {
    return demoListMyCertificates();
  }

  return makeRequest<Certificate[]>("api/v1/certificates", {
    getAccessToken,
  });
}
