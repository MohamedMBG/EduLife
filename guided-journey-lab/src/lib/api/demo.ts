import type {
  Certificate,
  CertificateDetail,
  CertificateVerification,
  CourseDetail,
  CourseProgress,
  EnrolledCourse,
  EnrollmentResponse,
  LessonDetail,
  PageResponse,
  Profile,
} from "./types";

interface DemoLessonTemplate {
  id: string;
  title: string;
  summary: string;
  lessonType: string;
  estimatedDurationMinutes: number;
  displayOrder: number;
  preview: boolean;
  contentBody: string;
  contentUrl: string | null;
}

interface DemoSectionTemplate {
  id: string;
  title: string;
  description: string;
  displayOrder: number;
  lessons: DemoLessonTemplate[];
}

interface DemoCourseTemplate {
  id: string;
  slug: string;
  title: string;
  shortDescription: string;
  description: string;
  level: string;
  languageCode: string;
  imageUrl: string | null;
  publishedAt: string;
  sections: DemoSectionTemplate[];
}

interface DemoEnrollmentRecord {
  enrollmentId: string;
  courseId: string;
  enrolledAt: string;
  status: string;
}

interface DemoCertificateRecord extends Certificate {
  source: "seeded" | "demo";
}

interface DemoSession {
  userId: string;
  role: string;
  email: string;
  displayName: string;
}

interface DemoStore {
  session: DemoSession | null;
  profile: {
    bio: string | null;
    avatarUrl: string | null;
  };
  enrollments: DemoEnrollmentRecord[];
  completedLessonIds: string[];
  certificates: DemoCertificateRecord[];
}

const DEMO_STORAGE_KEY = "edulife.website.demo.store.v2";

