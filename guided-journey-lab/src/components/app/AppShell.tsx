import { useState, type ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import { Award, BookOpen, CheckCircle2, Compass, GraduationCap, Home, LayoutDashboard, LogOut, Menu, Moon, Shield, Sun, Users, X } from "lucide-react";
import { useDarkMode } from "@/hooks/use-dark-mode";
import { useAuth } from "@/lib/auth/auth-context";

interface ShellUser {
  displayName: string;
  email: string;
}

interface AppShellProps {
  active: "dashboard" | "courses" | "explore" | "certificates" | "level" | "teach" | "groups" | "approvals";
  user: ShellUser;
  onLogout: () => Promise<void> | void;
  header: ReactNode;
  children: ReactNode;
}

function getInitials(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

interface NavItem {
  key: AppShellProps["active"];
  label: string;
  to: "/dashboard" | "/courses" | "/explore" | "/certificates" | "/level" | "/teach" | "/groups" | "/approvals";
  icon: typeof Home;
}

// Sidebar entries mirror what each backend role can actually do: learners follow the
// enroll → learn → exam → certificate flow, teachers author course content through the
// CMS, and group admins manage cohorts (members + assigned courses) — not course authoring.
const LEARNER_NAV: NavItem[] = [
  { key: "dashboard", label: "Home", to: "/dashboard", icon: Home },
  { key: "courses", label: "My Courses", to: "/courses", icon: BookOpen },
  { key: "explore", label: "Explore", to: "/explore", icon: Compass },
  { key: "certificates", label: "Certificates", to: "/certificates", icon: Award },
  { key: "level", label: "Level & Progress", to: "/level", icon: Shield },
];

const TEACHER_NAV: NavItem[] = [
  { key: "teach", label: "Teaching Studio", to: "/teach", icon: LayoutDashboard },
  { key: "groups", label: "My Cohorts", to: "/groups", icon: Users },
  { key: "explore", label: "Course Catalog", to: "/explore", icon: Compass },
];

const GROUP_ADMIN_NAV: NavItem[] = [
  { key: "groups", label: "My Groups", to: "/groups", icon: Users },
  { key: "approvals", label: "Course Approvals", to: "/approvals", icon: CheckCircle2 },
  { key: "explore", label: "Course Catalog", to: "/explore", icon: Compass },
];

function getPortalForRole(role: string | undefined): { label: string; nav: NavItem[] } {
  switch (role) {
    case "TEACHER":
      return { label: "Teacher portal", nav: TEACHER_NAV };
    case "GROUP_ADMIN":
      return { label: "Group admin portal", nav: GROUP_ADMIN_NAV };
    case "ADMIN":
      // Admins normally live in AdminShell; if they land on a learner route they get
      // the full learner nav so nothing is hidden from them.
      return { label: "Admin preview", nav: LEARNER_NAV };
    default:
      return { label: "Learner portal", nav: LEARNER_NAV };
  }
}

export function AppShell({ active, user, onLogout, header, children }: AppShellProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { dark, toggle: toggleDark } = useDarkMode();
  const auth = useAuth();
  const portal = getPortalForRole(auth.session?.role);

  const userInitials = getInitials(user.displayName || user.email || "EL");

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {sidebarOpen && (
        <button
          type="button"
          className="fixed inset-0 z-30 bg-foreground/20 backdrop-blur-sm md:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-label="Close menu"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-border/60 bg-surface-elevated transition-transform duration-300 md:static md:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
        style={{ boxShadow: "var(--shadow-luxury)" }}
      >
        <div className="group flex h-16 items-center gap-3 border-b border-border/60 px-6">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-teal text-teal-foreground shadow-soft">
            <GraduationCap className="h-4 w-4" />
          </span>
          <div className="opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-within:opacity-100">
            <p className="text-display text-lg leading-none text-foreground">EduLife</p>
            <p className="mt-1 text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
              {portal.label}
            </p>
          </div>
          <button
            type="button"
            className="ml-auto text-muted-foreground transition-colors hover:text-foreground md:hidden"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close menu"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <nav className="flex-1 px-4 py-6">
          <p className="px-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
            Navigation
          </p>
          <div className="mt-3 space-y-1">
            {portal.nav.map(({ key, label, to, icon: Icon }) => {
              const isActive = key === active;

              return (
                <Link
                  key={key}
                  to={to}
                  className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                    isActive
                      ? "bg-primary/10 text-primary shadow-soft"
                      : "text-muted-foreground hover:bg-accent/70 hover:text-foreground"
                  }`}
                  onClick={() => setSidebarOpen(false)}
                >
                  <Icon className="h-4 w-4" />
                  <span>{label}</span>
                </Link>
              );
            })}
          </div>
        </nav>

        <div className="border-t border-border/60 p-4">
          <div className="flex items-center gap-3">
            <Link
              to="/profile"
              className="grid h-10 w-10 place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground"
              aria-label="Open profile"
              onClick={() => setSidebarOpen(false)}
            >
              {userInitials}
            </Link>
            <Link
              to="/profile"
              className="min-w-0 flex-1"
              onClick={() => setSidebarOpen(false)}
            >
              <p className="truncate text-sm font-medium text-foreground">{user.displayName}</p>
              <p className="truncate text-xs text-muted-foreground">{user.email}</p>
            </Link>
            <button
              type="button"
              className="text-muted-foreground transition-colors hover:text-foreground"
              onClick={() => void onLogout()}
              aria-label="Log out"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 border-b border-border/60 bg-surface-elevated/90 backdrop-blur-md">
          <div className="flex h-16 items-center gap-4 px-4 sm:px-6">
            <button
              type="button"
              className="text-muted-foreground transition-colors hover:text-foreground md:hidden"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open menu"
            >
              <Menu className="h-5 w-5" />
            </button>
            <div className="min-w-0 flex-1">{header}</div>
            <button
              type="button"
              onClick={toggleDark}
              className="grid h-8 w-8 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
              aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
            >
              {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>
          </div>
        </header>

        <main className="flex-1 px-4 py-8 sm:px-6 lg:px-8">{children}</main>
      </div>
    </div>
  );
}
