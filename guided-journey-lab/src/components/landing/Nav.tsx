import { useState, useEffect } from "react";
import { GraduationCap, Menu, X, Sun, Moon } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Link } from "@tanstack/react-router";

const links = [
  { href: "#journey", label: "Journey" },
  { href: "#features", label: "Features" },
  { href: "#certificate", label: "Certificate" },
  { href: "#morocco", label: "Morocco" },
];

export function Nav() {
  const [open, setOpen] = useState(false);
  const [dark, setDark] = useState(false);

  useEffect(() => {
    const stored = localStorage.getItem("theme");
    if (stored === "dark") {
      document.documentElement.classList.add("dark");
      setDark(true);
    }
  }, []);

  function toggleDark() {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    localStorage.setItem("theme", next ? "dark" : "light");
  }

  return (
    <header className="fixed inset-x-0 top-0 z-50 backdrop-blur-xl bg-background/70 border-b border-border/60">
      <div className="mx-auto max-w-7xl px-6 lg:px-10 h-16 flex items-center justify-between">
        <a href="#top" className="flex items-center gap-2">
          <span className="grid place-items-center h-8 w-8 rounded-lg bg-gradient-primary text-primary-foreground">
            <GraduationCap className="h-4 w-4" />
          </span>
          <span className="text-display text-lg">EduLife</span>
        </a>

        <nav className="hidden md:flex items-center gap-8 text-sm text-muted-foreground">
          {links.map(({ href, label }) => (
            <a key={href} href={href} className="hover:text-foreground transition-colors">
              {label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-3">
          <Link
            to="/login"
            className="hidden md:inline-flex h-9 items-center rounded-full border border-border bg-surface-elevated px-4 text-sm font-medium text-foreground hover:bg-accent transition-colors"
          >
            Sign In
          </Link>
          <a
            href="#cta"
            className="inline-flex h-9 items-center rounded-full bg-foreground text-background px-4 text-sm font-medium hover:opacity-90 transition-opacity"
          >
            Get Early Access
          </a>
          <button
            onClick={toggleDark}
            className="grid h-9 w-9 place-items-center rounded-full border border-border text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
            aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
          >
            {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>
          <button
            className="md:hidden grid h-9 w-9 place-items-center rounded-full border border-border text-foreground hover:bg-accent transition-colors"
            onClick={() => setOpen((v) => !v)}
            aria-label={open ? "Close menu" : "Open menu"}
            aria-expanded={open}
          >
            {open ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
          </button>
        </div>
      </div>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2, ease: "easeInOut" }}
            className="md:hidden border-t border-border/60 bg-background/95 backdrop-blur-xl overflow-hidden"
          >
            <nav className="px-6 py-2 flex flex-col">
              {links.map(({ href, label }) => (
                <a
                  key={href}
                  href={href}
                  onClick={() => setOpen(false)}
                  className="py-4 text-base text-muted-foreground hover:text-foreground transition-colors border-b border-border/40"
                >
                  {label}
                </a>
              ))}
              <Link
                to="/login"
                onClick={() => setOpen(false)}
                className="py-4 text-base text-muted-foreground hover:text-foreground transition-colors border-b border-border/40"
              >
                Sign In
              </Link>
              <a
                href="#cta"
                onClick={() => setOpen(false)}
                className="mt-3 mb-2 inline-flex h-10 w-full items-center justify-center rounded-full bg-foreground text-background text-sm font-medium hover:opacity-90 transition-opacity"
              >
                Get Early Access
              </a>
            </nav>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