const demoCourses: DemoCourseTemplate[] = [
  {
    id: "course-darija-web",
    slug: "darija-web-foundations",
    title: "Web Foundations in Darija",
    shortDescription:
      "A friendly first step into HTML, CSS, and page structure with examples grounded in local business ideas.",
    description:
      "This demo course shows how EduLife organizes sections, lessons, progress, and certificates. It follows a beginner-friendly path from page structure to visual styling.",
    level: "BEGINNER",
    languageCode: "darija",
    imageUrl: "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80",
    publishedAt: "2026-05-10T09:00:00Z",
    sections: [
      {
        id: "section-darija-1",
        title: "Start with structure",
        description: "Build the first mental model for how a page is assembled.",
        displayOrder: 1,
        lessons: [
          {
            id: "lesson-darija-1",
            title: "How the web page is built",
            summary: "Understand the role of HTML before styling or scripting.",
            lessonType: "READING",
            estimatedDurationMinutes: 12,
            displayOrder: 1,
            preview: true,
            contentUrl: null,
            contentBody:
              "HTML gives a page its structure. In EduLife terms, think of it like the course outline before the lesson design.\n\nStart with headings, paragraphs, lists, and links. Once structure is clear, style becomes easier and more consistent.",
          },
          {
            id: "lesson-darija-2",
            title: "CSS basics that make pages feel alive",
            summary: "Move from bare structure to color, spacing, and hierarchy.",
            lessonType: "READING",
            estimatedDurationMinutes: 16,
            displayOrder: 2,
            preview: false,
            contentUrl: null,
            contentBody:
              "CSS controls spacing, typography, and color. A small number of consistent rules creates a much stronger interface than many random tweaks.\n\nUse layout, scale, and contrast intentionally.",
          },
        ],
      },
      {
        id: "section-darija-2",
        title: "Practice the learner flow",
        description: "Apply the structure in a realistic mini-project.",
        displayOrder: 2,
        lessons: [
          {
            id: "lesson-darija-3",
            title: "Create a landing section for a course",
            summary: "Combine layout and messaging into one polished section.",
            lessonType: "PROJECT",
            estimatedDurationMinutes: 20,
            displayOrder: 1,
            preview: false,
            contentUrl: null,
            contentBody:
              "Use one clear headline, one supporting paragraph, and one action. The goal is clarity, not decoration.\n\nA strong landing section tells the learner what the course is, who it is for, and why it matters.",
          },
        ],
      },
    ],
  },
  {
    id: "course-french-ui",
    slug: "ui-principles-french",
    title: "UI Principles for Student Apps",
    shortDescription:
      "A practical French-language course on interface clarity, hierarchy, and conversion-focused screen design.",
    description:
      "This course is already completed in the demo account so the certificates screen has a realistic earned credential to display.",
    level: "INTERMEDIATE",
    languageCode: "fr",
    imageUrl: "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=80",
    publishedAt: "2026-05-12T09:00:00Z",
    sections: [
      {
        id: "section-french-1",
        title: "Hierarchy and readability",
        description: "Design information so students know where to focus first.",
        displayOrder: 1,
        lessons: [
          {
            id: "lesson-french-1",
            title: "Typography that guides attention",
            summary: "Use scale and contrast to make the path obvious.",
            lessonType: "VIDEO",
            estimatedDurationMinutes: 14,
            displayOrder: 1,
            preview: false,
            contentUrl: "https://www.example.com/demo-ui-typography",
            contentBody: "",
          },
          {
            id: "lesson-french-2",
            title: "Reduce friction in critical actions",
            summary: "Clarify CTAs, remove hesitation, and shorten the path.",
            lessonType: "READING",
            estimatedDurationMinutes: 11,
            displayOrder: 2,
            preview: false,
            contentUrl: null,
            contentBody:
              "Every extra choice weakens the primary action. For an enrollment flow, the screen should make the next step obvious and low risk.",
          },
        ],
      },
    ],
  },
  {
    id: "course-english-career",
    slug: "career-skills-english",
    title: "Career Skills for Digital Learners",
    shortDescription:
      "Short English lessons on planning, consistency, and portfolio habits for self-directed learners.",
    description:
      "A catalog-only course in the demo so Explore still has something available to enroll in without any backend.",
    level: "BEGINNER",
    languageCode: "en",
    imageUrl: "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80",
    publishedAt: "2026-05-14T09:00:00Z",
    sections: [
      {
        id: "section-career-1",
        title: "Plan your path",
        description: "Turn vague ambition into a repeatable weekly system.",
        displayOrder: 1,
        lessons: [
          {
            id: "lesson-career-1",
            title: "Weekly learning plans",
            summary: "Build a schedule that survives busy weeks.",
            lessonType: "READING",
            estimatedDurationMinutes: 9,
            displayOrder: 1,
            preview: true,
            contentUrl: null,
            contentBody:
              "A workable plan fits your real life. Keep the weekly target small enough that you can hit it consistently and build trust in your own system.",
          },
          {
            id: "lesson-career-2",
            title: "Portfolio proof over passive consumption",
            summary: "Turn learning into visible evidence.",
            lessonType: "PROJECT",
            estimatedDurationMinutes: 18,
            displayOrder: 2,
            preview: false,
            contentUrl: null,
            contentBody:
              "Every learning sprint should produce something visible: a note, a mockup, a code sample, or a case study. Proof compounds motivation.",
          },
        ],
      },
    ],
  },
];

const allLessonIds = demoCourses.flatMap((course) =>
  course.sections.flatMap((section) => section.lessons.map((lesson) => lesson.id)),
);

function createInitialStore(): DemoStore {
  return {
    session: null,
    profile: {
      bio: "Demo learner exploring the EduLife guided journey without Firebase or the Spring Boot API.",
      avatarUrl: null,
    },
    enrollments: [
      {
        enrollmentId: "enrollment-darija-web",
        courseId: "course-darija-web",
        enrolledAt: "2026-05-20T10:00:00Z",
        status: "ACTIVE",
      },
      {
        enrollmentId: "enrollment-french-ui",
        courseId: "course-french-ui",
        enrolledAt: "2026-05-18T10:00:00Z",
        status: "ACTIVE",
      },
    ],
    completedLessonIds: ["lesson-darija-1", "lesson-french-1", "lesson-french-2"],
    certificates: [
      {
        id: "certificate-french-ui",
        courseId: "course-french-ui",
        certificateNumber: "EDU-DEMO-2026-0001",
        learnerName: "Demo User",
        teacherName: "EduLife Demo",
        courseTitle: "French UI Vocabulary",
        courseLevel: "Beginner",
        issuedAt: "2026-05-22T15:30:00Z",
        verificationHash: "demo-certificate-french-ui",
        source: "seeded",
      },
    ],
  };
}

