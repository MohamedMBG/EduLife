import { PublicCertificatesSection } from "./PublicCertificatesSection";
import { PublicConflictSection } from "./PublicConflictSection";
import { PublicFooter } from "./PublicFooter";
import { PublicHeroSection } from "./PublicHeroSection";
import { PublicMethodologySection } from "./PublicMethodologySection";
import { PublicMobileLearningSection } from "./PublicMobileLearningSection";
import { PublicNavbar } from "./PublicNavbar";
import { PublicWaitlistCTA } from "./PublicWaitlistCTA";

export function PublicLandingPage() {
  return (
    <div className="relative min-h-[100dvh] bg-background text-foreground antialiased">
      <a
        href="#main-content"
        className="sr-only rounded-full bg-primary px-4 py-2 text-sm font-medium text-white focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70]"
      >
        Skip to content
      </a>
      <div className="noise-overlay" aria-hidden />
      <PublicNavbar />
      <main id="main-content">
        <PublicHeroSection />
        <PublicConflictSection />
        <PublicMethodologySection />
        <PublicCertificatesSection />
        <PublicWaitlistCTA />
        <PublicMobileLearningSection />
      </main>
      <PublicFooter />
    </div>
  );
}
