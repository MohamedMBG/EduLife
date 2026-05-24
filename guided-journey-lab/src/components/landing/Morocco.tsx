import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";

const langs = [
  {
    code: "DR",
    label: "Darija",
    note: "Conversational and familiar",
    badgeClass: "bg-gradient-gold text-gold-foreground",
  },
  {
    code: "FR",
    label: "Français",
    note: "Academic and professional",
    badgeClass: "bg-gradient-to-br from-primary to-primary-glow text-primary-foreground",
  },
  {
    code: "EN",
    label: "English",
    note: "Global and technical",
    badgeClass: "bg-gradient-to-br from-teal to-[oklch(0.55_0.15_185)] text-teal-foreground",
  },
];

export function Morocco() {
  return (
    <section id="morocco" className="relative py-28 lg:py-40">
      <div className="mx-auto max-w-6xl px-6 lg:px-10 grid lg:grid-cols-2 gap-16 items-center">
        <div>
          <SectionLabel>Built for Morocco</SectionLabel>
          <SectionHeadline>Closer to the way Morocco learns.</SectionHeadline>
          <p className="mt-6 text-lg text-muted-foreground leading-relaxed max-w-lg">
            EduLife is designed for multilingual, mobile-first learning with a simpler
            path for students across Morocco. Darija, French, and English support help
            more learners move forward with confidence.
          </p>
        </div>

        <div className="space-y-4">
          {langs.map((l, i) => (
            <motion.div
              key={l.code}
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-60px" }}
              transition={{ duration: 0.6, delay: i * 0.1 }}
              className="flex items-center gap-5 rounded-2xl border border-border bg-surface-elevated p-6 hover:border-primary/30 transition-colors"
            >
              <div className={`grid h-14 w-14 place-items-center rounded-xl text-display text-lg ${l.badgeClass}`}>
                {l.code}
              </div>
              <div>
                <p className="text-xl text-display text-foreground">{l.label}</p>
                <p className="text-sm text-muted-foreground">{l.note}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