function cloneStore(store: DemoStore): DemoStore {
  return JSON.parse(JSON.stringify(store)) as DemoStore;
}

function readStore() {
  if (typeof window === "undefined") {
    return cloneStore(createInitialStore());
  }

  const rawStore = window.localStorage.getItem(DEMO_STORAGE_KEY);

  if (!rawStore) {
    const initialStore = createInitialStore();
    writeStore(initialStore);
    return cloneStore(initialStore);
  }

  try {
    const parsed = JSON.parse(rawStore) as DemoStore;

    return {
      ...createInitialStore(),
      ...parsed,
      profile: { ...createInitialStore().profile, ...parsed.profile },
      enrollments: parsed.enrollments ?? [],
      completedLessonIds: (parsed.completedLessonIds ?? []).filter((lessonId) =>
        allLessonIds.includes(lessonId),
      ),
      certificates: parsed.certificates ?? [],
    };
  } catch {
    const initialStore = createInitialStore();
    writeStore(initialStore);
    return cloneStore(initialStore);
  }
}

function writeStore(store: DemoStore) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(store));
}

function updateStore(mutator: (store: DemoStore) => DemoStore) {
  const nextStore = mutator(readStore());
  writeStore(nextStore);
  return nextStore;
}

function requireSession(store = readStore()) {
  if (!store.session) {
    throw new Error("Demo session not found. Sign in from the website demo first.");
  }

  return store.session;
}

function getCourseTemplate(courseId: string) {
  const course = demoCourses.find((item) => item.id === courseId);

  if (!course) {
    throw new Error("Demo course not found.");
  }

  return course;
}

function isEnrolled(store: DemoStore, courseId: string) {
  return store.enrollments.some((enrollment) => enrollment.courseId === courseId);
}

function getCourseLessons(course: DemoCourseTemplate) {
  return course.sections.flatMap((section) =>
    section.lessons.map((lesson) => ({ lesson, section })),
  );
}

function buildCourseProgress(store: DemoStore, course: DemoCourseTemplate): CourseProgress {
  const completedLessonIds = new Set(store.completedLessonIds);
  const lessons = getCourseLessons(course);
  const completedLessons = lessons.filter(({ lesson }) => completedLessonIds.has(lesson.id)).length;
  const totalLessons = lessons.length;

  return {
    courseId: course.id,
    completedLessons,
    totalLessons,
    percentComplete: totalLessons === 0 ? 0 : (completedLessons / totalLessons) * 100,
    sections: course.sections.map((section) => ({
      sectionId: section.id,
      title: section.title,
      displayOrder: section.displayOrder,
      lessons: section.lessons.map((lesson) => ({
        lessonId: lesson.id,
        title: lesson.title,
        lessonType: lesson.lessonType,
        durationMinutes: lesson.estimatedDurationMinutes,
        displayOrder: lesson.displayOrder,
        preview: lesson.preview,
        completed: completedLessonIds.has(lesson.id),
        completedAt: completedLessonIds.has(lesson.id) ? "2026-05-29T10:00:00Z" : null,
      })),
    })),
  };
}

