import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";
import { Layers, TrendingUp, FileCheck, Medal, Smartphone, Languages } from "lucide-react";

const features = [
  { icon: Layers, title: "Structured Courses", desc: "Clear learning paths instead of scattered content." },
  { icon: TrendingUp, title: "Lesson Progress", desc: "Track what you finished and what comes next." },
  { icon: FileCheck, title: "Final Exam", desc: "Prove understanding through a real completion step." },
  { icon: Medal, title: "Earned Certificates", desc: "Certificates are unlocked by passing, not by clicking through." },
  { icon: Smartphone, title: "Mobile First", desc: "Built for the way learners actually study today." },
  { icon: Languages, title: "Multilingual Access", desc: "Designed for Darija, French, and English." },
];

export function Features() {
  return (
    <section id="features" className="relative py-28 lg:py-40">
      <div className="mx-auto max-w-6xl px-6 lg:px-10">
        <div className="max-w-3xl">
          <SectionLabel>What makes it different</SectionLabel>
          <SectionHeadline>Everything important. Nothing fragmented.</SectionHeadline>
        </div>

        <div className="mt-16 grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {features.map((f, i) => {
            const Icon = f.icon;
            return (
              <motion.div
                key={f.title}
                initial={{ opacity: 0, y: 24 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-60px" }}
                transition={{ duration: 0.6, delay: i * 0.06 }}
                className="group relative rounded-3xl border border-border bg-surface-elevated p-8 hover:border-primary/30 hover:shadow-elevated transition-all duration-500"
              >
                <div className="grid h-12 w-12 place-items-center rounded-xl bg-primary/5 text-primary group-hover:bg-gradient-primary group-hover:text-primary-foreground transition-all duration-500">
                  <Icon className="h-5 w-5" strokeWidth={1.75} />
                </div>
                <h3 className="mt-7 text-xl text-display text-foreground">{f.title}</h3>
                <p className="mt-3 text-muted-foreground leading-relaxed">{f.desc}</p>
              </motion.div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
