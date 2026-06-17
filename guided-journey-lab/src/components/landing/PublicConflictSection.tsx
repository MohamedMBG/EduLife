import { CheckCircle2 } from "lucide-react";
import { motion } from "framer-motion";
import { TextClipReveal, ScrollReveal } from "./animations";

const STANDARD_POINTS = [
  {
    title: "Curated linear experience",
    description: "One guided sequence instead of jumping between playlists, PDFs, and chat groups.",
  },
  {
    title: "Server-side validation",
    description:
      "Progress and outcomes are confirmed in the platform instead of guessed on the client.",
  },
  {
    title: "Proctored graded exams",
    description: "The final checkpoint measures mastery before any certificate is issued.",
  },
];

export function PublicConflictSection() {
  return (
    <section id="philosophy" className="px-5 py-20 sm:px-6 lg:px-8 lg:py-28">
      <div className="mx-auto grid max-w-[1280px] gap-10 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.02fr)] lg:items-center lg:gap-16">
        <ScrollReveal direction="up">
          <div className="max-w-[560px]">
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#505f76]">
              The conflict
            </p>
            <TextClipReveal>
              <h2 className="mt-5 max-w-[11ch] pb-2 text-[clamp(2.4rem,5vw,4.35rem)] font-light leading-[1.06] tracking-[-0.05em] text-primary">
                The Internet is a library <span className="italic text-[#505f76]">without a map.</span>
              </h2>
            </TextClipReveal>

            <ScrollReveal delay={0.15}>
              <div className="mt-10 rounded-[2rem] border border-[#dfe3e7] bg-white p-7 shadow-[0_22px_56px_-42px_rgba(9,20,38,0.3)]">
                <div className="flex items-center justify-between gap-4">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#a16a6a]">
                    The old way
                  </p>
                  <span
                    className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-[#f9ecec] text-[#b26c6c]"
                    aria-hidden
                  >
                    <span className="text-sm leading-none">x</span>
                  </span>
                </div>
                <h3 className="mt-6 text-[1.6rem] font-semibold tracking-[-0.04em] text-primary">
                  Fragmented &amp; shallow
                </h3>
                <p className="mt-4 max-w-[42ch] text-[0.98rem] leading-7 text-[#505f76]">
                  Random playlists, copied notes, and disconnected advice create motion without
                  direction. Learners spend more time sorting material than building mastery.
                </p>
              </div>
            </ScrollReveal>
          </div>
        </ScrollReveal>

        <ScrollReveal direction="right" delay={0.1}>
          <article
            className="relative overflow-hidden rounded-[2.3rem] bg-primary p-8 text-white shadow-[0_32px_88px_-34px_rgba(9,20,38,0.58)] sm:p-10 lg:p-12"
          >
            <motion.div
              animate={{ scale: [1, 1.15, 1], rotate: [0, 5, 0] }}
              transition={{ duration: 12, repeat: Infinity, ease: "easeInOut" }}
              className="absolute right-[-14px] top-[-12px] h-40 w-40 rounded-full border border-white/8 bg-white/[0.03]"
              aria-hidden
            />
            <motion.div
              animate={{ scale: [1, 1.1, 1], x: [0, 10, 0] }}
              transition={{ duration: 15, repeat: Infinity, ease: "easeInOut" }}
              className="absolute bottom-[-52px] right-10 h-48 w-48 rounded-full border border-white/8"
              aria-hidden
            />

            <div className="relative z-10">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <p className="text-[10px] font-semibold uppercase tracking-[0.24em] text-white/62">
                  The EduLife standard
                </p>
                <span className="rounded-full border border-white/12 bg-white/8 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.18em] text-white/80">
                  Verified path
                </span>
              </div>

              <h3 className="mt-8 max-w-[10ch] text-[clamp(2rem,4vw,3.4rem)] font-semibold leading-[1.04] tracking-[-0.05em] text-white">
                Mastery by architecture.
              </h3>

              <div className="mt-10 space-y-6">
                {STANDARD_POINTS.map((point, index) => (
                  <motion.div
                    key={point.title}
                    initial={{ opacity: 0, x: 24 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true, margin: "-60px" }}
                    transition={{
                      duration: 0.7,
                      delay: 0.15 + index * 0.1,
                      ease: [0.16, 1, 0.3, 1],
                    }}
                    className="flex gap-4"
                  >
                    <span className="mt-0.5 inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border border-white/10 bg-white/7">
                      <CheckCircle2 className="h-4.5 w-4.5 text-white/88" />
                    </span>
                    <div>
                      <p className="text-lg font-semibold tracking-[-0.03em] text-white">
                        {point.title}
                      </p>
                      <p className="mt-1 max-w-[34ch] text-sm leading-7 text-white/68">
                        {point.description}
                      </p>
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          </article>
        </ScrollReveal>
      </div>
    </section>
  );
}
