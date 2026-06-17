import { AnimatePresence, motion, useMotionValueEvent, useScroll } from "framer-motion";
import { Link } from "@tanstack/react-router";
import { Menu, X } from "lucide-react";
import { useState } from "react";
import { useAuth } from "@/lib/auth/auth-context";

const NAV_ITEMS = [
  { label: "Programs", href: "#programs" },
  { label: "Philosophy", href: "#philosophy" },
  { label: "Faculty", href: "#faculty" },
  { label: "Admissions", href: "#admissions" },
];

export function PublicNavbar() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const auth = useAuth();
  const primaryCtaTo = auth.status === "authenticated" ? "/dashboard" : "/register";
  const primaryCtaLabel = auth.status === "authenticated" ? "Open Dashboard" : "Enroll Now";
  const { scrollY } = useScroll();

  useMotionValueEvent(scrollY, "change", (latest) => {
    setScrolled(latest > 50);
  });

  return (
    <>
      <motion.header
        className="sticky top-0 z-50 border-b border-[#c5c6cd]/60 backdrop-blur-xl"
        animate={{
          backgroundColor: scrolled
            ? "rgba(246, 250, 254, 0.95)"
            : "rgba(246, 250, 254, 0.88)",
          boxShadow: scrolled
            ? "0 8px 32px -16px rgba(9, 20, 38, 0.12)"
            : "0 0px 0px 0px rgba(9, 20, 38, 0)",
        }}
        transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
      >
        <nav
          className="mx-auto flex h-[4.5rem] w-full max-w-[1280px] items-center justify-between gap-6 px-5 sm:px-6 lg:px-8"
          aria-label="Public navigation"
        >
          <motion.a
            href="#top"
            className="text-[1.05rem] font-semibold tracking-[-0.04em] text-primary transition-opacity hover:opacity-75"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
          >
            EduLife
          </motion.a>

          <div className="hidden items-center gap-8 lg:flex">
            {NAV_ITEMS.map((item) => (
              <motion.a
                key={item.href}
                href={item.href}
                className="relative text-[11px] font-semibold uppercase tracking-[0.16em] text-[#505f76] transition-colors hover:text-primary"
                whileHover="hover"
              >
                {item.label}
                <motion.span
                  className="absolute -bottom-1 left-0 h-px bg-primary"
                  initial={{ width: "0%" }}
                  variants={{
                    hover: { width: "100%" },
                  }}
                  transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
                />
              </motion.a>
            ))}
          </div>

          <div className="hidden items-center gap-3 lg:flex">
            <Link
              to="/login"
              className="rounded-full px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.16em] text-[#505f76] transition-colors hover:text-primary"
            >
              Sign In
            </Link>
            <motion.div whileHover={{ y: -2 }} whileTap={{ scale: 0.98 }}>
              <Link
                to={primaryCtaTo}
                className="rounded-full bg-primary px-5 py-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-white shadow-[0_18px_38px_-24px_rgba(9,20,38,0.55)]"
              >
                {primaryCtaLabel}
              </Link>
            </motion.div>
          </div>

          <button
            type="button"
            onClick={() => setMobileOpen((open) => !open)}
            aria-expanded={mobileOpen}
            aria-controls="public-mobile-menu"
            aria-label={mobileOpen ? "Close navigation menu" : "Open navigation menu"}
            className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-[#c5c6cd]/70 bg-white/90 text-primary transition-colors hover:bg-white lg:hidden"
          >
            {mobileOpen ? <X className="h-4.5 w-4.5" /> : <Menu className="h-4.5 w-4.5" />}
          </button>
        </nav>
      </motion.header>

      <AnimatePresence>
        {mobileOpen ? (
          <>
            <motion.button
              type="button"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setMobileOpen(false)}
              className="fixed inset-0 z-40 bg-primary/12 backdrop-blur-sm lg:hidden"
              aria-label="Close navigation menu"
            />
            <motion.div
              id="public-mobile-menu"
              initial={{ opacity: 0, y: -12, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -12, scale: 0.96 }}
              transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
              className="fixed inset-x-4 top-20 z-50 rounded-[28px] border border-[#c5c6cd]/70 bg-white p-5 shadow-[0_28px_72px_-38px_rgba(9,20,38,0.38)] lg:hidden"
            >
              <div className="flex flex-col gap-2">
                {NAV_ITEMS.map((item, i) => (
                  <motion.a
                    key={item.href}
                    href={item.href}
                    onClick={() => setMobileOpen(false)}
                    initial={{ opacity: 0, x: -12 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.04 * i, duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
                    className="rounded-2xl px-4 py-3 text-sm font-medium text-primary transition-colors hover:bg-[#f0f4f8]"
                  >
                    {item.label}
                  </motion.a>
                ))}
              </div>

              <div className="mt-4 grid gap-3 border-t border-[#dfe3e7] pt-4">
                <Link
                  to="/login"
                  onClick={() => setMobileOpen(false)}
                  className="rounded-full border border-[#c5c6cd] px-4 py-3 text-center text-[11px] font-semibold uppercase tracking-[0.16em] text-primary transition-colors hover:bg-[#f6fafe]"
                >
                  Sign In
                </Link>
                <Link
                  to={primaryCtaTo}
                  onClick={() => setMobileOpen(false)}
                  className="rounded-full bg-primary px-4 py-3 text-center text-[11px] font-semibold uppercase tracking-[0.18em] text-white"
                >
                  {primaryCtaLabel}
                </Link>
              </div>
            </motion.div>
          </>
        ) : null}
      </AnimatePresence>
    </>
  );
}
