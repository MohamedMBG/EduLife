import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookOpen, Plus, Users, X } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import { createGroup, listMyGroups } from "../lib/api/client";
import { RequireGroupManager, useAuth } from "../lib/auth/auth-context";
import type { GroupSummary } from "../lib/api/types";

export const Route = createFileRoute("/groups/")({
  component: GroupsRoute,
  head: () => ({ meta: [{ title: "Groups - EduLife" }] }),
});

function GroupsRoute() {
  return (
    <RequireGroupManager>
      <GroupsPage />
    </RequireGroupManager>
  );
}

function GroupsPage() {
  const auth = useAuth();
  const [creating, setCreating] = useState(false);

  const groupsQuery = useQuery({
    queryKey: ["groups"],
    queryFn: () => listMyGroups(auth.getAccessToken),
  });

  const groups = groupsQuery.data ?? [];

  return (
    <AppLayout>
      <div className="mx-auto max-w-5xl space-y-8">
        <section className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-display text-3xl text-foreground">Your groups</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Each group bundles learners with the courses assigned to them.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setCreating((open) => !open)}
            className="inline-flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-elevated transition-all hover:opacity-90 active:scale-[0.98]"
          >
            {creating ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
            {creating ? "Close" : "New group"}
          </button>
        </section>

        {creating && <CreateGroupCard onDone={() => setCreating(false)} />}

        {groupsQuery.isLoading ? (
          <StateCard
            title="Loading your groups..."
            detail="Fetching your cohorts from the backend."
          />
        ) : groupsQuery.isError ? (
          <StateCard title="Groups unavailable" detail={groupsQuery.error.message} />
        ) : groups.length === 0 ? (
          <StateCard
            title="No groups yet"
            detail="Create your first group, then add members and assign courses."
          />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {groups.map((group) => (
              <GroupCard key={group.id} group={group} />
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  );
}

function GroupCard({ group }: { group: GroupSummary }) {
  return (
    <Link
      to="/groups/$groupId"
      params={{ groupId: group.id }}
      className="group rounded-3xl border border-border bg-card p-6 shadow-soft transition-all hover:border-primary/40 hover:shadow-elevated"
    >
      <span className="grid h-10 w-10 place-items-center rounded-xl bg-primary/10 text-primary">
        <Users className="h-5 w-5" />
      </span>
      <h2 className="mt-4 text-display text-xl text-foreground group-hover:text-primary transition-colors">
        {group.name}
      </h2>
      <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-1.5">
          <Users className="h-3.5 w-3.5" />
          {group.memberCount} member{group.memberCount === 1 ? "" : "s"}
        </span>
        <span className="inline-flex items-center gap-1.5">
          <BookOpen className="h-3.5 w-3.5" />
          {group.courseCount} course{group.courseCount === 1 ? "" : "s"}
        </span>
      </div>
    </Link>
  );
}

function CreateGroupCard({ onDone }: { onDone: () => void }) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [name, setName] = useState("");

  const createMutation = useMutation({
    mutationFn: () => createGroup(auth.getAccessToken, name.trim()),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["groups"] });
      onDone();
    },
  });

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        createMutation.mutate();
      }}
      className="space-y-4 rounded-3xl border border-border bg-surface-elevated p-6 shadow-elevated"
    >
      <h2 className="text-display text-xl text-foreground">New group</h2>
      <input
        required
        maxLength={255}
        value={name}
        onChange={(event) => setName(event.target.value)}
        placeholder="e.g. Bac SM 2026 — Morning cohort"
        className="w-full h-11 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
      />
      {createMutation.isError && (
        <p
          role="alert"
          className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          {createMutation.error.message}
        </p>
      )}
      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={createMutation.isPending}
          className="inline-flex h-11 items-center rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90 disabled:opacity-40"
        >
          {createMutation.isPending ? "Creating..." : "Create group"}
        </button>
        <button
          type="button"
          onClick={onDone}
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}

function StateCard({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-3xl border border-border bg-surface-elevated px-6 py-10 text-center shadow-soft">
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="mt-2 text-xs text-muted-foreground">{detail}</p>
    </div>
  );
}
