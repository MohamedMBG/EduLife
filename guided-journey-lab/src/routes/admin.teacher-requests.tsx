import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { CheckCircle2, XCircle, Clock, ChevronDown } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import {
  approveTeacherRequest,
  listAdminTeacherRequests,
  rejectTeacherRequest,
} from "../lib/api/client";
import { useAuth } from "../lib/auth/auth-context";
import type { TeacherRequestStatus, TeacherRequestSummary } from "../lib/api/types";

export const Route = createFileRoute("/admin/teacher-requests")({
  component: AdminTeacherRequestsRoute,
  head: () => ({ meta: [{ title: "Teacher Requests - Admin - EduLife" }] }),
});

const STATUS_TABS: { key: TeacherRequestStatus; label: string }[] = [
  { key: "PENDING", label: "Pending" },
  { key: "APPROVED", label: "Approved" },
  { key: "REJECTED", label: "Rejected" },
];

function AdminTeacherRequestsRoute() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [activeStatus, setActiveStatus] = useState<TeacherRequestStatus>("PENDING");
  const [rejectTarget, setRejectTarget] = useState<string | null>(null);
  const [rejectNote, setRejectNote] = useState("");
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);

  const requestsQuery = useQuery({
    queryKey: ["admin", "teacher-requests", activeStatus],
    queryFn: () => listAdminTeacherRequests(auth.getAccessToken, activeStatus),
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => approveTeacherRequest(auth.getAccessToken, id),
    onSuccess: () => {
      setActionFeedback("Request approved — user promoted to Teacher.");
      queryClient.invalidateQueries({ queryKey: ["admin", "teacher-requests"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "metrics"] });
    },
    onError: (err: Error) => setActionFeedback(`Approval failed: ${err.message}`),
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) =>
      rejectTeacherRequest(auth.getAccessToken, id, note),
    onSuccess: () => {
      setActionFeedback("Request rejected.");
      setRejectTarget(null);
      setRejectNote("");
      queryClient.invalidateQueries({ queryKey: ["admin", "teacher-requests"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "metrics"] });
    },
    onError: (err: Error) => setActionFeedback(`Rejection failed: ${err.message}`),
  });

  const requests = requestsQuery.data?.content ?? [];
  const isPending = (id: string) =>
    approveMutation.isPending || rejectMutation.isPending || id === rejectTarget;

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-display text-2xl text-foreground">Teacher Applications</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Review learners requesting teacher role. Approving promotes the user immediately.
          </p>
        </div>

        {/* Filter tabs */}
        <div className="flex gap-2">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => {
                setActiveStatus(tab.key);
                setActionFeedback(null);
              }}
              className={[
                "rounded-full px-4 py-2 text-sm font-medium transition-colors",
                tab.key === activeStatus
                  ? "bg-primary text-primary-foreground"
                  : "border border-border bg-surface-elevated text-muted-foreground hover:text-foreground",
              ].join(" ")}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Action feedback */}
        {actionFeedback && (
          <div className="rounded-2xl border border-border bg-surface-elevated px-4 py-3 text-sm text-foreground shadow-soft">
            {actionFeedback}
          </div>
        )}

        {/* Content */}
        {requestsQuery.isLoading ? (
          <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-12 text-center shadow-soft">
            <p className="text-sm text-muted-foreground">Loading requests…</p>
          </div>
        ) : requestsQuery.isError ? (
          <div className="rounded-3xl border border-destructive/20 bg-destructive/5 px-6 py-8 text-center shadow-soft">
            <p className="text-sm font-semibold text-foreground">Could not load requests</p>
            <p className="mt-1 text-sm text-muted-foreground">{requestsQuery.error.message}</p>
            <button
              type="button"
              onClick={() => requestsQuery.refetch()}
              className="mt-4 rounded-full bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
            >
              Retry
            </button>
          </div>
        ) : requests.length === 0 ? (
          <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-12 text-center shadow-soft">
            <p className="text-sm font-semibold text-foreground">
              No {activeStatus.toLowerCase()} requests
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              Nothing to review in this category right now.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {requests.map((req) => (
              <RequestCard
                key={req.id}
                req={req}
                isExpanded={rejectTarget === req.id}
                rejectNote={rejectTarget === req.id ? rejectNote : ""}
                onRejectNoteChange={setRejectNote}
                onApprove={() => {
                  if (!isPending(req.id)) approveMutation.mutate(req.id);
                }}
                onRejectOpen={() => {
                  setRejectTarget(req.id);
                  setRejectNote("");
                }}
                onRejectCancel={() => setRejectTarget(null)}
                onRejectConfirm={() => {
                  rejectMutation.mutate({ id: req.id, note: rejectNote.trim() || undefined });
                }}
                disabled={isPending(req.id)}
              />
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  );
}

interface RequestCardProps {
  req: TeacherRequestSummary;
  isExpanded: boolean;
  rejectNote: string;
  onRejectNoteChange: (v: string) => void;
  onApprove: () => void;
  onRejectOpen: () => void;
  onRejectCancel: () => void;
  onRejectConfirm: () => void;
  disabled: boolean;
}

function RequestCard({
  req,
  isExpanded,
  rejectNote,
  onRejectNoteChange,
  onApprove,
  onRejectOpen,
  onRejectCancel,
  onRejectConfirm,
  disabled,
}: RequestCardProps) {
  const dateStr = req.requestedAt?.substring(0, 10) ?? "";

  return (
    <article className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft">
      {/* Top row */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={req.status} />
          <span className="text-xs text-muted-foreground">{dateStr}</span>
        </div>
      </div>

      {/* Email */}
      <p className="mt-3 text-base font-semibold text-foreground">{req.userEmail}</p>

      {/* Motivation */}
      {req.motivation && (
        <p className="mt-2 text-sm text-muted-foreground line-clamp-3">{req.motivation}</p>
      )}

      {/* Admin note (reviewed only) */}
      {req.adminNote && (
        <div className="mt-3 rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3">
          <p className="text-xs font-bold uppercase tracking-wide text-destructive">Admin note</p>
          <p className="mt-1 text-sm text-foreground">{req.adminNote}</p>
        </div>
      )}

      {/* Actions (pending only) */}
      {req.status === "PENDING" && (
        <div className="mt-5">
          {!isExpanded ? (
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={onApprove}
                disabled={disabled}
                className="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
              >
                <CheckCircle2 className="h-4 w-4" />
                Approve
              </button>
              <button
                type="button"
                onClick={onRejectOpen}
                disabled={disabled}
                className="inline-flex items-center gap-2 rounded-full border border-destructive/30 bg-destructive/5 px-4 py-2 text-sm font-semibold text-destructive disabled:opacity-60"
              >
                <XCircle className="h-4 w-4" />
                Reject
              </button>
            </div>
          ) : (
            <div className="space-y-3 rounded-2xl border border-border bg-muted/40 p-4">
              <p className="text-sm font-medium text-foreground">
                Leave an optional note (visible in admin log only):
              </p>
              <textarea
                value={rejectNote}
                onChange={(e) => onRejectNoteChange(e.target.value)}
                maxLength={500}
                rows={3}
                placeholder="Admin note (optional, max 500 chars)"
                className="w-full rounded-xl border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring resize-none"
              />
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={onRejectConfirm}
                  disabled={disabled}
                  className="inline-flex items-center gap-2 rounded-full bg-destructive px-4 py-2 text-sm font-semibold text-destructive-foreground disabled:opacity-60"
                >
                  <XCircle className="h-4 w-4" />
                  Confirm reject
                </button>
                <button
                  type="button"
                  onClick={onRejectCancel}
                  className="rounded-full border border-border bg-surface-elevated px-4 py-2 text-sm text-muted-foreground hover:text-foreground"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </article>
  );
}

function StatusBadge({ status }: { status: TeacherRequestStatus }) {
  if (status === "APPROVED") {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-bold uppercase tracking-wide text-primary">
        <CheckCircle2 className="h-3 w-3" />
        Approved
      </span>
    );
  }
  if (status === "REJECTED") {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-destructive/10 px-2.5 py-1 text-xs font-bold uppercase tracking-wide text-destructive">
        <XCircle className="h-3 w-3" />
        Rejected
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-accent/55 px-2.5 py-1 text-xs font-bold uppercase tracking-wide text-primary">
      <Clock className="h-3 w-3" />
      Pending
    </span>
  );
}
