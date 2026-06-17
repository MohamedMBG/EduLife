import { useState } from "react";
import { Link, useMatches } from "@tanstack/react-router";
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
  Search,
  Shield,
  Sun,
  Users,
  UserCircle2,
  X,
  type LucideIcon,
} from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";
import { useDarkMode } from "@/hooks/use-dark-mode";
import { useAuth } from "@/lib/auth/auth-context";
import { cn } from "@/lib/utils";

interface NavEntry {
  label: string;
  to: string;
  icon: LucideIcon;
  match: string[];
}

const LEARNER_NAV: NavEntry[] = [
  { label: "Dashboard", to: "/dashboard", icon: Home, match: ["/dashboard"] },
  { label: "Explore", to: "/explore", icon: Compass, match: ["/explore"] },
  { label: "My Courses", to: "/courses", icon: BookOpen, match: ["/courses", "/learn"] },
  { label: "Study Planner", to: "/planner", icon: CalendarDays, match: ["/planner"] },
  { label: "Career Advisor", to: "/advisor", icon: BrainCircuit, match: ["/advisor"] },
  { label: "Certificates", to: "/certificates", icon: Award, match: ["/certificates"] },
  { label: "Analytics", to: "/analytics", icon: BarChart3, match: ["/analytics"] },
  { label: "Level", to: "/level", icon: Shield, match: ["/level"] },
];

const TEACHER_NAV: NavEntry[] = [
  { label: "Teaching Studio", to: "/teach", icon: LayoutDashboard, match: ["/teach"] },
  { label: "Analytics", to: "/analytics", icon: BarChart3, match: ["/analytics"] },
  { label: "My Cohorts", to: "/groups", icon: Users, match: ["/groups"] },
  { label: "Course Catalog", to: "/explore", icon: Compass, match: ["/explore"] },
];

const GROUP_ADMIN_NAV: NavEntry[] = [
  { label: "My Groups", to: "/groups", icon: Users, match: ["/groups"] },
  { label: "Analytics", to: "/analytics", icon: BarChart3, match: ["/analytics"] },
  { label: "Approvals", to: "/approvals", icon: CheckCircle2, match: ["/approvals"] },
  { label: "Course Catalog", to: "/explore", icon: Compass, match: ["/explore"] },
];

const ADMIN_NAV: NavEntry[] = [
  {
    label: "Dashboard",
    to: "/admin/dashboard",
    icon: LayoutDashboard,
    match: ["/admin/dashboard"],
  },
  { label: "Analytics", to: "/admin/analytics", icon: BarChart3, match: ["/admin/analytics"] },
  {
    label: "Teacher Requests",
    to: "/admin/teacher-requests",
    icon: GraduationCap,
    match: ["/admin/teacher-requests"],
  },
  { label: "Course Catalog", to: "/explore", icon: Compass, match: ["/explore"] },
];

function getNavForRole(role: string | undefined): NavEntry[] {
  switch (role) {
    case "TEACHER":
      return TEACHER_NAV;
    case "GROUP_ADMIN":
      return GROUP_ADMIN_NAV;
    case "ADMIN":
      return ADMIN_NAV;
    default:
      return LEARNER_NAV;
  }
}

