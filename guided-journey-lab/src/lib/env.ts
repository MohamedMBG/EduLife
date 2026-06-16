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

export function getEnvConfigurationError() {
  const missing = getMissingEnvVars();

  if (missing.length === 0) {
    return null;
  }

  return `Missing website environment values: ${missing.join(", ")}. Copy guided-journey-lab/.env.example into a local .env file and fill in the Firebase + backend settings, or set VITE_DEMO_MODE=true to run the standalone demo.`;
}
