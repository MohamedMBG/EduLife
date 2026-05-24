import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowRight, CheckCircle } from "lucide-react";

export function FinalCTA() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!email) return;
    // TODO: wire to /api/waitlist
    setSubmitted(true);
  }

  return (
    <section id="cta" className="relative py-28 lg:py-40 overflow-hidden">
      <div className="mx-auto max-w-5xl px-6 lg:px-10">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.8 }}
          className="relative rounded-[2.5rem] bg-gradient-to-br from-primary via-primary to-primary-glow text-primary-foreground p-12 sm:p-16 lg:p-24 text-center overflow-hidden shadow-glow"
        >
          <div className="absolute -top-32 -left-32 h-80 w-80 rounded-full bg-gold/30 blur-3xl" />
          <div className="absolute -bottom-32 -right-32 h-80 w-80 rounded-full bg-teal/30 blur-3xl" />

          <h2 className="relative text-display text-4xl sm:text-5xl lg:text-6xl leading-tight max-w-3xl mx-auto">
            Start the smarter way to learn.
          </h2>
          <p className="relative mt-6 text-lg text-primary-foreground/80 max-w-xl mx-auto leading-relaxed">
            One platform. One path. A better way to build skills and prove progress.
          </p>

          <div className="relative mt-10 flex flex-col items-center gap-4">
            <AnimatePresence mode="wait">
              {submitted ? (
                <motion.div
                  key="success"
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  transition={{ duration: 0.3 }}
                  className="flex items-center gap-3 text-lg font-medium text-gold"
                >
                  <CheckCircle className="h-5 w-5" />
                  You're on the list — we'll be in touch.
                </motion.div>
              ) : (
                <motion.form
                  key="form"
                  onSubmit={handleSubmit}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="flex flex-col sm:flex-row gap-3 w-full max-w-md"
                >
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@email.com"
                    className="h-12 flex-1 rounded-full bg-primary-foreground/15 border border-primary-foreground/25 px-5 text-sm text-primary-foreground placeholder:text-primary-foreground/50 outline-none focus:border-gold focus:bg-primary-foreground/20 transition-all"
                  />
                  <button
                    type="submit"
                    className="group inline-flex h-12 shrink-0 items-center gap-2 rounded-full bg-gold text-gold-foreground px-7 text-sm font-medium shadow-gold hover:scale-[1.02] transition-transform"
                  >
                    Join the Waitlist
                    <ArrowRight className="h-4 w-4 group-hover:translate-x-0.5 transition-transform" />
                  </button>
                </motion.form>
              )}
            </AnimatePresence>

            <a
              href="#journey"
              className="inline-flex h-12 items-center rounded-full border border-primary-foreground/25 px-7 text-sm font-medium text-primary-foreground hover:bg-primary-foreground/10 transition-colors"
            >
              See How It Works
            </a>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