function ensureCompletionCertificate(store: DemoStore, courseId: string) {
  const existingCertificate = store.certificates.some((certificate) => certificate.courseId === courseId);

  if (existingCertificate) {
    return store;
  }

  const course = getCourseTemplate(courseId);
  const progress = buildCourseProgress(store, course);

  if (progress.totalLessons === 0 || progress.completedLessons < progress.totalLessons) {
    return store;
  }

  // Demo mode does not include the exam engine, so it issues a synthetic certificate only after
  // every lesson is completed to keep the certificate screen explorable without a backend.
  const session = requireSession(store);
  const newCertificate: DemoCertificateRecord = {
    id: `certificate-${courseId}`,
    courseId,
    certificateNumber: `EDU-DEMO-${new Date().getFullYear()}-${String(store.certificates.length + 1).padStart(4, "0")}`,
    learnerName: session.displayName,
    teacherName: "EduLife Demo",
    courseTitle: course.title,
    courseLevel: "All Levels",
    issuedAt: new Date().toISOString(),
    verificationHash: `demo-certificate-${courseId}`,
    source: "demo",
  };

  return {
    ...store,
    certificates: [...store.certificates, newCertificate],
  };
}

export function isDemoModeEnabled() {
  return true;
}

export function getDemoSession() {
  return readStore().session;
}

export async function demoLogin(email: string, _password: string) {
  const normalizedEmail = email.trim().toLowerCase() || "demo@edulife.app";
  const displayName = normalizedEmail.split("@")[0].replace(/[._-]+/g, " ").trim();

  let role = "STUDENT";
  if (normalizedEmail === "admin@edulife.test") {
    role = "ADMIN";
  } else if (normalizedEmail === "teacher@edulife.test") {
    role = "TEACHER";
  } else if (normalizedEmail === "groupadmin@edulife.test") {
    role = "GROUP_ADMIN";
  }

  const store = updateStore((current) => ({
    ...current,
    session: {
      userId: "demo-user-001",
      role,
      email: normalizedEmail,
      displayName:
        displayName
          .split(" ")
          .filter(Boolean)
          .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
          .join(" ") || "Demo Learner",
    },
  }));

  return store.session!;
}

export async function demoRegister(input: { name: string; email: string; password: string }) {
  const normalizedName = input.name.trim() || "Demo Learner";

  updateStore((current) => ({
    ...current,
    session: null,
    profile: {
      ...current.profile,
    },
  }));

  return {
    message: `Demo account ready for ${normalizedName}. Use ${input.email.trim() || "demo@edulife.app"} on the sign-in screen to continue.`,
  };
}

export async function demoLogout() {
  updateStore((current) => ({
    ...current,
    session: null,
  }));
}

export async function demoSyncAuth() {
  const session = requireSession();

  return {
    userId: session.userId,
    role: session.role,
  };
}

export async function demoGetProfile(): Promise<Profile> {
  const store = readStore();
  const session = requireSession(store);
  const enrolledCourses = store.enrollments.length;
  const completedLessons = store.completedLessonIds.length;
  const certificates = store.certificates.length;

  return {
    userId: session.userId,
    email: session.email,
    displayName: session.displayName,
    bio: store.profile.bio,
    avatarUrl: store.profile.avatarUrl,
    enrolledCourses,
    completedLessons,
    certificates,
  };
}

