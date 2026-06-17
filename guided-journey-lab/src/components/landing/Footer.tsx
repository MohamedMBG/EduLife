import { GraduationCap, Twitter, Instagram, Linkedin, Youtube, ArrowUpRight } from "lucide-react";

const nav = [
  {
    title: "Product",
    items: [
      ["Features", "#features"],
      ["Journey", "#journey"],
      ["Certificate", "#certificate"],
      ["Morocco", "#morocco"],
    ],
  },
  {
    title: "Company",
    items: [
      ["About", "#"],
      ["Pilot cohort", "#cta"],
      ["Contact", "mailto:hello@edulife.ma"],
    ],
  },
  {
    title: "Legal",
    items: [
      ["Privacy", "#"],
      ["Terms", "#"],
      ["Verify certificate", "#"],
    ],
  },
];

const socials = [
  { Icon: Twitter, label: "Twitter" },
  { Icon: Instagram, label: "Instagram" },
  { Icon: Linkedin, label: "LinkedIn" },
  { Icon: Youtube, label: "YouTube" },
];

export function Footer() {
  return (
    <footer className="relative border-t border-border bg-surface overflow-hidden">
      <div className="absolute -top-32 left-1/2 -translate-x-1/2 h-64 w-[800px] rounded-full bg-gradient-aurora blur-3xl opacity-30 pointer-events-none" />

      <div className="relative mx-auto max-w-7xl px-6 lg:px-10 py-20">
        <div className="grid lg:grid-cols-12 gap-12 lg:gap-10">
          <div className="lg:col-span-4">
            <div className="flex items-center gap-2.5">
              <span className="grid place-items-center h-10 w-10 rounded-xl bg-gradient-primary text-primary-foreground shadow-bezel">
                <GraduationCap className="h-5 w-5" strokeWidth={1.5} />
              </span>
              <span className="text-display text-xl">EduLife</span>
            </div>
            <p className="mt-5 max-w-xs text-sm text-muted-foreground leading-relaxed">
              Structured learning for real progress — designed in Morocco, for Morocco.
            </p>

            <a
              href="mailto:hello@edulife.ma"
              className="group mt-8 inline-flex items-center gap-2 text-sm text-foreground/85 hover:text-foreground transition-colors"
            >
              <span className="relative">
                hello@edulife.ma
                <span className="absolute -bottom-0.5 left-0 h-px w-full bg-foreground/30 group-hover:bg-primary transition-colors duration-500" />
              </span>
              <ArrowUpRight className="h-3.5 w-3.5" strokeWidth={1.75} />
            </a>
          </div>

          <div className="lg:col-span-8 grid grid-cols-2 sm:grid-cols-3 gap-8 text-sm">
            {nav.map((column) => (
              <div key={column.title}>
                <p className="text-[10px] uppercase tracking-[0.2em] font-mono text-muted-foreground">
                  {column.title}
                </p>
                <ul className="mt-5 space-y-3">
                  {column.items.map(([label, href]) => (
                    <li key={label}>
                      <a
                        href={href}
                        className="text-foreground/75 hover:text-foreground transition-colors"
                      >
                        {label}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <div className="mt-16 pt-8 border-t border-border/60 flex flex-col-reverse sm:flex-row gap-6 sm:items-center sm:justify-between">
          <p className="text-xs text-muted-foreground">
            © {new Date().getFullYear()} EduLife. Made with care for Moroccan learners.
          </p>
          <div className="flex items-center gap-2">
            {socials.map(({ Icon, label }) => (
              <a
                key={label}
                href="#"
                className="grid h-10 w-10 place-items-center rounded-full hairline bg-surface-elevated text-muted-foreground hover:text-foreground hover:-translate-y-0.5 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
                aria-label={label}
              >
                <Icon className="h-4 w-4" strokeWidth={1.5} />
              </a>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
