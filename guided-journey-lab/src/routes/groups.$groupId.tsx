import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, BookOpen, Plus, Trash2, UserPlus, Users } from "lucide-react";
import { AppLayout } from "../components/app/AppLayout";
import {
  addGroupMember,
  attachGroupCourse,
  getGroupDetail,
  listCourses,
  removeGroupMember,
} from "../lib/api/client";
import { RequireGroupManager, useAuth } from "../lib/auth/auth-context";
import type { GroupCourseDetail, GroupMemberDetail } from "../lib/api/types";

export const Route = createFileRoute("/groups/$groupId")({
  component: GroupDetailRoute,
  head: () => ({ meta: [{ title: "Group - EduLife" }] }),
});

function GroupDetailRoute() {
  return (
    <RequireGroupManager>
      <GroupDetailPage />
    </RequireGroupManager>
  );
}

function GroupDetailPage() {
  const auth = useAuth();
  const { groupId } = Route.useParams();
  const queryClient = useQueryClient();

  const groupQuery = useQuery({
    queryKey: ["groups", groupId],
    queryFn: () => getGroupDetail(auth.getAccessToken, groupId),
  });

  const group = groupQuery.data;

  return (
    <AppLayout>
      <div className="mx-auto max-w-4xl space-y-8">
        <Link
          to="/groups"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to groups
        </Link>

        {groupQuery.isLoading ? (
          <StateCard title="Loading group..." detail="Fetching members and assigned courses." />
        ) : groupQuery.isError ? (
          <StateCard title="Group unavailable" detail={groupQuery.error.message} />
        ) : !group ? (
          <StateCard title="Group not found" detail="This group does not exist or is not yours." />
        ) : (
          <>
            <h1 className="text-display text-3xl text-foreground">{group.name}</h1>

            <MembersSection
              groupId={groupId}
              members={group.members}
              onChanged={() => queryClient.invalidateQueries({ queryKey: ["groups", groupId] })}
            />

            <CoursesSection
              groupId={groupId}
              courses={group.courses}
              onChanged={() => queryClient.invalidateQueries({ queryKey: ["groups", groupId] })}
            />
          </>
        )}
      </div>
    </AppLayout>
  );
}

function MembersSection({
  groupId,
  members,
  onChanged,
}: {
  groupId: string;
  members: GroupMemberDetail[];
  onChanged: () => void;
}) {
  const auth = useAuth();
  const [email, setEmail] = useState("");

  const addMutation = useMutation({
    mutationFn: () => addGroupMember(auth.getAccessToken, groupId, email.trim()),
    onSuccess: () => {
      setEmail("");
      onChanged();
    },
  });

  const removeMutation = useMutation({
    mutationFn: (userId: string) => removeGroupMember(auth.getAccessToken, groupId, userId),
    onSuccess: onChanged,
  });

  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-soft">
      <div className="flex items-center gap-3">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/10 text-primary">
          <Users className="h-4 w-4" />
        </span>
        <h2 className="text-display text-lg text-foreground">Members ({members.length})</h2>
      </div>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          addMutation.mutate();
        }}
        className="mt-4 flex flex-wrap items-center gap-3"
      >
        <input
          required
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="member@example.com"
          className="h-10 min-w-0 flex-1 rounded-xl border border-input bg-surface px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
        />
        <button
          type="submit"
          disabled={addMutation.isPending}
          className="inline-flex h-10 items-center gap-2 rounded-full bg-primary px-4 text-sm font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90 disabled:opacity-40"
        >
          <UserPlus className="h-4 w-4" />
          {addMutation.isPending ? "Adding..." : "Add by email"}
        </button>
      </form>

      {(addMutation.isError || removeMutation.isError) && (
        <p
          role="alert"
          className="mt-3 rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          {addMutation.error?.message || removeMutation.error?.message}
        </p>
      )}

      <div className="mt-4 space-y-2">
        {members.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            No members yet — add learners or teachers by email.
          </p>
        ) : (
          members.map((member) => (
            <div
              key={member.userId}
              className="flex items-center gap-3 rounded-xl border border-border/60 bg-surface px-4 py-2.5"
            >
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-foreground">{member.email}</p>
                <p className="text-[11px] uppercase tracking-wide text-muted-foreground">
                  {member.role ?? "UNKNOWN"}
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  if (window.confirm(`Remove ${member.email} from this group?`)) {
                    removeMutation.mutate(member.userId);
                  }
                }}
                disabled={removeMutation.isPending}
                className="grid h-8 w-8 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive disabled:opacity-40"
                aria-label={`Remove ${member.email}`}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </div>
          ))
        )}
      </div>
    </section>
  );
}

