import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";

const langs = [
  {
    code: "DR",
    label: "Darija",
    note: "Conversational and familiar — the language learners actually think in.",
    badgeClass: "bg-gradient-gold text-gold-foreground",
    accentLine: "from-gold/60",
  },
  {
    code: "FR",
    label: "Français",
    note: "Academic and professional, mapped to the way schools and employers test.",
    badgeClass: "bg-gradient-primary text-primary-foreground",
    accentLine: "from-primary/60",
  },
  {
    code: "EN",
    label: "English",
    note: "Global and technical — covers the tech and certification landscape.",
    badgeClass: "bg-gradient-to-br from-teal to-[oklch(0.55_0.15_185)] text-teal-foreground",
    accentLine: "from-teal/60",
  },
];

export function Morocco() {
  return (
    <section id="morocco" className="relative py-32 lg:py-44 overflow-hidden">
      <div className="absolute right-[-12%] top-1/2 -z-10 h-[460px] w-[460px] -translate-y-1/2 rounded-full bg-gradient-aurora blur-3xl opacity-40" />

      <div className="mx-auto max-w-7xl px-6 lg:px-10 grid lg:grid-cols-12 gap-14 lg:gap-20 items-center">
        <div className="lg:col-span-5">
          <SectionLabel>Built for Morocco</SectionLabel>
          <SectionHeadline>
            Closer to{" "}
            <span className="italic font-normal text-muted-foreground">
              the way Morocco learns.
            </span>
          </SectionHeadline>
          <p className="mt-8 max-w-[48ch] text-lg leading-relaxed text-muted-foreground">
            EduLife is designed for multilingual, mobile-first learning. Darija, Français,
            and English support are not afterthought tabs — every screen, exam, and
            certificate respects the learner's language.
          </p>

          <div className="mt-10 flex items-center gap-4 text-sm text-muted-foreground">
            <span className="inline-flex h-2 w-2 rounded-full bg-primary animate-pulse" />
            Currently piloting with learners across Casablanca, Rabat, and Marrakech.
          </div>
        </div>

        <div className="lg:col-span-7 space-y-4">
          {langs.map((l, i) => (
            <motion.div
              key={l.code}
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-60px" }}
              transition={{ duration: 0.65, delay: i * 0.1, ease: [0.16, 1, 0.3, 1] }}
              className="group relative overflow-hidden rounded-2xl hairline bg-surface-elevated p-2 lg:p-2.5"
              style={{ marginLeft: `${i * 24}px` }}
            >
              <div
                className={`absolute inset-y-0 left-0 w-[3px] bg-gradient-to-b ${l.accentLine} to-transparent`}
              />
              <div className="flex items-center gap-5 rounded-xl p-4 lg:p-5 transition-colors duration-500 group-hover:bg-accent/50">
                <div
                  className={`grid h-16 w-16 shrink-0 place-items-center rounded-2xl text-display text-xl shadow-bezel ${l.badgeClass}`}
                >
                  {l.code}
                </div>
                <div className="min-w-0">
                  <p className="text-display text-2xl text-foreground leading-tight">{l.label}</p>
                  <p className="mt-1 text-sm leading-relaxed text-muted-foreground max-w-[44ch]">
                    {l.note}
                  </p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
