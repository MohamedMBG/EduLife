import { initializeApp, type FirebaseApp } from "firebase/app";
import { appEnv, getEnvConfigurationError } from "../env";

let firebaseApp: FirebaseApp | null = null;

export function getFirebaseConfigurationError() {
  return getEnvConfigurationError();
}

export function getFirebaseApp() {
  const configError = getFirebaseConfigurationError();

  if (configError) {
    throw new Error(configError);
  }

  if (!firebaseApp) {
    firebaseApp = initializeApp(appEnv.firebase);
  }

  return firebaseApp;
}

let authPromise: Promise<import("firebase/auth").Auth> | null = null;

export async function getFirebaseAuth() {
  if (typeof window === "undefined") {
    throw new Error("Firebase Auth is only available in the browser.");
  }

  if (!authPromise) {
    authPromise = import("firebase/auth").then(async (firebaseAuth) => {
      const auth = firebaseAuth.getAuth(getFirebaseApp());

      // Session persistence keeps tokens out of localStorage per CLAUDE.md security rule.
      // Learner stays signed in across reloads in the same tab; closing the tab clears state.
      await firebaseAuth.setPersistence(auth, firebaseAuth.browserSessionPersistence);
      return auth;
    });
  }

  return authPromise;
}

export function getFirebaseAuthModule() {
  return import("firebase/auth");
}
