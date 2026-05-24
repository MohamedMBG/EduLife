import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import heroDevice from "@/assets/hero-device.png";

export function Hero() {
  return (
    <section id="top" className="relative pt-32 pb-24 lg:pt-44 lg:pb-32 overflow-hidden">
      {/* Ambient glow */}
      <div className="absolute inset-0 -z-10 bg-hero-gradient" />
      <div className="absolute left-1/2 top-20 -z-10 h-[560px] w-[860px] -translate-x-1/2 rounded-full bg-gradient-aurora blur-3xl opacity-70 animate-glow" />

      <div className="mx-auto max-w-6xl px-6 lg:px-10 text-center">
        <motion.span
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="inline-flex items-center gap-2 rounded-full border border-border/80 bg-surface-elevated px-4 py-1.5 text-xs tracking-wide text-muted-foreground"
        >
          <span className="h-1.5 w-1.5 rounded-full bg-gold" />
          Introducing EduLife — built for Moroccan learners
        </motion.span>

        <motion.h1
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.1 }}
          className="text-display mt-8 text-5xl sm:text-6xl lg:text-7xl leading-[1.02] text-foreground max-w-4xl mx-auto"
        >
          One clear path to learn,
          <span className="block bg-gradient-to-r from-primary via-primary-glow to-teal bg-clip-text text-transparent">
            pass, and grow.
          </span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.25 }}
          className="mt-7 text-lg sm:text-xl text-muted-foreground max-w-2xl mx-auto leading-relaxed"
        >
          EduLife brings courses, lessons, progress, exams, and certificates into one
          guided learning experience built for Moroccan learners.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.4 }}
          className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-3"
        >
          <a
            href="#cta"
            className="group inline-flex h-12 items-center gap-2 rounded-full bg-foreground text-background px-7 text-sm font-medium shadow-elevated hover:scale-[1.02] transition-transform"
          >
            Get Early Access
            <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
          </a>
          <a
            href="#journey"
            className="inline-flex h-12 items-center rounded-full border border-border bg-surface-elevated px-7 text-sm font-medium text-foreground hover:bg-accent transition-colors"
          >
            Explore the Journey
          </a>
        </motion.div>

        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 1, delay: 0.6 }}
          className="mt-6 text-sm text-muted-foreground/80 tracking-wide"
        >
          Structured learning · Real progress · Verified achievement
        </motion.p>

        {/* Device */}
        <motion.div
          initial={{ opacity: 0, y: 60 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1.1, delay: 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="relative mt-20 mx-auto max-w-[520px]"
        >
          <div className="absolute -inset-20 -z-10 bg-gradient-aurora blur-3xl opacity-60" />
          <div className="animate-float">
            <img
              src={heroDevice}
              alt="EduLife mobile app showing course progress and certificate"
              width={1024}
              height={1280}
              className="w-full h-auto drop-shadow-[0_40px_80px_rgba(30,40,90,0.25)]"
            />
          </div>
        </motion.div>
      </div>
    </section>
  );
}
