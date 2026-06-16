import { motion } from "framer-motion";
import { ArrowUpRight, Sparkle, ShieldCheck, Languages } from "lucide-react";
import heroDevice from "@/assets/hero-device.png";

const trustPoints = [
  { icon: ShieldCheck, label: "Backend-graded exams" },
  { icon: Languages, label: "Darija · FR · EN" },
  { icon: Sparkle, label: "Earned, not awarded" },
];

export function Hero() {
  return (
    <section
      id="top"
      className="relative min-h-[100dvh] overflow-hidden pt-32 pb-24 lg:pt-40 lg:pb-32"
    >
      <div className="absolute inset-0 -z-10 bg-hero-gradient" />
      <div className="absolute left-1/2 top-32 -z-10 h-[520px] w-[820px] -translate-x-1/2 rounded-full bg-gradient-aurora blur-3xl opacity-60 animate-glow" />
      <div className="absolute inset-x-0 top-0 -z-10 h-[60vh] bg-[radial-gradient(60%_50%_at_50%_0%,oklch(0.40_0.19_152/0.10),transparent)]" />

      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <div className="grid lg:grid-cols-12 gap-10 lg:gap-6 items-center">
          <div className="lg:col-span-7 relative">
            <motion.span
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
              className="eyebrow eyebrow-dot"
            >
              Built for Moroccan learners
            </motion.span>

            <motion.h1
              initial={{ opacity: 0, y: 24, filter: "blur(8px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{ duration: 0.95, delay: 0.08, ease: [0.16, 1, 0.3, 1] }}
              className="text-display mt-6 text-[clamp(2.75rem,6vw,5.5rem)] leading-[0.96] text-foreground"
            >
              One clear path
              <span className="block">
                to learn,{" "}
                <span className="italic font-normal text-primary">pass</span>
                ,
              </span>
              <span className="block text-foreground/80">and graduate.</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0, y: 18 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.22, ease: [0.16, 1, 0.3, 1] }}
              className="mt-8 max-w-[52ch] text-lg sm:text-xl leading-relaxed text-muted-foreground"
            >
              Courses, lessons, progress, graded exams, and verified certificates —
              in one guided learning experience designed for the way Morocco studies.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.36, ease: [0.16, 1, 0.3, 1] }}
              className="mt-10 flex flex-wrap items-center gap-3"
            >
              <a
                href="#cta"
                className="group relative inline-flex h-12 items-center gap-1.5 rounded-full bg-foreground text-background pl-6 pr-1.5 text-sm font-medium shadow-bezel transition-transform duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02] active:scale-[0.98]"
              >
                <span>Get Early Access</span>
                <span className="grid h-9 w-9 place-items-center rounded-full bg-background/15 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:bg-background/25 group-hover:translate-x-0.5 group-hover:-translate-y-px">
                  <ArrowUpRight className="h-4 w-4" strokeWidth={1.75} />
                </span>
              </a>
              <a
                href="#journey"
                className="group inline-flex h-12 items-center gap-2 rounded-full px-6 text-sm font-medium text-foreground/85 hover:text-foreground transition-colors"
              >
                <span className="relative">
                  Explore the journey
                  <span className="absolute -bottom-0.5 left-0 h-px w-0 bg-foreground transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:w-full" />
                </span>
              </a>
            </motion.div>

            <motion.ul
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.9, delay: 0.5, ease: [0.16, 1, 0.3, 1] }}
              className="mt-12 flex flex-wrap gap-x-7 gap-y-3 text-[13px] text-muted-foreground"
            >
              {trustPoints.map(({ icon: Icon, label }) => (
                <li key={label} className="inline-flex items-center gap-2">
                  <Icon className="h-3.5 w-3.5 text-primary/80" strokeWidth={1.5} />
                  {label}
                </li>
              ))}
            </motion.ul>
          </div>

          <div className="lg:col-span-5 relative">
            <motion.div
              initial={{ opacity: 0, y: 50, rotate: 2 }}
              animate={{ opacity: 1, y: 0, rotate: -1.5 }}
              transition={{ duration: 1.1, delay: 0.4, ease: [0.16, 1, 0.3, 1] }}
              className="relative mx-auto max-w-[460px] lg:ml-auto lg:mr-0"
            >
              <div className="absolute -inset-12 -z-10 bg-gradient-aurora blur-3xl opacity-50" />

              {/* floating proof card top-left */}
              <motion.div
                initial={{ opacity: 0, y: -10, x: -10 }}
                animate={{ opacity: 1, y: 0, x: 0 }}
                transition={{ duration: 0.9, delay: 0.9, ease: [0.16, 1, 0.3, 1] }}
                className="absolute -left-4 lg:-left-16 top-10 z-20 max-w-[200px] rounded-2xl glass shadow-elevated p-3.5"
              >
                <p className="text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
                  Pass threshold
                </p>
                <p className="mt-1.5 text-display text-2xl leading-none">80%</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Backend-graded MCQ exam — no client trust.
                </p>
              </motion.div>

              {/* floating proof card bottom-right */}
              <motion.div
                initial={{ opacity: 0, y: 20, x: 10 }}
                animate={{ opacity: 1, y: 0, x: 0 }}
                transition={{ duration: 0.9, delay: 1.05, ease: [0.16, 1, 0.3, 1] }}
                className="absolute -right-4 lg:-right-12 bottom-12 z-20 flex items-center gap-3 rounded-full glass shadow-elevated px-3 py-2"
              >
                <span className="grid h-7 w-7 place-items-center rounded-full bg-gradient-gold text-gold-foreground">
                  <Sparkle className="h-3.5 w-3.5" strokeWidth={1.75} />
                </span>
                <div className="leading-tight">
                  <p className="text-[10px] uppercase tracking-[0.16em] text-muted-foreground">
                    Verified
                  </p>
                  <p className="text-xs font-medium text-foreground">Certificate #07-LK4</p>
                </div>
              </motion.div>

              <div className="bezel animate-float">
                <div className="bezel-inner overflow-hidden">
                  <img
                    src={heroDevice}
                    alt="EduLife mobile app showing course progress, lesson tracking, and certificate"
                    width={1024}
                    height={1280}
                    className="w-full h-auto"
                  />
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </section>
  );
}
