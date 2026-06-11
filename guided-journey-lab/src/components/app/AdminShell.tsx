import type { ReactNode } from "react";
import { Link, useRouter } from "@tanstack/react-router";
import {
  BarChart3,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  ShieldCheck,
  Users,
} from "lucide-react";
import { useAuth } from "../../lib/auth/auth-context";

interface AdminShellProps {
  active: "dashboard" | "teacher-requests" | "users";
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
    key: "teacher-requests",
    label: "Teacher Requests",
    icon: <GraduationCap className="h-4 w-4" />,
    href: "/admin/teacher-requests",
  },
  {
    key: "users",
    label: "Users",
    icon: <Users className="h-4 w-4" />,
    href: "/admin/users",
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
    <div className="flex min-h-screen bg-[oklch(0.955_0.006_240)]">
      {/* Sidebar */}
      <aside className="hidden w-60 flex-col border-r border-border bg-surface-elevated lg:flex">
        {/* Brand */}
        <div className="border-b border-border px-6 py-5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[oklch(0.40_0.19_250)] text-white">
              <ShieldCheck className="h-4 w-4" />
            </div>
            <div>
              <p className="text-sm font-semibold text-foreground">EduLife</p>
              <p className="text-xs text-muted-foreground">Admin console</p>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex flex-col gap-1 p-3 pt-4">
          {NAV_ITEMS.map((item) => {
            const isActive = item.key === active;
            return (
              <Link
                key={item.key}
                to={item.href}
                className={[
                  "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-[oklch(0.40_0.19_250)] text-white shadow-sm"
                    : "text-muted-foreground hover:bg-accent hover:text-foreground",
                ].join(" ")}
              >
                {item.icon}
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* Footer */}
        <div className="mt-auto border-t border-border p-4">
          <div className="mb-3 px-1">
            <p className="text-xs font-medium text-foreground truncate">
              {auth.session?.email}
            </p>
            <p className="mt-0.5 inline-flex items-center gap-1 rounded-full bg-[oklch(0.92_0.03_250)] px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-[oklch(0.40_0.19_250)]">
              <ShieldCheck className="h-2.5 w-2.5" />
              Admin
            </p>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
          >
            <LogOut className="h-4 w-4" />
            Sign out
          </button>
        </div>
      </aside>

      {/* Mobile header */}
      <div className="fixed inset-x-0 top-0 z-30 flex items-center gap-3 border-b border-border bg-surface-elevated px-4 py-3 lg:hidden">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-[oklch(0.40_0.19_250)] text-white">
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
                  ? "bg-[oklch(0.40_0.19_250)] text-white"
                  : "text-muted-foreground hover:bg-accent",
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
        <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">{children}</div>
      </main>
    </div>
  );
}