function CoursesSection({
  groupId,
  courses,
  onChanged,
}: {
  groupId: string;
  courses: GroupCourseDetail[];
  onChanged: () => void;
}) {
  const auth = useAuth();
  const [selectedCourseId, setSelectedCourseId] = useState("");

  const catalogQuery = useQuery({
    queryKey: ["courses", "group-attach"],
    queryFn: () => listCourses(auth.getAccessToken, { size: 50 }),
  });

  const attachedIds = new Set(courses.map((course) => course.courseId));
  const attachable = (catalogQuery.data?.content ?? []).filter(
    (course) => !attachedIds.has(course.id),
  );

  const attachMutation = useMutation({
    mutationFn: () => attachGroupCourse(auth.getAccessToken, groupId, selectedCourseId),
    onSuccess: () => {
      setSelectedCourseId("");
      onChanged();
    },
  });

  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-soft">
      <div className="flex items-center gap-3">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/10 text-primary">
          <BookOpen className="h-4 w-4" />
        </span>
        <h2 className="text-display text-lg text-foreground">
          Assigned courses ({courses.length})
        </h2>
      </div>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          attachMutation.mutate();
        }}
        className="mt-4 flex flex-wrap items-center gap-3"
      >
        <select
          required
          value={selectedCourseId}
          onChange={(event) => setSelectedCourseId(event.target.value)}
          className="h-10 min-w-0 flex-1 rounded-xl border border-input bg-surface px-3 text-sm text-foreground outline-none focus:border-primary transition-all"
        >
          <option value="" disabled>
            {catalogQuery.isLoading
              ? "Loading catalog..."
              : attachable.length === 0
                ? "No more courses to assign"
                : "Pick a course to assign"}
          </option>
          {attachable.map((course) => (
            <option key={course.id} value={course.id}>
              {course.title}
            </option>
          ))}
        </select>
        <button
          type="submit"
          disabled={attachMutation.isPending || !selectedCourseId}
          className="inline-flex h-10 items-center gap-2 rounded-full bg-primary px-4 text-sm font-medium text-primary-foreground shadow-soft transition-all hover:opacity-90 disabled:opacity-40"
        >
          <Plus className="h-4 w-4" />
          {attachMutation.isPending ? "Assigning..." : "Assign"}
        </button>
      </form>

      {(attachMutation.isError || catalogQuery.isError) && (
        <p
          role="alert"
          className="mt-3 rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          {attachMutation.error?.message || catalogQuery.error?.message}
        </p>
      )}

      <div className="mt-4 space-y-2">
        {courses.length === 0 ? (
          <p className="text-xs text-muted-foreground">No courses assigned to this group yet.</p>
        ) : (
          courses.map((course) => (
            <div
              key={course.courseId}
              className="flex items-center gap-3 rounded-xl border border-border/60 bg-surface px-4 py-2.5"
            >
              <BookOpen className="h-4 w-4 text-muted-foreground" />
              <p className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">
                {course.title}
              </p>
              {course.status && (
                <span className="rounded-full bg-secondary px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide text-secondary-foreground">
                  {course.status}
                </span>
              )}
            </div>
          ))
        )}
      </div>
    </section>
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