export async function demoListCourses(query: {
  q?: string;
  category?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<CourseDetail>> {
  const normalizedQuery = query.q?.trim().toLowerCase() ?? "";
  const normalizedCategory = query.category?.trim().toLowerCase();
  const page = Math.max(query.page ?? 0, 0);
  const size = Math.max(query.size ?? demoCourses.length, 1);

  const filteredCourses = demoCourses.filter((course) => {
    const matchesQuery =
      normalizedQuery.length === 0 ||
      course.title.toLowerCase().includes(normalizedQuery) ||
      course.shortDescription.toLowerCase().includes(normalizedQuery) ||
      course.description.toLowerCase().includes(normalizedQuery);
    const matchesCategory =
      !normalizedCategory || course.level.toLowerCase() === normalizedCategory;

    return matchesQuery && matchesCategory;
  });

  const start = page * size;
  const content = filteredCourses.slice(start, start + size).map((course) => ({
    id: course.id,
    slug: course.slug,
    title: course.title,
    shortDescription: course.shortDescription,
    description: course.description,
    level: course.level,
    languageCode: course.languageCode,
    imageUrl: course.imageUrl,
    publishedAt: course.publishedAt,
    sections: course.sections.map((section) => ({
      id: section.id,
      title: section.title,
      description: section.description,
      displayOrder: section.displayOrder,
      lessons: section.lessons.map((lesson) => ({
        id: lesson.id,
        title: lesson.title,
        summary: lesson.summary,
        lessonType: lesson.lessonType,
        estimatedDurationMinutes: lesson.estimatedDurationMinutes,
        displayOrder: lesson.displayOrder,
        preview: lesson.preview,
      })),
    })),
  }));

  return {
    content,
    totalPages: Math.max(Math.ceil(filteredCourses.length / size), 1),
    totalElements: filteredCourses.length,
    size,
    number: page,
    first: page === 0,
    last: start + size >= filteredCourses.length,
    empty: filteredCourses.length === 0,
  };
}

export async function demoGetCourseDetail(courseId: string): Promise<CourseDetail> {
  const course = getCourseTemplate(courseId);

  return {
    id: course.id,
    slug: course.slug,
    title: course.title,
    shortDescription: course.shortDescription,
    description: course.description,
    level: course.level,
    languageCode: course.languageCode,
    imageUrl: course.imageUrl,
    publishedAt: course.publishedAt,
    sections: course.sections.map((section) => ({
      id: section.id,
      title: section.title,
      description: section.description,
      displayOrder: section.displayOrder,
      lessons: section.lessons.map((lesson) => ({
        id: lesson.id,
        title: lesson.title,
        summary: lesson.summary,
        lessonType: lesson.lessonType,
        estimatedDurationMinutes: lesson.estimatedDurationMinutes,
        displayOrder: lesson.displayOrder,
        preview: lesson.preview,
      })),
    })),
  };
}

export async function demoListMyEnrollments(): Promise<EnrolledCourse[]> {
  const store = readStore();
  requireSession(store);

  return store.enrollments
    .map((enrollment) => {
      const course = getCourseTemplate(enrollment.courseId);

      return {
        enrollmentId: enrollment.enrollmentId,
        courseId: course.id,
        slug: course.slug,
        title: course.title,
        shortDescription: course.shortDescription,
        level: course.level,
        languageCode: course.languageCode,
        imageUrl: course.imageUrl,
        enrolledAt: enrollment.enrolledAt,
      };
    })
    .sort((left, right) => right.enrolledAt.localeCompare(left.enrolledAt));
}

export async function demoEnrollInCourse(courseId: string): Promise<EnrollmentResponse> {
  const store = updateStore((current) => {
    requireSession(current);
    getCourseTemplate(courseId);

    if (isEnrolled(current, courseId)) {
      return current;
    }

    return {
      ...current,
      enrollments: [
        ...current.enrollments,
        {
          enrollmentId: `enrollment-${courseId}`,
          courseId,
          enrolledAt: new Date().toISOString(),
          status: "ACTIVE",
        },
      ],
    };
  });

  const enrollment = store.enrollments.find((item) => item.courseId === courseId);

  if (!enrollment) {
    throw new Error("Demo enrollment could not be created.");
  }

  return {
    enrollmentId: enrollment.enrollmentId,
    courseId: enrollment.courseId,
    enrolledAt: enrollment.enrolledAt,
    status: enrollment.status,
  };
}

export async function demoUnenrollFromCourse(enrollmentId: string) {
  updateStore((current) => {
    requireSession(current);
    const enrollment = current.enrollments.find((item) => item.enrollmentId === enrollmentId);

    if (!enrollment) {
      return current;
    }

    const course = getCourseTemplate(enrollment.courseId);
    const courseLessonIds = new Set(getCourseLessons(course).map(({ lesson }) => lesson.id));

    return {
      ...current,
      enrollments: current.enrollments.filter((item) => item.enrollmentId !== enrollmentId),
      completedLessonIds: current.completedLessonIds.filter((lessonId) => !courseLessonIds.has(lessonId)),
      certificates: current.certificates.filter((certificate) => certificate.courseId !== enrollment.courseId),
    };
  });
}

export async function demoGetCourseProgress(courseId: string): Promise<CourseProgress> {
  const store = readStore();
  requireSession(store);

  if (!isEnrolled(store, courseId)) {
    throw new Error("Enroll in this demo course first to unlock progress.");
  }

  return buildCourseProgress(store, getCourseTemplate(courseId));
}

export async function demoGetLessonDetail(
  courseId: string,
  lessonId: string,
): Promise<LessonDetail> {
  const store = readStore();
  requireSession(store);
  const course = getCourseTemplate(courseId);
  const lessonRecord = course.sections
    .flatMap((section) => section.lessons.map((lesson) => ({ section, lesson })))
    .find((item) => item.lesson.id === lessonId);

  if (!lessonRecord) {
    throw new Error("Demo lesson not found.");
  }

  if (!lessonRecord.lesson.preview && !isEnrolled(store, courseId)) {
    throw new Error("Enroll in this course to unlock the full lesson in demo mode.");
  }

  return {
    lessonId: lessonRecord.lesson.id,
    courseId,
    sectionId: lessonRecord.section.id,
    sectionTitle: lessonRecord.section.title,
    title: lessonRecord.lesson.title,
    summary: lessonRecord.lesson.summary,
    lessonType: lessonRecord.lesson.lessonType,
    contentUrl: lessonRecord.lesson.contentUrl,
    contentBody: lessonRecord.lesson.contentBody || null,
    durationMinutes: lessonRecord.lesson.estimatedDurationMinutes,
    displayOrder: lessonRecord.lesson.displayOrder,
    preview: lessonRecord.lesson.preview,
    completed: store.completedLessonIds.includes(lessonId),
  };
}

export async function demoMarkLessonComplete(courseId: string, lessonId: string) {
  updateStore((current) => {
    requireSession(current);

    if (!isEnrolled(current, courseId)) {
      throw new Error("Enroll in the course before marking demo lessons complete.");
    }

    const course = getCourseTemplate(courseId);
    const lessonExists = getCourseLessons(course).some(({ lesson }) => lesson.id === lessonId);

    if (!lessonExists) {
      throw new Error("Demo lesson not found.");
    }

    const completedLessonIds = current.completedLessonIds.includes(lessonId)
      ? current.completedLessonIds
      : [...current.completedLessonIds, lessonId];

    return ensureCompletionCertificate({
      ...current,
      completedLessonIds,
    }, courseId);
  });
}

export async function demoListMyCertificates(): Promise<Certificate[]> {
  const store = readStore();
  requireSession(store);

  return [...store.certificates].sort((left, right) => right.issuedAt.localeCompare(left.issuedAt));
}

export async function demoGetCertificate(certificateId: string): Promise<CertificateDetail> {
  const store = readStore();
  const session = requireSession(store);

  const record = store.certificates.find((certificate) => certificate.id === certificateId);

  if (!record) {
    throw new Error("Certificate not found");
  }

  return {
    id: record.id,
    courseId: record.courseId,
    certificateNumber: record.certificateNumber,
    learnerName: session.displayName,
    teacherName: "EduLife Demo",
    courseTitle: record.courseTitle,
    courseLevel: "All Levels",
    issuedAt: record.issuedAt,
    verificationHash: `demo-${record.id}`,
    pdfUrl: null,
  };
}

export function demoVerifyCertificate(hash: string): Promise<CertificateVerification> {
  return Promise.resolve({
    learnerName: "Demo Learner",
    teacherName: "EduLife Platform",
    courseTitle: "French UI Vocabulary",
    courseLevel: "All Levels",
    issuedAt: "2026-05-22T15:30:00Z",
    certificateNumber: `EL-DEMO-${hash.slice(0, 8).toUpperCase()}`,
    verificationHash: hash,
    valid: true,
  });
}
