import {
  createContext,
  startTransition,
  useContext,
  useEffect,
  useEffectEvent,
  useState,
  type ReactNode,
} from "react";
import { useNavigate } from "@tanstack/react-router";
import type { User as FirebaseUser } from "firebase/auth";
import { ApiClientError, syncAuth } from "../api/client";
import type { UserRole } from "../api/types";
import { appEnv } from "../env";
import { demoLogin, demoLogout, demoRegister, getDemoSession } from "../api/demo";
import { getFirebaseAuth, getFirebaseAuthModule, getFirebaseConfigurationError } from "./firebase";

export interface AuthSession {
  userId: string;
  role: string;
  email: string;
  displayName: string;
}

type AuthStatus = "loading" | "authenticated" | "anonymous";

const INTENDED_ROLE_KEY = "edulife_intended_role";

interface RegisterInput {
  name: string;
  email: string;
  password: string;
  intendedRole?: UserRole;
}

interface AuthContextValue {
  status: AuthStatus;
  session: AuthSession | null;
  error: string | null;
  configured: boolean;
  getAccessToken: (forceRefresh?: boolean) => Promise<string | null>;
  login: (email: string, password: string) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function getDisplayName(user: FirebaseUser) {
  return user.displayName?.trim() || user.email || "EduLife learner";
}

function getReadableAuthError(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  if (error && typeof error === "object" && "code" in error) {
    switch (String(error.code)) {
      case "auth/invalid-credential":
      case "auth/user-not-found":
      case "auth/wrong-password":
        return "The email or password is incorrect.";
      case "auth/email-already-in-use":
        return "This email address already has an EduLife account.";
      case "auth/weak-password":
        return "Use a stronger password with at least 8 characters.";
      case "auth/too-many-requests":
        return "Too many attempts. Wait a moment before trying again.";
      default:
        return "Authentication failed. Please try again.";
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Authentication failed. Please try again.";
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const configError = appEnv.demoMode ? null : getFirebaseConfigurationError();
  const [status, setStatus] = useState<AuthStatus>(() => {
    if (appEnv.demoMode) {
      return getDemoSession() ? "authenticated" : "anonymous";
    }

    return configError ? "anonymous" : "loading";
  });
  const [session, setSession] = useState<AuthSession | null>(() => {
    if (!appEnv.demoMode) {
      return null;
    }

    return getDemoSession();
  });
  const [error, setError] = useState<string | null>(() => {
    if (appEnv.demoMode) {
      return null;
    }

    return configError;
  });

  const commitAnonymous = useEffectEvent((message: string | null = null) => {
    startTransition(() => {
      setStatus("anonymous");
      setSession(null);
      setError(message);
    });
  });

  const commitAuthenticated = useEffectEvent((nextSession: AuthSession) => {
    startTransition(() => {
      setStatus("authenticated");
      setSession(nextSession);
      setError(null);
    });
  });

  const hydrateSession = useEffectEvent(async (firebaseUser: FirebaseUser) => {
    startTransition(() => {
      setStatus("loading");
      setError(null);
    });

    if (!firebaseUser.emailVerified) {
      // The backend rejects unverified learners on every protected endpoint, so the web app
      // clears the browser session early and keeps the user on the verification step.
      const auth = await getFirebaseAuth();
      await auth.signOut();
      commitAnonymous("Email is not verified. Check your inbox before signing in.");
      return;
    }

    const storedRole = localStorage.getItem(INTENDED_ROLE_KEY) as UserRole | null;
    const sync = await syncAuth(
      async (forceRefresh) => firebaseUser.getIdToken(forceRefresh),
      storedRole ?? undefined,
    );
    // Clear after first use — subsequent syncs must not re-apply the registration intent.
    localStorage.removeItem(INTENDED_ROLE_KEY);

    commitAuthenticated({
      userId: sync.userId,
      role: sync.role,
      email: firebaseUser.email || "",
      displayName: getDisplayName(firebaseUser),
    });
  });

  useEffect(() => {
    if (appEnv.demoMode) {
      return;
    }

    if (configError || typeof window === "undefined") {
      return;
    }

    let unsubscribe = () => {};
    let cancelled = false;

    void (async () => {
      try {
        const auth = await getFirebaseAuth();
        const firebaseAuth = await getFirebaseAuthModule();

        unsubscribe = firebaseAuth.onIdTokenChanged(auth, (firebaseUser) => {
          if (cancelled) {
            return;
          }

          if (!firebaseUser) {
            commitAnonymous();
            return;
          }

          void hydrateSession(firebaseUser).catch((nextError) => {
            void auth.signOut().catch(() => undefined);
            commitAnonymous(getReadableAuthError(nextError));
          });
        });
      } catch (nextError) {
        commitAnonymous(getReadableAuthError(nextError));
      }
    })();

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [commitAnonymous, configError, hydrateSession]);

  async function login(email: string, password: string) {
    setError(null);

    if (appEnv.demoMode) {
      const demoSession = await demoLogin(email, password);
      commitAuthenticated(demoSession);
      return;
    }

    if (configError) {
      throw new Error(configError);
    }

    const auth = await getFirebaseAuth();
    const firebaseAuth = await getFirebaseAuthModule();
    const credential = await firebaseAuth.signInWithEmailAndPassword(auth, email, password);

    if (!credential.user.emailVerified) {
      // Resend verification so a learner is not stuck after forgetting the first message.
      await firebaseAuth.sendEmailVerification(credential.user).catch(() => undefined);
      await auth.signOut();
      commitAnonymous("Email is not verified. A fresh verification link has been sent.");
      return;
    }

    await hydrateSession(credential.user);
  }

  async function register(input: RegisterInput) {
    setError(null);

    if (appEnv.demoMode) {
      const result = await demoRegister(input);
      commitAnonymous(result.message);
      return;
    }

    if (configError) {
      throw new Error(configError);
    }

    const auth = await getFirebaseAuth();
    const firebaseAuth = await getFirebaseAuthModule();
    const credential = await firebaseAuth.createUserWithEmailAndPassword(
      auth,
      input.email,
      input.password,
    );

    if (input.name.trim()) {
      await firebaseAuth.updateProfile(credential.user, { displayName: input.name.trim() });
    }

    // Persist intended role so it is passed to /auth/sync after email verification and sign-in.
    if (input.intendedRole && input.intendedRole !== "LEARNER") {
      localStorage.setItem(INTENDED_ROLE_KEY, input.intendedRole);
    }

    await firebaseAuth.sendEmailVerification(credential.user);
    await auth.signOut();
    commitAnonymous(
      "Account created. Check your inbox, verify your email, then sign in to sync your EduLife profile.",
    );
  }

  async function logout() {
    if (appEnv.demoMode) {
      await demoLogout();
      commitAnonymous();
      return;
    }

    const auth = await getFirebaseAuth();
    await auth.signOut();
    commitAnonymous();
  }

  async function getAccessToken(forceRefresh = false) {
    if (appEnv.demoMode) {
      return "demo-access-token";
    }

    if (configError) {
      return null;
    }

    const auth = await getFirebaseAuth();
    return auth.currentUser ? auth.currentUser.getIdToken(forceRefresh) : null;
  }

  return (
    <AuthContext.Provider
      value={{
        status,
        session,
        error,
        configured: !configError,
        getAccessToken,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider.");
  }

  return context;
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (auth.status === "anonymous" && auth.configured) {
      navigate({ to: "/login" });
    }
  }, [auth.configured, auth.status, navigate]);

  if (auth.status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4">
        <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-5 text-center shadow-elevated">
          <p className="text-sm font-medium text-foreground">Loading your EduLife session...</p>
          <p className="mt-2 text-xs text-muted-foreground">
            {appEnv.demoMode
              ? "Preparing the standalone website demo."
              : "The website is syncing your Firebase identity with the backend."}
          </p>
        </div>
      </div>
    );
  }

  if (auth.status !== "authenticated") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4">
        <div className="max-w-lg rounded-3xl border border-border bg-surface-elevated px-6 py-5 text-center shadow-elevated">
          <p className="text-sm font-medium text-foreground">
            {auth.error || "Redirecting to the sign-in page..."}
          </p>
          <p className="mt-2 text-xs text-muted-foreground">
            {appEnv.demoMode
              ? "Demo mode still keeps learner routes behind a local sign-in so you can navigate the flow consistently."
              : "Protected learner routes stay behind Firebase auth so the web app matches the Android and backend security model."}
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
