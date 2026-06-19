const requiredEnvVars = [
  "VITE_API_BASE_URL",
  "VITE_FIREBASE_API_KEY",
  "VITE_FIREBASE_AUTH_DOMAIN",
  "VITE_FIREBASE_PROJECT_ID",
  "VITE_FIREBASE_APP_ID",
  "VITE_FIREBASE_MESSAGING_SENDER_ID",
] as const;

type RequiredEnvVar = (typeof requiredEnvVars)[number];

function readEnv(name: RequiredEnvVar) {
  return import.meta.env[name]?.trim() ?? "";
}

function readOptionalEnv(name: string) {
  return import.meta.env[name]?.trim() ?? "";
}

function isTruthyFlag(value: string) {
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}

const demoMode = isTruthyFlag(readOptionalEnv("VITE_DEMO_MODE"));

export const appEnv = {
  demoMode,
  advisorAiEnabled: isTruthyFlag(readOptionalEnv("VITE_ADVISOR_AI_ENABLED")),
  apiBaseUrl: readEnv("VITE_API_BASE_URL").replace(/\/+$/, ""),
  firebase: {
    apiKey: readEnv("VITE_FIREBASE_API_KEY"),
    authDomain: readEnv("VITE_FIREBASE_AUTH_DOMAIN"),
    projectId: readEnv("VITE_FIREBASE_PROJECT_ID"),
    appId: readEnv("VITE_FIREBASE_APP_ID"),
    messagingSenderId: readEnv("VITE_FIREBASE_MESSAGING_SENDER_ID"),
    storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET?.trim() || undefined,
  },
};

export function getMissingEnvVars() {
  if (demoMode) {
    return [];
  }

  return requiredEnvVars.filter((name) => readEnv(name).length === 0);
}

// Cleartext HTTP is only acceptable for local development hosts. A production API base URL must
// be HTTPS, because the Firebase ID token rides on every request (audit 2026-06-19 P2-3).
const LOCAL_DEV_HOSTS: ReadonlySet<string> = new Set([
  "localhost",
  "127.0.0.1",
  "0.0.0.0",
  "[::1]",
  "::1",
]);

export function getInsecureApiBaseUrlError(): string | null {
  if (demoMode) {
    return null;
  }

  const raw = appEnv.apiBaseUrl;

  if (!raw) {
    // Missing value is reported by the required-vars check instead.
    return null;
  }

  let parsed: URL;
  try {
    parsed = new URL(raw);
  } catch {
    return `Invalid VITE_API_BASE_URL: "${raw}" is not a valid URL.`;
  }

  if (parsed.protocol === "https:") {
    return null;
  }

  if (parsed.protocol === "http:" && LOCAL_DEV_HOSTS.has(parsed.hostname)) {
    return null;
  }

  return "VITE_API_BASE_URL must use HTTPS in production. Cleartext HTTP is only allowed for local development hosts (localhost / 127.0.0.1).";
}

export function getEnvConfigurationError() {
  const missing = getMissingEnvVars();

  if (missing.length === 0) {
    return getInsecureApiBaseUrlError();
  }

  return `Missing website environment values: ${missing.join(", ")}. Copy guided-journey-lab/.env.example into a local .env file and fill in the Firebase + backend settings, or set VITE_DEMO_MODE=true to run the standalone demo.`;
}
