import { motion } from "framer-motion";
import { SectionLabel, SectionHeadline } from "./Section";
import certImg from "@/assets/certificate.png";

export function Certificate() {
  return (
    <section id="certificate" className="relative py-28 lg:py-40 overflow-hidden">
      <div className="absolute left-1/2 top-1/2 -z-10 h-[500px] w-[700px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-gradient-gold blur-3xl opacity-20" />
      <div className="mx-auto max-w-6xl px-6 lg:px-10 grid lg:grid-cols-2 gap-16 items-center">
        <div>
          <SectionLabel>Proof of achievement</SectionLabel>
          <SectionHeadline>Progress should lead to something real.</SectionHeadline>
          <p className="mt-6 text-lg text-muted-foreground leading-relaxed max-w-lg">
            EduLife certificates are only issued after passing the final exam, creating
            a stronger signal of effort and completion.
          </p>
          <dl className="mt-10 grid grid-cols-2 gap-6 max-w-md">
            <div>
              <dt className="text-xs uppercase tracking-[0.2em] text-muted-foreground">Standard</dt>
              <dd className="mt-2 text-3xl text-display text-foreground">Exam</dd>
              <p className="text-sm text-muted-foreground">pass required</p>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-[0.2em] text-muted-foreground">Verified</dt>
              <dd className="mt-2 text-3xl text-display text-foreground">ID</dd>
              <p className="text-sm text-muted-foreground">unique per certificate</p>
            </div>
          </dl>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 40, rotate: -2 }}
          whileInView={{ opacity: 1, y: 0, rotate: -2 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 1, ease: [0.22, 1, 0.36, 1] }}
          className="relative"
        >
          <div className="absolute -inset-10 -z-10 bg-gradient-aurora blur-3xl opacity-60" />
          <div className="animate-float">
            <img
              src={certImg}
              alt="EduLife certificate of completion"
              width={1280}
              height={896}
              loading="lazy"
              className="w-full h-auto rounded-2xl shadow-elevated border border-border"
            />
          </div>
        </motion.div>
      </div>
    </section>
  );
}
