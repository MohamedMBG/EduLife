import {
  createContext,
  startTransition,
  useContext,
  useEffect,
  useEffectEvent,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useLocation, useNavigate } from "@tanstack/react-router";
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

const REGISTERABLE_ROLES: ReadonlySet<UserRole> = new Set(["LEARNER", "TEACHER", "GROUP_ADMIN"]);

function readStoredIntendedRole(): UserRole | undefined {
  const stored = localStorage.getItem(INTENDED_ROLE_KEY);

  if (!stored) {
    return undefined;
  }

  return REGISTERABLE_ROLES.has(stored as UserRole) ? (stored as UserRole) : undefined;
}

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
  clearError: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function getDisplayName(user: FirebaseUser) {
  return user.displayName?.trim() || user.email || "EduLife learner";
}

export function getReadableAuthError(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  if (error && typeof error === "object" && "code" in error) {
    switch (String(error.code)) {
      case "auth/invalid-credential":
      case "auth/user-not-found":
      case "auth/wrong-password":
        return "The email or password is incorrect.";
      case "auth/invalid-email":
        return "Enter a valid email address.";
      case "auth/user-disabled":
        return "This account has been disabled. Contact support.";
      case "auth/email-already-in-use":
        return "This email address already has an EduLife account.";
      case "auth/weak-password":
        return "Use a stronger password with at least 8 characters.";
      case "auth/too-many-requests":
        return "Too many attempts. Wait a moment before trying again.";
      case "auth/network-request-failed":
        return "Network error. Check your connection and try again.";
      case "auth/operation-not-allowed":
        return "Email/password sign-in is not enabled. Contact support.";
      case "auth/missing-password":
        return "Enter your password.";
      default:
        return "Authentication failed. Please try again.";
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Authentication failed. Please try again.";
}

class UnverifiedEmailError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "UnverifiedEmailError";
  }
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

  // Track the last Firebase uid we synced so background remounts (HMR, StrictMode double-invoke,
  // tab refocus) do not re-hit the rate-limited /auth/sync endpoint. Token refreshes are handled
  // separately by the client's 401-retry path using getIdToken(forceRefresh).
  const syncedUidRef = useRef<string | null>(null);

  const hydrateSession = useEffectEvent(async (firebaseUser: FirebaseUser) => {
    if (!firebaseUser.emailVerified) {
      // The backend rejects unverified learners on every protected endpoint, so the web app
      // clears the browser session early and keeps the user on the verification step.
      const auth = await getFirebaseAuth();
      await auth.signOut();
      syncedUidRef.current = null;
      commitAnonymous("Email is not verified. Check your inbox before signing in.");
      return;
    }

    if (syncedUidRef.current === firebaseUser.uid && session) {
      // Same user re-emitted by Firebase (token refresh, remount, etc.) — keep existing
      // session instead of re-calling /auth/sync.
      return;
    }

    startTransition(() => {
      setStatus("loading");
      setError(null);
    });

    const storedRole = readStoredIntendedRole();
    let sync: Awaited<ReturnType<typeof syncAuth>>;
    try {
      sync = await syncAuth(
        async (forceRefresh) => firebaseUser.getIdToken(forceRefresh),
        storedRole,
      );
    } catch (syncError) {
      syncedUidRef.current = null;
      commitAnonymous(getReadableAuthError(syncError));
      return;
    }
    // Clear after first use — subsequent syncs must not re-apply the registration intent.
    localStorage.removeItem(INTENDED_ROLE_KEY);

    syncedUidRef.current = firebaseUser.uid;

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

        // onAuthStateChanged fires only on sign-in/sign-out — NOT on background token refreshes.
        // Using onIdTokenChanged here would call /auth/sync on every hourly refresh and on every
        // remount, draining the backend's 30/min rate limit on the endpoint.
        unsubscribe = firebaseAuth.onAuthStateChanged(auth, (firebaseUser) => {
          if (cancelled) {
            return;
          }

          if (!firebaseUser) {
            syncedUidRef.current = null;
            commitAnonymous();
            return;
          }

          void hydrateSession(firebaseUser).catch((nextError) => {
            syncedUidRef.current = null;
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

    const trimmedEmail = email.trim();

    if (appEnv.demoMode) {
      try {
        const demoSession = await demoLogin(trimmedEmail, password);
        commitAuthenticated(demoSession);
      } catch (nextError) {
        throw new Error(getReadableAuthError(nextError));
      }
      return;
    }

    if (configError) {
      throw new Error(configError);
    }

    try {
      const auth = await getFirebaseAuth();
      const firebaseAuth = await getFirebaseAuthModule();
      const credential = await firebaseAuth.signInWithEmailAndPassword(
        auth,
        trimmedEmail,
        password,
      );

      if (!credential.user.emailVerified) {
        // Resend verification so a learner is not stuck after forgetting the first message.
        await firebaseAuth.sendEmailVerification(credential.user).catch(() => undefined);
        await auth.signOut();
        throw new UnverifiedEmailError(
          "Email is not verified. A fresh verification link has been sent.",
        );
      }

      // hydrateSession runs from onIdTokenChanged once Firebase emits the new credential. Calling
      // it here as well would race two concurrent /auth/sync requests and trip the firebase_uid
      // unique constraint, so the listener is the single source of truth.
    } catch (nextError) {
      if (nextError instanceof UnverifiedEmailError) {
        throw nextError;
      }
      throw new Error(getReadableAuthError(nextError));
    }
  }

  async function register(input: RegisterInput) {
    setError(null);

    const trimmedEmail = input.email.trim();
    const trimmedName = input.name.trim();

    if (appEnv.demoMode) {
      try {
        const result = await demoRegister({ ...input, email: trimmedEmail, name: trimmedName });
        commitAnonymous(result.message);
      } catch (nextError) {
        throw new Error(getReadableAuthError(nextError));
      }
      return;
    }

    if (configError) {
      throw new Error(configError);
    }

    try {
      const auth = await getFirebaseAuth();
      const firebaseAuth = await getFirebaseAuthModule();
      const credential = await firebaseAuth.createUserWithEmailAndPassword(
        auth,
        trimmedEmail,
        input.password,
      );

      if (trimmedName) {
        await firebaseAuth.updateProfile(credential.user, { displayName: trimmedName });
      }

      // Persist intended role so it is passed to /auth/sync after email verification and sign-in.
      if (
        input.intendedRole &&
        input.intendedRole !== "LEARNER" &&
        REGISTERABLE_ROLES.has(input.intendedRole)
      ) {
        localStorage.setItem(INTENDED_ROLE_KEY, input.intendedRole);
      }

      await firebaseAuth.sendEmailVerification(credential.user);
      await auth.signOut();
      commitAnonymous(
        "Account created. Check your inbox, verify your email, then sign in to sync your EduLife profile.",
      );
    } catch (nextError) {
      throw new Error(getReadableAuthError(nextError));
    }
  }

  function clearError() {
    setError(null);
  }

  async function logout() {
    syncedUidRef.current = null;

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
        clearError,
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
  const location = useLocation();

  useEffect(() => {
    if (auth.status === "anonymous" && auth.configured) {
      // Carry the original path so login can return the user to the deep link they wanted.
      navigate({ to: "/login", search: { redirect: location.href } });
    }
  }, [auth.configured, auth.status, navigate, location.href]);

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

// Course authoring is a teacher activity; group admins manage cohorts, not content
// (see AGENTS.md role matrix), so they are routed to /groups instead.
const COURSE_AUTHOR_ROLES: ReadonlySet<string> = new Set(["TEACHER", "ADMIN"]);
const GROUP_MANAGER_ROLES: ReadonlySet<string> = new Set(["TEACHER", "GROUP_ADMIN", "ADMIN"]);
const COURSE_APPROVER_ROLES: ReadonlySet<string> = new Set(["GROUP_ADMIN", "ADMIN"]);

function RequireRole({
  allowed,
  fallbackTo,
  loadingMessage,
  children,
}: {
  allowed: ReadonlySet<string>;
  fallbackTo: "/dashboard" | "/groups";
  loadingMessage: string;
  children: ReactNode;
}) {
  const auth = useAuth();
  const navigate = useNavigate();
  const role = auth.session?.role;

  useEffect(() => {
    if (auth.status === "anonymous" && auth.configured) {
      navigate({ to: "/login" });
    } else if (auth.status === "authenticated" && role && !allowed.has(role)) {
      // The backend would 403 these endpoints anyway; keep each role in its own portal.
      navigate({ to: fallbackTo });
    }
  }, [allowed, auth.configured, auth.status, fallbackTo, navigate, role]);

  if (auth.status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4">
        <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-5 text-center shadow-elevated">
          <p className="text-sm font-medium text-foreground">{loadingMessage}</p>
        </div>
      </div>
    );
  }

  if (auth.status !== "authenticated" || !role || !allowed.has(role)) {
    return null;
  }

  return <>{children}</>;
}

export function RequireTeacher({ children }: { children: ReactNode }) {
  return (
    <RequireRole
      allowed={COURSE_AUTHOR_ROLES}
      fallbackTo="/dashboard"
      loadingMessage="Loading your teaching session…"
    >
      {children}
    </RequireRole>
  );
}

export function RequireGroupManager({ children }: { children: ReactNode }) {
  return (
    <RequireRole
      allowed={GROUP_MANAGER_ROLES}
      fallbackTo="/dashboard"
      loadingMessage="Loading your groups…"
    >
      {children}
    </RequireRole>
  );
}

export function RequireCourseApprover({ children }: { children: ReactNode }) {
  return (
    <RequireRole
      allowed={COURSE_APPROVER_ROLES}
      fallbackTo="/dashboard"
      loadingMessage="Loading course approvals…"
    >
      {children}
    </RequireRole>
  );
}

export function RequireAdmin({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (auth.status === "anonymous" && auth.configured) {
      navigate({ to: "/login" });
    } else if (auth.status === "authenticated" && auth.session?.role !== "ADMIN") {
      navigate({ to: "/dashboard" });
    }
  }, [auth.configured, auth.session?.role, auth.status, navigate]);

  if (auth.status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4">
        <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-5 text-center shadow-elevated">
          <p className="text-sm font-medium text-foreground">Loading admin session…</p>
        </div>
      </div>
    );
  }

  if (auth.status !== "authenticated" || auth.session?.role !== "ADMIN") {
    return null;
  }

  return <>{children}</>;
}
