import { GraduationCap, Twitter, Instagram, Linkedin, Youtube } from "lucide-react";

export function Footer() {
  return (
    <footer className="border-t border-border bg-surface">
      <div className="mx-auto max-w-7xl px-6 lg:px-10 py-16 grid md:grid-cols-3 gap-12">
        <div>
          <div className="flex items-center gap-2">
            <span className="grid place-items-center h-8 w-8 rounded-lg bg-gradient-primary text-primary-foreground">
              <GraduationCap className="h-4 w-4" />
            </span>
            <span className="text-display text-lg">EduLife</span>
          </div>
          <p className="mt-4 text-sm text-muted-foreground max-w-xs leading-relaxed">
            Structured learning for real progress.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-8 text-sm">
          {[
            ["About", "#"],
            ["Courses", "#"],
            ["Certificates", "#certificate"],
            ["Contact", "#"],
            ["Privacy", "#"],
          ].map(([label, href]) => (
            <a key={label} href={href} className="text-muted-foreground hover:text-foreground transition-colors">
              {label}
            </a>
          ))}
        </div>

        <div className="flex md:justify-end items-start gap-3">
          {[
            { Icon: Twitter, label: "Twitter" },
            { Icon: Instagram, label: "Instagram" },
            { Icon: Linkedin, label: "LinkedIn" },
            { Icon: Youtube, label: "YouTube" },
          ].map(({ Icon, label }) => (
            <a
              key={label}
              href="#"
              className="grid h-10 w-10 place-items-center rounded-full border border-border bg-surface-elevated text-muted-foreground hover:text-foreground hover:border-primary/30 transition-colors"
              aria-label={label}
            >
              <Icon className="h-4 w-4" />
            </a>
          ))}
        </div>
      </div>
      <div className="border-t border-border">
        <div className="mx-auto max-w-7xl px-6 lg:px-10 py-6 flex flex-col sm:flex-row gap-3 justify-between text-xs text-muted-foreground">
          <p>© {new Date().getFullYear()} EduLife. All rights reserved.</p>
          <p>Made with care for Moroccan learners.</p>
        </div>
      </div>
    </footer>
  );
}
