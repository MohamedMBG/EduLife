import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import {
  ArrowLeft,
  Award,
  CalendarDays,
  Download,
  GraduationCap,
  ShieldCheck,
  User,
} from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { ApiClientError, downloadCertificate, getCertificate } from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

function downloadErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    switch (error.status) {
      case 404:
        return "Certificate not found.";
      case 403:
        return "You do not have access to this certificate.";
      case 409:
        return "This certificate is not available for download.";
      default:
        return "PDF could not be generated. Please try again.";
    }
  }
  return "PDF could not be generated. Please try again.";
}

export const Route = createFileRoute("/certificates/$certificateId")({
  component: CertificateDetailRoute,
  head: () => ({ meta: [{ title: "Certificate - EduLife" }] }),
});

function CertificateDetailRoute() {
  return (
    <RequireAuth>
      <CertificateDetailPage />
    </RequireAuth>
  );
}

function CertificateDetailPage() {
  const { certificateId } = Route.useParams();
  const auth = useAuth();
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const certificateQuery = useQuery({
    queryKey: ["certificate", certificateId],
    queryFn: () => getCertificate(auth.getAccessToken, certificateId),
  });

  const downloadMutation = useMutation({
    mutationFn: async () => {
      const blob = await downloadCertificate(auth.getAccessToken, certificateId);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `certificate-${certificateId}.pdf`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    },
    onError: (error: unknown) => setDownloadError(downloadErrorMessage(error)),
    onSuccess: () => setDownloadError(null),
  });

  return (
    <AppLayout>
      {certificateQuery.isLoading ? (
        <StateCard
          title="Loading certificate..."
          detail="Fetching verified credential from backend."
        />
      ) : certificateQuery.isError ? (
        <StateCard title="Certificate unavailable" detail={certificateQuery.error.message} />
      ) : !certificateQuery.data ? (
        <StateCard title="Certificate not found" detail="No certificate exists for this id." />
      ) : (
        <section className="space-y-6">
          <article className="overflow-hidden rounded-3xl border border-gold/30 bg-gradient-gold p-8 shadow-gold">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <p className="inline-flex items-center gap-2 rounded-full bg-gold-foreground/10 px-3 py-1 text-[11px] uppercase tracking-[0.16em] text-gold-foreground">
                  <Award className="h-3.5 w-3.5" />
                  Verified certificate
                </p>
                <h1 className="mt-4 text-display text-3xl text-gold-foreground">
                  {certificateQuery.data.courseTitle}
                </h1>
                <p className="mt-2 text-sm text-gold-foreground/85">
                  Awarded to {certificateQuery.data.learnerName}
                </p>
              </div>
              <div className="grid h-16 w-16 place-items-center rounded-2xl bg-gold-foreground/10 text-gold-foreground">
                <GraduationCap className="h-8 w-8" />
              </div>
            </div>

            <dl className="mt-6 grid gap-4 sm:grid-cols-2">
              <DetailField
                icon={<ShieldCheck className="h-3.5 w-3.5" />}
                label="Certificate number"
                value={certificateQuery.data.certificateNumber}
              />
              <DetailField
                icon={<CalendarDays className="h-3.5 w-3.5" />}
                label="Issued at"
                value={new Date(certificateQuery.data.issuedAt).toLocaleDateString()}
              />
              <DetailField
                icon={<User className="h-3.5 w-3.5" />}
                label="Issued by"
                value={certificateQuery.data.teacherName}
              />
              <DetailField
                icon={<ShieldCheck className="h-3.5 w-3.5" />}
                label="Verification hash"
                value={certificateQuery.data.verificationHash}
                mono
              />
            </dl>
          </article>

          <div className="flex flex-col gap-3 rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm font-semibold text-foreground">Download as PDF</p>
              <p className="mt-1 text-xs text-muted-foreground">
                Backend-generated PDF, signed with the verification hash above.
              </p>
              {downloadError ? (
                <p className="mt-2 text-xs text-destructive">{downloadError}</p>
              ) : null}
            </div>
            <button
              type="button"
              onClick={() => downloadMutation.mutate()}
              disabled={downloadMutation.isPending}
              className="inline-flex items-center justify-center gap-2 rounded-full bg-primary px-5 py-2.5 text-xs font-semibold text-primary-foreground disabled:opacity-60"
            >
              <Download className="h-3.5 w-3.5" />
              {downloadMutation.isPending ? "Preparing..." : "Download PDF"}
            </button>
          </div>
        </section>
      )}
    </AppLayout>
  );
}

function DetailField({
  icon,
  label,
  value,
  mono,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="rounded-2xl bg-gold-foreground/8 p-4">
      <dt className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gold-foreground/75">
        {icon}
        {label}
      </dt>
      <dd
        className={
          "mt-2 break-all text-sm font-medium text-gold-foreground" +
          (mono ? " font-mono text-xs" : "")
        }
      >
        {value}
      </dd>
    </div>
  );
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-10 text-center shadow-soft">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
    </div>
  );
}
