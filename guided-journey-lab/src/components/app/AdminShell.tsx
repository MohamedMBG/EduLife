import type { ReactNode } from "react";
import { Link, useRouter } from "@tanstack/react-router";
import {
  BarChart3,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  ShieldAlert,
  ShieldCheck,
} from "lucide-react";
import { useAuth } from "../../lib/auth/auth-context";

interface AdminShellProps {
  active: "dashboard" | "analytics" | "teacher-requests";
  children: ReactNode;
}

interface NavItem {
  key: AdminShellProps["active"];
  label: string;
  icon: ReactNode;
  href: string;
}

const NAV_ITEMS: NavItem[] = [
  {
    key: "dashboard",
    label: "Dashboard",
    icon: <LayoutDashboard className="h-4 w-4" />,
    href: "/admin/dashboard",
  },
  {
    key: "analytics",
    label: "Analytics",
    icon: <BarChart3 className="h-4 w-4" />,
    href: "/admin/analytics",
  },
  {
    key: "teacher-requests",
    label: "Teacher requests",
    icon: <GraduationCap className="h-4 w-4" />,
    href: "/admin/teacher-requests",
  },
];

export function AdminShell({ active, children }: AdminShellProps) {
  const auth = useAuth();
  const router = useRouter();

  async function handleLogout() {
    await auth.logout();
    router.navigate({ to: "/login" });
  }

  return (
    <div className="flex min-h-screen bg-background">
      {/* Sidebar */}
      <aside className="hidden w-72 flex-col border-r border-border/70 bg-surface-elevated/95 backdrop-blur-md lg:flex">
        {/* Brand */}
        <div className="border-b border-border/70 px-6 py-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-bezel">
              <ShieldCheck className="h-4 w-4" />
            </div>
            <div>
              <p className="text-display text-lg leading-none text-foreground">EduLife</p>
              <p className="mt-1 text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
                Platform admin
              </p>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex flex-col gap-1 p-4 pt-5">
          <p className="px-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
            Operations
          </p>
          {NAV_ITEMS.map((item) => {
            const isActive = item.key === active;
            return (
              <Link
                key={item.key}
                to={item.href}
                className={[
                  "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-accent/55 text-primary shadow-bezel ring-1 ring-primary/15"
                    : "text-muted-foreground hover:bg-accent/35 hover:text-primary",
                ].join(" ")}
              >
                {item.icon}
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="mx-4 rounded-2xl border border-primary/15 bg-accent/35 p-4">
          <div className="flex items-center gap-2 text-primary">
            <ShieldAlert className="h-4 w-4" />
            <p className="text-xs font-bold uppercase tracking-[0.16em]">Admin scope</p>
          </div>
          <p className="mt-2 text-xs leading-relaxed text-muted-foreground">
            Course approval, teacher access, and platform totals are server-authorized by backend
            RBAC.
          </p>
        </div>

        {/* Footer */}
        <div className="mt-auto border-t border-border p-4">
          <div className="mb-3 px-1">
            <p className="text-xs font-medium text-foreground truncate">{auth.session?.email}</p>
            <p className="mt-0.5 inline-flex items-center gap-1 rounded-full bg-accent/55 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-primary">
              <ShieldCheck className="h-2.5 w-2.5" />
              Admin
            </p>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-muted-foreground hover:bg-accent/45 hover:text-primary transition-colors"
          >
            <LogOut className="h-4 w-4" />
            Sign out
          </button>
        </div>
      </aside>

      {/* Mobile header */}
      <div className="fixed inset-x-0 top-0 z-30 flex items-center gap-3 border-b border-border/70 bg-surface-elevated/92 px-4 py-3 backdrop-blur-md lg:hidden">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          <ShieldCheck className="h-3.5 w-3.5" />
        </div>
        <p className="text-sm font-semibold text-foreground">Admin console</p>
        <div className="ml-auto flex items-center gap-2">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.key}
              to={item.href}
              className={[
                "flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium transition-colors",
                item.key === active
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent/45",
              ].join(" ")}
            >
              {item.icon}
              <span className="hidden sm:inline">{item.label}</span>
            </Link>
          ))}
        </div>
      </div>

      {/* Main content */}
      <main className="flex-1 overflow-auto pt-14 lg:pt-0">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">{children}</div>
      </main>
    </div>
  );
}
