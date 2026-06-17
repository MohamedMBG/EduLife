import { Link } from "@tanstack/react-router";
import { Globe, Instagram, Linkedin, Mail, MapPin } from "lucide-react";
import { toast } from "sonner";
import { motion } from "framer-motion";
import { ScrollReveal } from "./animations";

const PROGRAM_LINKS = [
  { label: "Engineering", href: "#programs" },
  { label: "Design", href: "#programs" },
  { label: "Leadership", href: "#programs" },
];

const FOUNDATION_LINKS = [
  { label: "Philosophy", href: "#philosophy" },
  { label: "Admissions", href: "#admissions" },
  { label: "Scholarships", href: "#admissions" },
];

export function PublicFooter() {
  function handlePlaceholderPolicy(label: string) {
    toast("Coming soon", {
      description: `${label} will be published with the public admissions launch.`,
    });
  }

  return (
    <footer className="border-t border-[#dfe3e7] px-5 pb-10 pt-16 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-[1280px]">
        <div className="grid gap-14 border-b border-[#dfe3e7] pb-14 lg:grid-cols-[1.3fr_0.7fr_0.7fr_0.8fr]">
          <ScrollReveal delay={0}>
            <div className="max-w-[360px]">
              <p className="text-[1.55rem] font-semibold tracking-[-0.05em] text-primary">EduLife</p>
              <p className="mt-4 text-sm leading-7 text-[#505f76]">
                High-structure learning for Moroccan learners who want one credible route from course
                discovery to graduation.
              </p>

              <div className="mt-6 flex items-center gap-3">
                {[
                  { href: "mailto:hello@edulife.ma", label: "Email EduLife", icon: Mail },
                  { href: "#philosophy", label: "View EduLife philosophy", icon: Globe },
                  {
                    href: "mailto:hello@edulife.ma?subject=EduLife%20Social",
                    label: "Contact EduLife on Instagram",
                    icon: Instagram,
                  },
                  {
                    href: "mailto:hello@edulife.ma?subject=EduLife%20LinkedIn",
                    label: "Contact EduLife on LinkedIn",
                    icon: Linkedin,
                  },
                ].map((social, i) => {
                  const Icon = social.icon;
                  return (
                    <motion.a
                      key={social.label}
                      href={social.href}
                      aria-label={social.label}
                      whileHover={{ y: -3, scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      transition={{ type: "spring", stiffness: 400, damping: 17 }}
                      className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-[#c5c6cd] bg-white text-primary transition-colors hover:bg-[#eef3f8]"
                    >
                      <Icon className="h-4 w-4" />
                    </motion.a>
                  );
                })}
              </div>
            </div>
          </ScrollReveal>

          <ScrollReveal delay={0.08}>
            <FooterColumn title="Programs" links={PROGRAM_LINKS} />
          </ScrollReveal>
          <ScrollReveal delay={0.16}>
            <FooterColumn title="Foundation" links={FOUNDATION_LINKS} />
          </ScrollReveal>

          <ScrollReveal delay={0.24}>
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#768397]">
                Office
              </p>
              <a
                href="mailto:hello@edulife.ma"
                className="mt-5 inline-flex items-center gap-2 text-sm font-medium text-primary transition-colors hover:text-[#505f76]"
              >
                <Mail className="h-4 w-4" />
                hello@edulife.ma
              </a>
              <p className="mt-4 inline-flex items-start gap-2 text-sm leading-7 text-[#505f76]">
                <MapPin className="mt-1 h-4 w-4 shrink-0 text-primary" />
                <span>Casablanca, Morocco</span>
              </p>
            </div>
          </ScrollReveal>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          className="flex flex-col gap-5 pt-8 text-[11px] font-medium uppercase tracking-[0.16em] text-[#768397] md:flex-row md:items-center md:justify-between"
        >
          <p>Copyright 2026 EduLife Academy. All credentials reserved.</p>
          <div className="flex flex-wrap items-center gap-5">
            <button
              type="button"
              onClick={() => handlePlaceholderPolicy("Privacy")}
              className="transition-colors hover:text-primary"
            >
              Privacy
            </button>
            <button
              type="button"
              onClick={() => handlePlaceholderPolicy("Terms")}
              className="transition-colors hover:text-primary"
            >
              Terms
            </button>
            <Link
              to="/certificates/verify/$hash"
              params={{ hash: "demo-certificate-french-ui" }}
              className="transition-colors hover:text-primary"
            >
              Verify Certificate
            </Link>
          </div>
        </motion.div>
      </div>
    </footer>
  );
}

function FooterColumn({
  title,
  links,
}: {
  title: string;
  links: Array<{ label: string; href: string }>;
}) {
  return (
    <div>
      <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#768397]">{title}</p>
      <div className="mt-5 grid gap-3">
        {links.map((link) => (
          <motion.a
            key={link.label}
            href={link.href}
            className="text-sm text-[#505f76] transition-colors hover:text-primary"
            whileHover={{ x: 4 }}
            transition={{ type: "spring", stiffness: 300, damping: 20 }}
          >
            {link.label}
          </motion.a>
        ))}
      </div>
    </div>
  );
}