function getInitials(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function useActiveRoute(): string {
  const matches = useMatches();
  const last = matches[matches.length - 1];
  return last?.pathname ?? "/";
}

function isRouteActive(pathname: string, matchPatterns: string[]): boolean {
  return matchPatterns.some((pattern) => {
    if (pathname === pattern) return true;
    if (pathname.startsWith(pattern + "/")) return true;
    return false;
  });
}

interface AppTopNavProps {
  onSearch?: (query: string) => void;
  searchValue?: string;
  showSearch?: boolean;
}

export function AppTopNav({ onSearch, searchValue, showSearch = false }: AppTopNavProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState(searchValue ?? "");
  const { dark, toggle: toggleDark } = useDarkMode();
  const auth = useAuth();
  const pathname = useActiveRoute();

  const isAuthenticated = auth.status === "authenticated";
  const role = auth.session?.role;
  const nav = getNavForRole(role);
  const displayName = auth.session?.displayName || "EduLife user";
  const email = auth.session?.email || "";
  const initials = getInitials(displayName || email || "EL");

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (onSearch) {
      onSearch(searchQuery);
    } else if (searchQuery.trim()) {
      window.location.href = `/explore?q=${encodeURIComponent(searchQuery.trim())}`;
    }
  }

  return (
    <>
      <header className="sticky top-0 z-50 border-b border-border/70 bg-surface-elevated/90 backdrop-blur-xl">
        <nav
          className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8"
          aria-label="Primary navigation"
        >
          {/* Logo */}
          <Link
            to={isAuthenticated ? "/dashboard" : "/"}
            className="flex shrink-0 items-center gap-2.5"
          >
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-bezel">
              <GraduationCap className="h-4 w-4" strokeWidth={1.75} />
            </span>
            <span className="text-display text-lg tracking-tight text-foreground">EduLife</span>
          </Link>

          {/* Desktop nav links */}
          {isAuthenticated && (
            <div className="hidden items-center gap-1 md:flex">
              {nav.map((item) => {
                const active = isRouteActive(pathname, item.match);
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    className={cn(
                      "relative px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.14em] transition-colors duration-300",
                      active
                        ? "text-primary after:absolute after:inset-x-1 after:bottom-0 after:h-0.5 after:rounded-full after:bg-primary"
                        : "text-muted-foreground hover:text-foreground",
                    )}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </div>
          )}

          {/* Right section */}
          <div className="flex items-center gap-2">
            {/* Search */}
            {showSearch && (
              <form onSubmit={handleSearchSubmit} className="relative hidden lg:block">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="search"
                  value={searchQuery}
                  onChange={(e) => {
                    setSearchQuery(e.target.value);
                    onSearch?.(e.target.value);
                  }}
                  placeholder="Search courses..."
                  aria-label="Search courses"
                  className="h-9 w-56 rounded-xl border border-border/70 bg-surface pl-9 pr-3 text-sm text-foreground outline-none transition-all duration-300 placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary/25"
                />
              </form>
            )}

            {/* Dark mode toggle */}
            <button
              type="button"
              onClick={toggleDark}
              className="grid h-9 w-9 place-items-center rounded-full border border-border/70 bg-surface text-muted-foreground transition-colors hover:bg-accent/45 hover:text-primary"
              aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
            >
              {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>

            {isAuthenticated ? (
              /* Profile dropdown */
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setProfileOpen(!profileOpen)}
                  className="flex items-center gap-2 rounded-full focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <span className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-xs font-semibold text-primary-foreground shadow-bezel">
                    {initials}
                  </span>
                </button>

                <AnimatePresence>
                  {profileOpen && (
                    <>
                      <button
                        type="button"
                        className="fixed inset-0 z-40"
                        onClick={() => setProfileOpen(false)}
                        aria-label="Close menu"
                      />
                      <motion.div
                        initial={{ opacity: 0, y: -4, scale: 0.97 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: -4, scale: 0.97 }}
                        transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
                        className="absolute right-0 z-50 mt-2 w-64 rounded-2xl border border-border/70 bg-surface-elevated p-2 shadow-elevated"
                      >
                        <Link
                          to="/profile"
                          onClick={() => setProfileOpen(false)}
                          className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm transition-colors hover:bg-accent/45"
                        >
                          <UserCircle2
                            className="h-4.5 w-4.5 text-muted-foreground"
                            strokeWidth={1.5}
                          />
                          <span className="min-w-0">
                            <span className="block truncate font-medium text-foreground">
                              {displayName}
                            </span>
                            <span className="block truncate text-xs text-muted-foreground">
                              {email}
                            </span>
                          </span>
                        </Link>
                        <div className="my-1 h-px bg-border/70" />
                        <button
                          type="button"
                          onClick={() => {
                            setProfileOpen(false);
                            void auth.logout();
                          }}
                          className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-destructive transition-colors hover:bg-destructive/8"
                        >
                          <LogOut className="h-4 w-4" strokeWidth={1.5} />
                          Sign out
                        </button>
                      </motion.div>
                    </>
                  )}
                </AnimatePresence>
              </div>
            ) : (
              /* Guest actions */
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="hidden px-3 py-1.5 text-sm font-medium text-foreground transition-colors hover:text-primary sm:inline-flex"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="inline-flex h-9 items-center gap-1 rounded-full bg-primary px-4 text-sm font-medium text-primary-foreground shadow-bezel transition-transform duration-300 hover:scale-[1.02] active:scale-[0.98]"
                >
                  Get Started
                </Link>
              </div>
            )}

            {/* Mobile hamburger */}
            {isAuthenticated && (
              <button
                type="button"
                className="grid h-9 w-9 place-items-center rounded-full text-foreground transition-colors hover:bg-accent/45 md:hidden"
                onClick={() => setMobileOpen(!mobileOpen)}
                aria-label={mobileOpen ? "Close menu" : "Open menu"}
                aria-expanded={mobileOpen}
              >
                {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
              </button>
            )}
          </div>
        </nav>
      </header>

      {/* Mobile menu */}
      <AnimatePresence>
        {mobileOpen && isAuthenticated && (
          <>
            <motion.button
              type="button"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-40 bg-foreground/20 backdrop-blur-sm md:hidden"
              onClick={() => setMobileOpen(false)}
              aria-label="Close menu"
            />
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
              className="fixed inset-x-0 top-16 z-50 mx-4 rounded-2xl border border-border/70 bg-surface-elevated p-3 shadow-elevated md:hidden"
            >
              <nav className="flex flex-col">
                {nav.map((item) => {
                  const active = isRouteActive(pathname, item.match);
                  const Icon = item.icon;
                  return (
                    <Link
                      key={item.to}
                      to={item.to}
                      onClick={() => setMobileOpen(false)}
                      className={cn(
                        "flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium transition-colors",
                        active
                          ? "bg-accent/55 text-primary"
                          : "text-muted-foreground hover:bg-accent/35 hover:text-foreground",
                      )}
                    >
                      <Icon className="h-4.5 w-4.5" />
                      {item.label}
                    </Link>
                  );
                })}

                <div className="my-2 h-px bg-border/70" />

                <Link
                  to="/profile"
                  onClick={() => setMobileOpen(false)}
                  className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent/35 hover:text-foreground"
                >
                  <UserCircle2 className="h-4.5 w-4.5" />
                  Profile
                </Link>

                <button
                  type="button"
                  onClick={() => {
                    setMobileOpen(false);
                    void auth.logout();
                  }}
                  className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium text-destructive transition-colors hover:bg-destructive/8"
                >
                  <LogOut className="h-4.5 w-4.5" />
                  Sign out
                </button>
              </nav>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
