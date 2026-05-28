import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { useMemo, useState } from "react";
import {
  Award,
  BookOpen,
  Calendar,
  CheckCircle,
  Compass,
  Download,
  ExternalLink,
  GraduationCap,
  Home,
  LogOut,
  Menu,
  Settings,
  Share2,
  Shield,
  Sparkles,
  Star,
  X,
  Zap,
} from "lucide-react";
import { learnerCertificates } from "../lib/learner-flow-data";

export const Route = createFileRoute("/certificates")({
  validateSearch: (search: Record<string, unknown>) => ({
    earned: typeof search.earned === "string" ? search.earned : undefined,
  }),
  component: CertificatesPage,
  head: () => ({ meta: [{ title: "Certificates - EduLife" }] }),
});

const user = { name: "Mohamed Baghdadi", email: "m.baghdadi@example.com", initials: "MB" };

const navLinks = [
  { icon: Home, label: "Home", to: "/dashboard" as const },
  { icon: BookOpen, label: "My Courses", to: "/courses" as const },
  { icon: Compass, label: "Explore", to: "/explore" as const },
  { icon: Award, label: "Certificates", to: "/certificates" as const },
  { icon: Settings, label: "Settings", to: "/profile" as const },
];

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
}

function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-foreground/20 backdrop-blur-sm md:hidden" onClick={onClose} />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-surface-elevated transition-transform duration-300 ease-in-out ${
          open ? "translate-x-0" : "-translate-x-full"
        } md:static md:z-auto md:translate-x-0`}
        style={{ boxShadow: "var(--shadow-luxury)" }}
      >
        <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border/60 px-6">
          <div className="relative">
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-glow">
              <GraduationCap className="h-4 w-4" />
            </span>
            <span className="absolute -inset-1 -z-10 rounded-2xl bg-primary/20 blur-md" />
          </div>
          <span className="text-display text-lg tracking-tight text-foreground">EduLife</span>
          <button className="ml-auto text-muted-foreground transition-colors hover:text-foreground md:hidden" onClick={onClose}>
            <X className="h-4 w-4" />
          </button>
        </div>

        <nav className="flex-1 space-y-0.5 px-3 py-6">
          <p className="mb-3 px-3 text-[10px] font-medium uppercase tracking-[0.18em] text-muted-foreground/60">Main</p>
          {navLinks.map(({ icon: Icon, label, to }) => {
            const active = to === "/certificates";
            return (
              <Link
                key={label}
                to={to}
                className={`relative flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                  active ? "bg-primary/10 text-primary shadow-sm" : "text-muted-foreground hover:bg-accent/80 hover:text-foreground"
                }`}
              >
                <Icon className="h-4 w-4 shrink-0" strokeWidth={active ? 2 : 1.75} />
                {label}
                {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" />}
              </Link>
            );
          })}
          <div className="mt-4 border-t border-border/60 pt-4">
            <p className="mb-3 px-3 text-[10px] font-medium uppercase tracking-[0.18em] text-muted-foreground/60">Progress</p>
            <Link
              to="/level"
              className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-muted-foreground transition-all duration-200 hover:bg-accent/80 hover:text-foreground"
            >
              <Shield className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              Level & Progress
            </Link>
          </div>
        </nav>

        <div className="border-t border-border/60 p-4">
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground">
                {user.initials}
              </div>
              <span className="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-surface-elevated bg-teal" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground">{user.name}</p>
              <p className="truncate text-xs text-muted-foreground">{user.email}</p>
            </div>
            <Link to="/login" className="text-muted-foreground transition-colors hover:text-foreground" aria-label="Log out">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>
    </>
  );
}

function CertificateCard({ cert, index }: { cert: (typeof learnerCertificates)[number]; index: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: index * 0.1 }}
      className={`group relative overflow-hidden rounded-3xl border ${cert.accentBorder} bg-surface-elevated transition-all duration-300 hover:-translate-y-0.5 hover:shadow-luxury`}
    >
      <div className="relative h-32 overflow-hidden grain" style={{ background: `linear-gradient(135deg, ${cert.gradientFrom}, ${cert.gradientTo})` }}>
        <div className="pointer-events-none absolute -right-12 -top-12 h-40 w-40 rounded-full bg-white/10 blur-3xl" />
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="grid h-16 w-16 place-items-center rounded-2xl border border-white/20 bg-white/15 shadow-elevated backdrop-blur-sm">
            <Award className="h-8 w-8 text-white" strokeWidth={1.5} />
          </div>
        </div>
        <div className="absolute right-3 top-3">
          <span className="rounded-full border border-white/25 bg-white/20 px-2.5 py-1 text-[10px] font-semibold text-white backdrop-blur-sm">{cert.grade}</span>
        </div>
        <div className="absolute bottom-3 left-3">
          <span className="flex items-center gap-1 rounded-full border border-border/40 bg-background/80 px-2.5 py-1 text-[10px] font-semibold text-foreground backdrop-blur-md">
            <Star className="h-3 w-3 fill-gold text-gold" strokeWidth={0} />
            {cert.score}%
          </span>
        </div>
      </div>

      <div className="space-y-3 p-5">
        <div>
          <span className={`mb-2 inline-block rounded-full px-2.5 py-0.5 text-[11px] font-medium uppercase tracking-[0.12em] ${cert.accentBg} ${cert.accentText}`}>
            {cert.subject}
          </span>
          <h3 className="text-display text-sm font-semibold leading-snug text-foreground">{cert.courseTitle}</h3>
          <p className="mt-0.5 text-xs text-muted-foreground">Instructor: {cert.instructor}</p>
        </div>

        <div className="flex items-center gap-3 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <Calendar className="h-3 w-3" />
            {formatDate(cert.issuedAt)}
          </span>
          <span className="flex items-center gap-1">
            <Zap className="h-3 w-3 text-gold" />
            {cert.xpEarned} XP
          </span>
        </div>

        <div className="flex items-center gap-2 border-t border-border/60 pt-3">
          <button className="flex h-8 flex-1 items-center justify-center gap-1.5 rounded-xl border border-border/80 text-xs font-medium text-foreground transition-all hover:bg-accent">
            <Download className="h-3.5 w-3.5" />
            Download
          </button>
          <button className="flex h-8 flex-1 items-center justify-center gap-1.5 rounded-xl border border-border/80 text-xs font-medium text-foreground transition-all hover:bg-accent">
            <Share2 className="h-3.5 w-3.5" />
            Share
          </button>
          <button className="grid h-8 w-8 shrink-0 place-items-center rounded-xl border border-border/80 text-muted-foreground transition-all hover:bg-accent hover:text-foreground">
            <ExternalLink className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </motion.div>
  );
}

function CertificatesPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { earned } = Route.useSearch();

  const certificates = useMemo(() => {
    if (earned === "1") {
      return [
        {
          id: 99,
          courseTitle: "Web Development Fundamentals",
          subject: "Technology",
          instructor: "Khalid Moussaoui",
          issuedAt: "2026-05-28",
          score: 84,
          grade: "Passed",
          xpEarned: 150,
          duration: "18h",
          lessonCount: 8,
          gradientFrom: "oklch(0.38 0.16 145)",
          gradientTo: "oklch(0.52 0.20 142)",
          accentText: "text-primary",
          accentBg: "bg-primary/8",
          accentBorder: "border-primary/20",
        },
        ...learnerCertificates,
      ];
    }

    return learnerCertificates;
  }, [earned]);

  const totalXp = certificates.reduce((sum, cert) => sum + cert.xpEarned, 0);
  const averageScore = Math.round(certificates.reduce((sum, cert) => sum + cert.score, 0) / certificates.length);

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="sticky top-0 z-20 flex h-16 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/80 px-6 backdrop-blur-md">
          <button className="text-muted-foreground transition-colors hover:text-foreground md:hidden" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </button>
          <div className="ml-auto flex items-center gap-2.5">
            <Link
              to="/level"
              className="hidden h-9 items-center gap-2 rounded-full border border-primary/25 bg-primary/6 px-4 text-xs font-semibold text-primary transition-all hover:border-primary/40 hover:bg-primary/12 sm:inline-flex"
            >
              <Shield className="h-3.5 w-3.5" />
              Level 7
            </Link>
            <Link to="/profile" className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-sm font-semibold text-primary-foreground shadow-soft transition-opacity hover:opacity-90">
              {user.initials}
            </Link>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-5xl space-y-8 px-6 py-10 lg:px-8">
            <motion.div initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55 }} className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary to-primary-glow p-8 grain sm:p-10">
              <div className="pointer-events-none absolute -right-12 -top-12 h-48 w-48 rounded-full bg-white/10 blur-3xl" />
              <div className="pointer-events-none absolute -bottom-10 -left-10 h-40 w-40 rounded-full bg-white/6 blur-2xl" />
              <div className="relative z-10 flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
                <div>
                  <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/10 px-3.5 py-1.5 text-xs font-medium text-primary-foreground/80 backdrop-blur-sm">
                    <Sparkles className="h-3 w-3" />
                    {earned ? "Certificate unlocked" : "Your achievements"}
                  </div>
                  <h1 className="text-display text-3xl leading-tight text-primary-foreground">Certificates</h1>
                  <p className="mt-1.5 text-sm text-primary-foreground/70">
                    {earned ? "Your passed exam now appears as an issued certificate preview." : "Each certificate is issued only after the learner passes the final exam."}
                  </p>
                </div>

                <div className="flex shrink-0 items-center gap-4">
                  <div className="text-center">
                    <p className="text-display text-2xl font-semibold tabular-nums text-white">{certificates.length}</p>
                    <p className="mt-0.5 text-xs text-primary-foreground/60">Earned</p>
                  </div>
                  <div className="h-8 w-px bg-primary-foreground/20" />
                  <div className="text-center">
                    <p className="text-display text-2xl font-semibold tabular-nums text-white">{totalXp}</p>
                    <p className="mt-0.5 text-xs text-primary-foreground/60">XP</p>
                  </div>
                  <div className="h-8 w-px bg-primary-foreground/20" />
                  <div className="text-center">
                    <p className="text-display text-2xl font-semibold tabular-nums text-white">{averageScore}%</p>
                    <p className="mt-0.5 text-xs text-primary-foreground/60">Avg score</p>
                  </div>
                </div>
              </div>
            </motion.div>

            {earned && (
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="rounded-2xl border border-teal/25 bg-teal/6 px-5 py-4">
                {/* This banner makes the pass-to-certificate transition explicit for the learner. */}
                <div className="flex items-start gap-3">
                  <div className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-teal/12 text-teal">
                    <CheckCircle className="h-5 w-5" strokeWidth={1.75} />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">Final exam passed</p>
                    <p className="mt-1 text-sm text-muted-foreground">The Web Development Fundamentals certificate is now visible at the top of your certificate list.</p>
                  </div>
                </div>
              </motion.div>
            )}

            <div className="flex items-center gap-3">
              <span className="h-1 w-1 rounded-full bg-primary/60" />
              <h2 className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">
                {certificates.length} certificate{certificates.length !== 1 ? "s" : ""}
              </h2>
              <div className="h-px flex-1 bg-gradient-to-r from-border to-transparent" />
            </div>

            <div className="grid gap-5 pb-6 sm:grid-cols-2 lg:grid-cols-3">
              {certificates.map((cert, index) => (
                <CertificateCard key={cert.id} cert={cert} index={index} />
              ))}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
