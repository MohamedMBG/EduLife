import { useEffect, useState } from "react";
import { GraduationCap, Menu, X, Sun, Moon, ArrowUpRight } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Link } from "@tanstack/react-router";
import { useDarkMode } from "@/hooks/use-dark-mode";

const links = [
  { href: "#journey", label: "Journey" },
  { href: "#features", label: "Features" },
  { href: "#certificate", label: "Certificate" },
  { href: "#morocco", label: "Morocco" },
];

export function Nav() {
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const { dark, toggle: toggleDark } = useDarkMode();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header className="fixed inset-x-0 top-0 z-50 pointer-events-none">
      <div className="mx-auto max-w-7xl px-4 sm:px-6">
        <motion.div
          initial={{ y: -16, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
          className={`pointer-events-auto mt-4 flex items-center justify-between gap-3 rounded-full pl-3 pr-2 py-2 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
            scrolled
              ? "glass shadow-elevated"
              : "bg-background/40 border border-border/40 backdrop-blur-md"
          }`}
        >
          <a href="#top" className="flex items-center gap-2.5 pl-1 group">
            <span className="relative grid place-items-center h-8 w-8 rounded-xl bg-gradient-primary text-primary-foreground shadow-bezel">
              <GraduationCap className="h-4 w-4" strokeWidth={1.75} />
              <span className="absolute -inset-px rounded-xl ring-1 ring-white/30" />
            </span>
            <span className="text-display text-[1.05rem] tracking-tight">EduLife</span>
          </a>

          <nav className="hidden md:flex items-center gap-1 text-[13px] text-muted-foreground">
            {links.map(({ href, label }) => (
              <a
                key={href}
                href={href}
                className="relative px-3 py-1.5 rounded-full hover:text-foreground transition-colors duration-300 ease-out hover:bg-accent/60"
              >
                {label}
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <button
              onClick={toggleDark}
              className="hidden sm:grid h-9 w-9 place-items-center rounded-full text-muted-foreground hover:text-foreground hover:bg-accent transition-colors duration-300"
              aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
            >
              {dark ? <Sun className="h-4 w-4" strokeWidth={1.5} /> : <Moon className="h-4 w-4" strokeWidth={1.5} />}
            </button>

            <Link
              to="/login"
              className="hidden md:inline-flex h-9 items-center rounded-full px-3.5 text-[13px] font-medium text-foreground/80 hover:text-foreground transition-colors"
            >
              Sign In
            </Link>

            <a
              href="#cta"
              className="group relative inline-flex h-10 items-center gap-1 rounded-full bg-foreground text-background pl-4 pr-1 text-[13px] font-medium shadow-bezel transition-transform duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98]"
            >
              <span>Get Early Access</span>
              <span className="grid h-8 w-8 place-items-center rounded-full bg-background/15 group-hover:bg-background/25 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-x-0.5 group-hover:-translate-y-px">
                <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
              </span>
            </a>

            <button
              className="md:hidden grid h-9 w-9 place-items-center rounded-full text-foreground hover:bg-accent transition-colors"
              onClick={() => setOpen((v) => !v)}
              aria-label={open ? "Close menu" : "Open menu"}
              aria-expanded={open}
            >
              <span className="relative block h-3.5 w-4">
                <span
                  className={`absolute left-0 top-1/2 h-px w-full bg-current transition-all duration-400 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                    open ? "rotate-45" : "-translate-y-1.5"
                  }`}
                />
                <span
                  className={`absolute left-0 top-1/2 h-px w-full bg-current transition-all duration-400 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                    open ? "-rotate-45" : "translate-y-1.5"
                  }`}
                />
              </span>
            </button>
          </div>
        </motion.div>

        <AnimatePresence>
          {open && (
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
              className="md:hidden pointer-events-auto mt-3 rounded-3xl glass shadow-elevated overflow-hidden"
            >
              <nav className="px-4 py-3 flex flex-col">
                {links.map(({ href, label }, i) => (
                  <motion.a
                    key={href}
                    href={href}
                    onClick={() => setOpen(false)}
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.05 + i * 0.05, duration: 0.4 }}
                    className="py-3 px-2 text-base text-foreground/80 hover:text-foreground transition-colors border-b border-border/40 flex items-center justify-between"
                  >
                    {label}
                    <ArrowUpRight className="h-3.5 w-3.5 opacity-40" strokeWidth={1.75} />
                  </motion.a>
                ))}
                <Link
                  to="/login"
                  onClick={() => setOpen(false)}
                  className="py-3 px-2 text-base text-foreground/80 hover:text-foreground transition-colors border-b border-border/40"
                >
                  Sign In
                </Link>
                <button
                  onClick={() => {
                    setOpen(false);
                    toggleDark();
                  }}
                  className="py-3 px-2 text-base text-foreground/80 hover:text-foreground transition-colors flex items-center justify-between"
                >
                  Theme
                  {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                </button>
                <a
                  href="#cta"
                  onClick={() => setOpen(false)}
                  className="mt-2 mb-1 inline-flex h-11 w-full items-center justify-center gap-2 rounded-full bg-foreground text-background text-sm font-medium shadow-bezel"
                >
                  Get Early Access
                  <ArrowUpRight className="h-4 w-4" strokeWidth={1.75} />
                </a>
              </nav>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </header>
  );
}
