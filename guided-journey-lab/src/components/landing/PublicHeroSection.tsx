import { Link } from "@tanstack/react-router";
import { ArrowUpRight, CheckCircle2, Languages } from "lucide-react";
import { motion } from "framer-motion";
import { useAuth } from "@/lib/auth/auth-context";
import { MagneticButton, Parallax } from "./animations";

const HERO_METRICS = [
  { value: "12+", label: "Curated Paths" },
  { value: "Verified", label: "Credentials" },
  { value: "Darija/FR/EN", label: "Localized UI" },
];

const stagger = {
  hidden: {},
  visible: {
    transition: { staggerChildren: 0.06, delayChildren: 0.4 },
  },
};

const EASE = [0.16, 1, 0.3, 1] as [number, number, number, number];

const wordReveal = {
  hidden: { opacity: 0, y: 44, filter: "blur(12px)", rotateX: 50 },
  visible: {
    opacity: 1,
    y: 0,
    filter: "blur(0px)",
    rotateX: 0,
    transition: { duration: 0.85, ease: EASE },
  },
};

const fadeUp = {
  hidden: { opacity: 0, y: 28, filter: "blur(6px)" },
  visible: {
    opacity: 1,
    y: 0,
    filter: "blur(0px)",
    transition: { duration: 0.8, ease: EASE },
  },
};

const pillReveal = {
  hidden: { opacity: 0, scale: 0.88, filter: "blur(6px)" },
  visible: {
    opacity: 1,
    scale: 1,
    filter: "blur(0px)",
    transition: { duration: 0.7, delay: 0.15, ease: EASE },
  },
};

