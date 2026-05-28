import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { useState } from "react";
import {
  GraduationCap, Home, BookOpen, Compass, Award, Settings,
  LogOut, Shield, Menu, X, Camera, BookOpen as BookIcon,
  CheckCircle, Zap, Flame, Edit3, Save, Globe, Bell,
  User, Mail, FileText,
} from "lucide-react";

export const Route = createFileRoute("/profile")({
  component: ProfilePage,
  head: () => ({ meta: [{ title: "Profile — EduLife" }] }),
});

// ─── Data ─────────────────────────────────────────────────────────────────────

const navLinks = [
  { icon: Home,     label: "Home",         to: "/dashboard"    as const },
  { icon: BookOpen, label: "My Courses",   to: "/courses"      as const },
  { icon: Compass,  label: "Explore",      to: "/explore"      as const },
  { icon: Award,    label: "Certificates", to: "/certificates" as const },
  { icon: Settings, label: "Settings",     to: "/profile"      as const },
];

const LANGUAGES = ["English", "French", "Arabic", "Darija"] as const;

// ─── Sidebar ──────────────────────────────────────────────────────────────────

function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <>
      {open && (
        <div className="fixed inset-0 z-30 bg-foreground/20 backdrop-blur-sm md:hidden" onClick={onClose} />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-surface-elevated
          transition-transform duration-300 ease-in-out
          ${open ? "translate-x-0" : "-translate-x-full"} md:translate-x-0 md:static md:z-auto`}
        style={{ boxShadow: "var(--shadow-luxury)" }}
      >
        <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border/60 px-6">
          <div className="relative">
            <span className="grid place-items-center h-8 w-8 rounded-xl bg-gradient-primary text-primary-foreground shadow-glow">
              <GraduationCap className="h-4 w-4" />
            </span>
            <span className="absolute -inset-1 rounded-2xl bg-primary/20 blur-md -z-10" />
          </div>
          <span className="text-display text-lg text-foreground tracking-tight">EduLife</span>
          <button className="ml-auto md:hidden text-muted-foreground hover:text-foreground transition-colors" onClick={onClose}>
            <X className="h-4 w-4" />
          </button>
        </div>

        <nav className="flex-1 px-3 py-6 space-y-0.5">
          <p className="px-3 mb-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Main</p>
          {navLinks.map(({ icon: Icon, label, to }) => {
            const active = to === "/profile";
            return (
              <Link key={label} to={to}
                className={`relative w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${
                  active
                    ? "bg-primary/10 text-primary shadow-sm"
                    : "text-muted-foreground hover:bg-accent/80 hover:text-foreground"
                }`}
              >
                <Icon className="h-4 w-4 shrink-0" strokeWidth={active ? 2 : 1.75} />
                {label}
                {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" />}
              </Link>
            );
          })}
          <div className="pt-4 mt-4 border-t border-border/60">
            <p className="px-3 mb-3 text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Progress</p>
            <Link to="/level"
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:bg-accent/80 hover:text-foreground transition-all duration-200"
            >
              <Shield className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              Level & Progress
            </Link>
          </div>
        </nav>

        <div className="border-t border-border/60 p-4">
          <div className="flex items-center gap-3">
            <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold">
              MB
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground">Mohamed Baghdadi</p>
              <p className="truncate text-xs text-muted-foreground">m.baghdadi@example.com</p>
            </div>
            <Link to="/login" className="text-muted-foreground hover:text-foreground transition-colors" aria-label="Log out">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>
    </>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

function ProfilePage() {
  const [sidebarOpen, setSidebarOpen]   = useState(false);
  const [editing, setEditing]           = useState(false);
  const [saved, setSaved]               = useState(false);

  const [name,     setName]     = useState("Mohamed Baghdadi");
  const [email,    setEmail]    = useState("m.baghdadi@example.com");
  const [bio,      setBio]      = useState("Lifelong learner based in Casablanca. Passionate about technology and languages.");
  const [language, setLanguage] = useState<typeof LANGUAGES[number]>("English");
  const [notifs,   setNotifs]   = useState(true);

  function handleSave() {
    setSaved(true);
    setEditing(false);
    setTimeout(() => setSaved(false), 3000);
  }

  const stats = [
    { icon: BookIcon,     label: "Courses enrolled", value: "6",    color: "text-primary",   bg: "bg-primary/8",  border: "border-primary/16" },
    { icon: CheckCircle,  label: "Completed",         value: "2",    color: "text-teal",      bg: "bg-teal/8",     border: "border-teal/16"    },
    { icon: Award,        label: "Certificates",      value: "2",    color: "text-gold",      bg: "bg-gold/8",     border: "border-gold/16"    },
    { icon: Zap,          label: "XP earned",         value: "1,300",color: "text-amber-500", bg: "bg-amber-50 dark:bg-amber-500/8", border: "border-amber-200 dark:border-amber-500/16" },
    { icon: Flame,        label: "Day streak",        value: "12",   color: "text-orange-500",bg: "bg-orange-50 dark:bg-orange-500/8", border: "border-orange-200 dark:border-orange-500/16" },
    { icon: Shield,       label: "Current level",     value: "7",    color: "text-primary",   bg: "bg-primary/8",  border: "border-primary/16" },
  ];

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">

        {/* Top bar */}
        <header className="flex h-16 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/80 backdrop-blur-md px-6 sticky top-0 z-20">
          <button className="md:hidden text-muted-foreground hover:text-foreground transition-colors" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </button>
          <div className="ml-auto flex items-center gap-2.5">
            {saved && (
              <motion.span
                initial={{ opacity: 0, x: 8 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0 }}
                className="flex items-center gap-1.5 text-xs font-medium text-teal"
              >
                <CheckCircle className="h-3.5 w-3.5" />
                Saved
              </motion.span>
            )}
            <Link to="/level"
              className="hidden sm:inline-flex items-center gap-2 h-9 rounded-full border border-primary/25 bg-primary/6 px-4 text-xs font-semibold text-primary hover:bg-primary/12 hover:border-primary/40 transition-all"
            >
              <Shield className="h-3.5 w-3.5" />Level 7
            </Link>
            <div className="grid h-9 w-9 place-items-center rounded-full bg-gradient-primary text-primary-foreground text-sm font-semibold shadow-soft">
              {name.split(" ").map(p => p[0]).join("").slice(0, 2).toUpperCase()}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-3xl px-6 lg:px-8 py-10 space-y-8">

            {/* Avatar + name header */}
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="relative rounded-3xl overflow-hidden bg-gradient-to-br from-primary to-primary-glow p-8 grain"
            >
              <div className="absolute -top-12 -right-12 h-48 w-48 rounded-full bg-white/10 blur-3xl pointer-events-none" />
              <div className="relative z-10 flex items-center gap-6">
                <div className="relative shrink-0">
                  <div className="grid h-20 w-20 place-items-center rounded-2xl bg-primary-foreground/15 border border-primary-foreground/25 text-3xl font-bold text-white">
                    {name.split(" ").map(p => p[0]).join("").slice(0, 2).toUpperCase()}
                  </div>
                  <button className="absolute -bottom-1 -right-1 grid h-6 w-6 place-items-center rounded-full bg-white text-foreground shadow-soft hover:opacity-80 transition-opacity">
                    <Camera className="h-3 w-3" />
                  </button>
                </div>
                <div className="flex-1 min-w-0">
                  <h1 className="text-display text-2xl text-white leading-tight">{name}</h1>
                  <p className="text-sm text-primary-foreground/70 mt-0.5">{email}</p>
                  <p className="text-xs text-primary-foreground/55 mt-1 line-clamp-2">{bio}</p>
                </div>
                <button
                  onClick={() => setEditing(v => !v)}
                  className="shrink-0 flex items-center gap-2 h-9 rounded-full border border-primary-foreground/25 bg-primary-foreground/10 px-4 text-xs font-medium text-white hover:bg-primary-foreground/20 transition-all backdrop-blur-sm"
                >
                  <Edit3 className="h-3.5 w-3.5" />
                  {editing ? "Cancel" : "Edit"}
                </button>
              </div>
            </motion.div>

            {/* Stats grid */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.1 }}
              className="grid grid-cols-2 sm:grid-cols-3 gap-3"
            >
              {stats.map((s, i) => {
                const Icon = s.icon;
                return (
                  <motion.div
                    key={s.label}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.15 + i * 0.05 }}
                    className={`flex items-center gap-3 rounded-2xl border ${s.border} p-4`}
                  >
                    <div className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl ${s.bg}`}>
                      <Icon className={`h-4 w-4 ${s.color}`} strokeWidth={1.75} />
                    </div>
                    <div>
                      <p className="text-base font-bold text-display text-foreground leading-none tabular-nums">{s.value}</p>
                      <p className="text-[10px] text-muted-foreground mt-0.5">{s.label}</p>
                    </div>
                  </motion.div>
                );
              })}
            </motion.div>

            {/* Edit form */}
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.2 }}
              className="rounded-2xl border border-border/70 bg-surface-elevated divide-y divide-border/60"
              style={{ boxShadow: "var(--shadow-soft)" }}
            >
              {/* Section: Personal info */}
              <div className="px-6 py-5">
                <div className="flex items-center gap-3 mb-5">
                  <span className="h-1 w-1 rounded-full bg-primary/60" />
                  <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Personal info</h2>
                </div>
                <div className="space-y-4">
                  {/* Name */}
                  <div className="space-y-1.5">
                    <label className="flex items-center gap-1.5 text-xs font-medium text-foreground">
                      <User className="h-3.5 w-3.5 text-muted-foreground" />
                      Full name
                    </label>
                    {editing ? (
                      <input
                        type="text"
                        value={name}
                        onChange={e => setName(e.target.value)}
                        className="w-full h-9 rounded-xl border border-border/80 bg-surface px-3 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary/40 focus:ring-2 focus:ring-ring/15 transition-all"
                      />
                    ) : (
                      <p className="text-sm text-foreground py-1.5">{name}</p>
                    )}
                  </div>

                  {/* Email */}
                  <div className="space-y-1.5">
                    <label className="flex items-center gap-1.5 text-xs font-medium text-foreground">
                      <Mail className="h-3.5 w-3.5 text-muted-foreground" />
                      Email address
                    </label>
                    {editing ? (
                      <input
                        type="email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        className="w-full h-9 rounded-xl border border-border/80 bg-surface px-3 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary/40 focus:ring-2 focus:ring-ring/15 transition-all"
                      />
                    ) : (
                      <p className="text-sm text-foreground py-1.5">{email}</p>
                    )}
                  </div>

                  {/* Bio */}
                  <div className="space-y-1.5">
                    <label className="flex items-center gap-1.5 text-xs font-medium text-foreground">
                      <FileText className="h-3.5 w-3.5 text-muted-foreground" />
                      Bio
                    </label>
                    {editing ? (
                      <textarea
                        value={bio}
                        onChange={e => setBio(e.target.value)}
                        rows={3}
                        className="w-full rounded-xl border border-border/80 bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground/50 outline-none focus:border-primary/40 focus:ring-2 focus:ring-ring/15 transition-all resize-none"
                      />
                    ) : (
                      <p className="text-sm text-foreground py-1.5 leading-relaxed">{bio}</p>
                    )}
                  </div>
                </div>
              </div>

              {/* Section: Preferences */}
              <div className="px-6 py-5">
                <div className="flex items-center gap-3 mb-5">
                  <span className="h-1 w-1 rounded-full bg-primary/60" />
                  <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Preferences</h2>
                </div>
                <div className="space-y-4">
                  {/* Language */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="grid h-8 w-8 place-items-center rounded-xl bg-primary/8">
                        <Globe className="h-4 w-4 text-primary" strokeWidth={1.75} />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">Interface language</p>
                        <p className="text-xs text-muted-foreground">Language used in the app</p>
                      </div>
                    </div>
                    {editing ? (
                      <select
                        value={language}
                        onChange={e => setLanguage(e.target.value as typeof LANGUAGES[number])}
                        className="h-8 rounded-xl border border-border/80 bg-surface px-2 text-xs text-foreground outline-none focus:border-primary/40 transition-all"
                      >
                        {LANGUAGES.map(l => <option key={l} value={l}>{l}</option>)}
                      </select>
                    ) : (
                      <span className="text-sm text-foreground">{language}</span>
                    )}
                  </div>

                  {/* Notifications */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="grid h-8 w-8 place-items-center rounded-xl bg-gold/8">
                        <Bell className="h-4 w-4 text-gold" strokeWidth={1.75} />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">Email notifications</p>
                        <p className="text-xs text-muted-foreground">Lesson reminders and progress updates</p>
                      </div>
                    </div>
                    <button
                      onClick={() => editing && setNotifs(v => !v)}
                      className={`relative h-6 w-10 rounded-full transition-all duration-200 ${
                        notifs ? "bg-primary" : "bg-border"
                      } ${!editing ? "opacity-60 cursor-not-allowed" : "cursor-pointer"}`}
                      disabled={!editing}
                    >
                      <span className={`absolute top-0.5 left-0.5 h-5 w-5 rounded-full bg-white shadow-sm transition-transform duration-200 ${notifs ? "translate-x-4" : "translate-x-0"}`} />
                    </button>
                  </div>
                </div>
              </div>

              {/* Save button */}
              {editing && (
                <div className="px-6 py-4 flex items-center justify-end gap-3">
                  <button
                    onClick={() => setEditing(false)}
                    className="h-9 rounded-xl border border-border/80 px-4 text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-accent transition-all"
                  >
                    Discard
                  </button>
                  <button
                    onClick={handleSave}
                    className="flex items-center gap-2 h-9 rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground hover:opacity-90 transition-opacity shadow-soft"
                  >
                    <Save className="h-3.5 w-3.5" />
                    Save changes
                  </button>
                </div>
              )}
            </motion.div>

            {/* Danger zone */}
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.3 }}
              className="rounded-2xl border border-destructive/20 bg-destructive/3 px-6 py-5"
            >
              <div className="flex items-center gap-3 mb-4">
                <span className="h-1 w-1 rounded-full bg-destructive/60" />
                <h2 className="text-xs uppercase tracking-[0.2em] text-destructive/70 font-medium">Danger zone</h2>
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-foreground">Delete account</p>
                  <p className="text-xs text-muted-foreground">Permanently remove your account and all data</p>
                </div>
                <button className="h-8 rounded-xl border border-destructive/30 px-4 text-xs font-medium text-destructive hover:bg-destructive/8 transition-all">
                  Delete
                </button>
              </div>
            </motion.div>

          </div>
        </main>
      </div>
    </div>
  );
}
