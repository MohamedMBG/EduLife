import { useState, type ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import {
  Award,
  BarChart3,
  BookOpen,
  BrainCircuit,
  CalendarDays,
  CheckCircle2,
  Compass,
  GraduationCap,
  Home,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Shield,
  Sun,
  Users,
  X,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { useDarkMode } from "@/hooks/use-dark-mode";
import { useAuth } from "@/lib/auth/auth-context";
import { motion } from "framer-motion";

interface ShellUser {
  displayName: string;
  email: string;
}

interface AppShellProps {
  active:
    | "dashboard"
    | "advisor"
    | "courses"
    | "explore"
    | "certificates"
    | "level"
    | "teach"
    | "groups"
    | "approvals"
    | "planner"
    | "analytics";
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
  to:
    | "/dashboard"
    | "/advisor"
    | "/courses"
    | "/explore"
    | "/certificates"
    | "/level"
    | "/teach"
    | "/groups"
    | "/approvals"
    | "/planner"
    | "/analytics";
  icon: typeof Home;
}

// Sidebar entries mirror what each backend role can actually do: learners follow the
// enroll → learn → exam → certificate flow, teachers author course content through the
// CMS, and group admins manage cohorts (members + assigned courses) — not course authoring.
const LEARNER_NAV: NavItem[] = [
  { key: "dashboard", label: "Home", to: "/dashboard", icon: Home },
  { key: "advisor", label: "Career Advisor", to: "/advisor", icon: BrainCircuit },
  { key: "planner", label: "Study Planner", to: "/planner", icon: CalendarDays },
  { key: "analytics", label: "Analytics", to: "/analytics", icon: BarChart3 },
  { key: "courses", label: "My Courses", to: "/courses", icon: BookOpen },
  { key: "explore", label: "Explore", to: "/explore", icon: Compass },
  { key: "certificates", label: "Certificates", to: "/certificates", icon: Award },
  { key: "level", label: "Level & Progress", to: "/level", icon: Shield },
];

const TEACHER_NAV: NavItem[] = [
  { key: "teach", label: "Teaching Studio", to: "/teach", icon: LayoutDashboard },
  { key: "analytics", label: "Analytics", to: "/analytics", icon: BarChart3 },
  { key: "groups", label: "My Cohorts", to: "/groups", icon: Users },
  { key: "explore", label: "Course Catalog", to: "/explore", icon: Compass },
];

const GROUP_ADMIN_NAV: NavItem[] = [
  { key: "groups", label: "My Groups", to: "/groups", icon: Users },
  { key: "analytics", label: "Analytics", to: "/analytics", icon: BarChart3 },
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
  const [isCollapsed, setIsCollapsed] = useState(false);
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
        className={`fixed inset-y-0 left-0 z-40 flex flex-col border-r border-border/70 bg-surface-elevated/95 backdrop-blur-md transition-all duration-300 md:static md:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        } ${isCollapsed ? "md:w-20 w-72" : "w-72"}`}
        style={{ boxShadow: "var(--shadow-luxury)" }}
      >
        <div
          className={`flex h-16 items-center border-b border-border/70 px-6 ${isCollapsed ? "md:px-4 md:justify-center" : "gap-3"}`}
        >
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-bezel">
            <GraduationCap className="h-4.5 w-4.5" strokeWidth={1.5} />
          </span>
          {!isCollapsed && (
            <div className="transition-opacity duration-200">
              <p className="text-display text-lg leading-none text-foreground">EduLife</p>
              <p className="mt-1 text-[10px] uppercase tracking-[0.18em] text-muted-foreground truncate max-w-[140px]">
                {portal.label}
              </p>
            </div>
          )}
          <button
            type="button"
            className={`hidden md:grid h-8 w-8 place-items-center rounded-lg border border-border/70 hover:bg-accent/45 text-muted-foreground hover:text-primary transition-colors cursor-pointer ${isCollapsed ? "mx-auto" : "ml-auto"}`}
            onClick={() => setIsCollapsed(!isCollapsed)}
            aria-label={isCollapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {isCollapsed ? (
              <ChevronRight className="h-4.5 w-4.5" />
            ) : (
              <ChevronLeft className="h-4.5 w-4.5" />
            )}
          </button>
          <button
            type="button"
            className="ml-auto text-muted-foreground transition-colors hover:text-foreground md:hidden"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close menu"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <nav className={`flex-1 py-6 ${isCollapsed ? "px-2" : "px-4"}`}>
          {!isCollapsed && (
            <p className="px-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground mb-3">
              Navigation
            </p>
          )}
          <div className={`space-y-1 ${isCollapsed ? "flex flex-col items-center" : ""}`}>
            {portal.nav.map(({ key, label, to, icon: Icon }) => {
              const isActive = key === active;

              return (
                <Link
                  key={key}
                  to={to}
                  title={isCollapsed ? label : undefined}
                  className={`flex items-center rounded-xl py-2.5 text-sm font-medium transition-all duration-300 ${
                    isCollapsed ? "w-12 h-12 justify-center" : "w-full px-3 gap-3"
                  } ${
                    isActive
                      ? "bg-accent/55 text-primary shadow-bezel ring-1 ring-primary/15"
                      : "text-muted-foreground hover:bg-accent/35 hover:text-primary"
                  }`}
                  onClick={() => setSidebarOpen(false)}
                >
                  <Icon className="h-4.5 w-4.5 shrink-0" />
                  {!isCollapsed && <span>{label}</span>}
                </Link>
              );
            })}
          </div>
        </nav>

        <div className={`border-t border-border/70 ${isCollapsed ? "p-2 py-4" : "p-4"}`}>
          <div
            className={`flex items-center ${isCollapsed ? "flex-col gap-4 justify-center" : "gap-3"}`}
          >
            <Link
              to="/profile"
              className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground shadow-bezel"
              aria-label="Open profile"
              onClick={() => setSidebarOpen(false)}
            >
              {userInitials}
            </Link>
            {!isCollapsed && (
              <Link to="/profile" className="min-w-0 flex-1" onClick={() => setSidebarOpen(false)}>
                <p className="truncate text-sm font-medium text-foreground">{user.displayName}</p>
                <p className="truncate text-xs text-muted-foreground">{user.email}</p>
              </Link>
            )}
            <button
              type="button"
              className={`text-muted-foreground transition-colors hover:text-primary cursor-pointer ${isCollapsed ? "grid h-10 w-10 place-items-center rounded-xl hover:bg-destructive/10 hover:text-destructive" : ""}`}
              onClick={() => void onLogout()}
              aria-label="Log out"
            >
              <LogOut className="h-4.5 w-4.5" />
            </button>
          </div>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 border-b border-border/70 bg-surface-elevated/86 backdrop-blur-md">
          <div className="flex h-16 items-center gap-4 px-4 sm:px-6">
            <button
              type="button"
              className="text-muted-foreground transition-colors hover:text-primary md:hidden"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open menu"
            >
              <Menu className="h-5 w-5" />
            </button>
            <div className="min-w-0 flex-1">{header}</div>
            <button
              type="button"
              onClick={toggleDark}
              className="grid h-9 w-9 place-items-center rounded-full border border-border/70 bg-surface text-muted-foreground transition-colors hover:bg-accent/45 hover:text-primary"
              aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
            >
              {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>
          </div>
        </header>

        <main className="flex-1 px-4 py-7 sm:px-6 lg:px-8">
          <div className="mx-auto w-full max-w-7xl">{children}</div>
        </main>
      </div>
    </div>
  );
}