export function PublicHeroSection() {
  const auth = useAuth();
  const primaryCtaTo = auth.status === "authenticated" ? "/dashboard" : "/register";
  const primaryCtaLabel = auth.status === "authenticated" ? "Open Dashboard" : "Get Early Access";

  const headlineWords = ["One", "path", "to"];
  const headlineAccents = [
    { word: "master", italic: true, muted: true },
    { word: ",", italic: false, muted: false, noSpace: true },
  ];
  const headlineBold = [{ word: "validate" }];
  const headlineEnd = [", and graduate."];

  return (
    <section
      id="top"
      className="overflow-hidden px-5 pb-20 pt-12 sm:px-6 lg:px-8 lg:pb-28 lg:pt-[4.5rem]"
    >
      <div className="mx-auto grid max-w-[1280px] gap-12 lg:grid-cols-[minmax(0,1.02fr)_minmax(360px,460px)] lg:items-center lg:gap-16">
        <div className="max-w-[620px]">
          <motion.div
            variants={pillReveal}
            initial="hidden"
            animate="visible"
            className="inline-flex items-center gap-2 rounded-full border border-[#c5c6cd]/70 bg-white px-4 py-2 text-[10px] font-semibold uppercase tracking-[0.2em] text-primary shadow-[0_16px_40px_-32px_rgba(9,20,38,0.36)]"
          >
            <span className="h-1.5 w-1.5 rounded-full bg-primary" aria-hidden />
            The standard for Moroccan learners
          </motion.div>

          <motion.h1
            variants={stagger}
            initial="hidden"
            animate="visible"
            className="mt-8 max-w-[11ch] pb-2 text-[clamp(3rem,8vw,5.2rem)] font-light leading-[1.04] tracking-[-0.06em] text-primary"
            style={{ perspective: "800px" }}
          >
            {headlineWords.map((word, i) => (
              <span key={i} className="inline-block overflow-hidden">
                <motion.span
                  variants={wordReveal}
                  className="inline-block"
                  style={{ transformOrigin: "center bottom" }}
                >
                  {word}{" "}
                </motion.span>
              </span>
            ))}
            {headlineAccents.map((item, i) => (
              <span key={`a-${i}`} className="inline-block overflow-hidden">
                <motion.span
                  variants={wordReveal}
                  className={`inline-block ${item.italic ? "font-light italic" : ""} ${item.muted ? "text-[#505f76]" : ""}`}
                  style={{ transformOrigin: "center bottom" }}
                >
                  {item.word}
                  {!item.noSpace ? " " : ""}
                </motion.span>
              </span>
            ))}
            {" "}
            {headlineBold.map((item, i) => (
              <span key={`b-${i}`} className="inline-block overflow-hidden">
                <motion.span
                  variants={wordReveal}
                  className="inline-block font-semibold"
                  style={{ transformOrigin: "center bottom" }}
                >
                  {item.word}
                </motion.span>
              </span>
            ))}
            {headlineEnd.map((text, i) => (
              <span key={`e-${i}`} className="inline-block overflow-hidden">
                <motion.span
                  variants={wordReveal}
                  className="inline-block"
                  style={{ transformOrigin: "center bottom" }}
                >
                  {text}
                </motion.span>
              </span>
            ))}
          </motion.h1>

          <motion.p
            variants={fadeUp}
            initial="hidden"
            animate="visible"
            transition={{ delay: 0.9 }}
            className="mt-7 max-w-[40rem] text-base leading-8 text-[#505f76] sm:text-[1.05rem]"
          >
            A high-fidelity learning experience designed for clarity. From structured lessons to
            verifiable certificates, EduLife gives Moroccan learners one guided route instead of
            scattered resources.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 1.1, ease: [0.16, 1, 0.3, 1] }}
            className="mt-10 flex flex-col gap-3 sm:flex-row"
          >
            <MagneticButton strength={0.3}>
              <Link
                to={primaryCtaTo}
                className="inline-flex items-center justify-center gap-3 rounded-full bg-primary px-6 py-4 text-[11px] font-semibold uppercase tracking-[0.18em] text-white shadow-[0_26px_52px_-30px_rgba(9,20,38,0.62)] transition-transform duration-300 hover:-translate-y-0.5 active:translate-y-0 active:scale-[0.98]"
              >
                {primaryCtaLabel}
                <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-white/12">
                  <ArrowUpRight className="h-3.5 w-3.5" />
                </span>
              </Link>
            </MagneticButton>
            <MagneticButton strength={0.2}>
              <a
                href="#programs"
                className="inline-flex items-center justify-center gap-3 rounded-full border border-[#c5c6cd] bg-white px-6 py-4 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary transition-colors hover:bg-[#eef3f8]"
              >
                The Journey
                <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-[#f0f4f8]">
                  <ArrowUpRight className="h-3.5 w-3.5" />
                </span>
              </a>
            </MagneticButton>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 1.4, ease: [0.16, 1, 0.3, 1] }}
            className="mt-12 grid gap-5 border-t border-[#dfe3e7] pt-8 sm:grid-cols-3 sm:gap-6"
          >
            {HERO_METRICS.map((metric, i) => (
              <motion.div
                key={metric.label}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  duration: 0.6,
                  delay: 1.5 + i * 0.1,
                  ease: [0.16, 1, 0.3, 1],
                }}
                className="space-y-1"
              >
                <p className="text-sm font-semibold tracking-[-0.03em] text-primary">
                  {metric.value}
                </p>
                <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#505f76]">
                  {metric.label}
                </p>
              </motion.div>
            ))}
          </motion.div>
        </div>

        <Parallax speed={-0.15} className="hidden lg:block">
          <motion.div
            initial={{ opacity: 0, x: 60, rotate: 6, scale: 0.92 }}
            animate={{ opacity: 1, x: 0, rotate: 0, scale: 1 }}
            transition={{ duration: 1.2, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
            className="relative"
          >
            <motion.div
              animate={{
                scale: [1, 1.05, 1],
                opacity: [0.5, 0.7, 0.5],
              }}
              transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
              className="absolute inset-x-8 top-8 h-56 rounded-full bg-[#d8e3fb]/70 blur-3xl"
              aria-hidden
            />

            <div className="relative ml-auto w-[340px] rounded-[3.2rem] border border-[#c5c6cd] bg-[#08111f] p-3 shadow-[0_38px_88px_-34px_rgba(9,20,38,0.5)]">
              <div className="relative overflow-hidden rounded-[2.6rem] border border-white/10 bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.13),_transparent_48%),linear-gradient(180deg,#121d31_0%,#080d17_100%)] px-6 pb-8 pt-10">
                <div
                  className="absolute left-1/2 top-4 h-1.5 w-[4.5rem] -translate-x-1/2 rounded-full bg-white/16"
                  aria-hidden
                />

                <div className="rounded-[2rem] border border-white/10 bg-white/[0.03] p-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.08)]">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-[10px] uppercase tracking-[0.18em] text-white/55">
                        Final exam
                      </p>
                      <p className="mt-2 text-lg font-semibold tracking-[-0.04em] text-white">
                        Credential review
                      </p>
                    </div>
                    <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-white/10">
                      <Languages className="h-5 w-5 text-white/85" />
                    </span>
                  </div>

                  <div className="mt-8 rounded-[1.8rem] border border-white/10 bg-[#0d1526] p-5">
                    <div className="aspect-[3/4] rounded-[1.4rem] border border-white/8 bg-[linear-gradient(160deg,rgba(255,255,255,0.08),rgba(255,255,255,0.01)),linear-gradient(180deg,#0b1220_0%,#05070e_100%)] p-5">
                      <div className="flex h-full flex-col justify-between">
                        <div className="space-y-2">
                          <div className="h-2.5 w-[4.5rem] rounded-full bg-white/18" />
                          <div className="h-2.5 w-24 rounded-full bg-white/10" />
                        </div>

                        <div className="space-y-4">
                          <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-full border border-white/10 bg-white/4">
                            <motion.div
                              animate={{ scale: [1, 1.08, 1] }}
                              transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
                              className="flex h-14 w-14 items-center justify-center rounded-full border border-white/12 bg-white/8"
                            >
                              <CheckCircle2 className="h-7 w-7 text-white/75" />
                            </motion.div>
                          </div>
                          <div className="space-y-2 text-center">
                            <p className="text-[10px] uppercase tracking-[0.18em] text-white/50">
                              Verified assessment
                            </p>
                            <p className="text-sm leading-6 text-white/72">
                              Scored on the server. Issued with a public verification trail.
                            </p>
                          </div>
                        </div>

                        <div className="grid gap-2">
                          <div className="h-2 rounded-full bg-white/10">
                            <motion.div
                              initial={{ width: "0%" }}
                              animate={{ width: "82%" }}
                              transition={{ duration: 1.8, delay: 1.2, ease: [0.16, 1, 0.3, 1] }}
                              className="h-2 rounded-full bg-white/70"
                            />
                          </div>
                          <div className="flex justify-between text-[10px] uppercase tracking-[0.18em] text-white/45">
                            <span>Progress</span>
                            <span>82%</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div
                  className="absolute bottom-10 right-[-22px] h-28 w-2 rounded-full bg-white/18"
                  aria-hidden
                />
              </div>
            </div>

            <motion.div
              initial={{ opacity: 0, x: -30, y: 20 }}
              animate={{ opacity: 1, x: 0, y: 0 }}
              transition={{ duration: 0.9, delay: 0.8, ease: [0.16, 1, 0.3, 1] }}
              className="absolute left-0 top-[56%] w-[188px] rounded-[1.75rem] border border-[#dfe3e7] bg-white p-4 shadow-[0_24px_54px_-30px_rgba(9,20,38,0.35)]"
            >
              <div className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-[#eff4fb] text-primary">
                <CheckCircle2 className="h-4.5 w-4.5" />
              </div>
              <p className="mt-4 text-sm font-semibold tracking-[-0.03em] text-primary">
                Authentic proof
              </p>
              <p className="mt-1 text-sm leading-6 text-[#505f76]">
                Every completion produces a public verification route and issuer trail.
              </p>
            </motion.div>
          </motion.div>
        </Parallax>
      </div>
    </section>
  );
}
