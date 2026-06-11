import { useEffect, useRef, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, ImagePlus, UserCircle2 } from "lucide-react";
import { AppShell } from "../components/app/AppShell";
import { ApiClientError, getProfile, updateProfile, uploadAvatar } from "../lib/api/client";
import { RequireAuth, useAuth } from "../lib/auth/auth-context";

const MAX_AVATAR_BYTES = 5 * 1024 * 1024;

export const Route = createFileRoute("/profile")({
  component: ProfileRoute,
  head: () => ({ meta: [{ title: "Profile - EduLife" }] }),
});

function ProfileRoute() {
  return (
    <RequireAuth>
      <ProfilePage />
    </RequireAuth>
  );
}

function ProfilePage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile(auth.getAccessToken),
  });

  const [displayName, setDisplayName] = useState("");
  const [bio, setBio] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [avatarError, setAvatarError] = useState<string | null>(null);

  useEffect(() => {
    if (profileQuery.data) {
      setDisplayName(profileQuery.data.displayName);
      setBio(profileQuery.data.bio ?? "");
    }
  }, [profileQuery.data]);

  const updateMutation = useMutation({
    mutationFn: () =>
      updateProfile(auth.getAccessToken, {
        displayName: displayName.trim(),
        bio: bio.trim(),
      }),
    onSuccess: async () => {
      setFormError(null);
      setSuccess("Profile updated.");
      await queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
    onError: (err) => {
      setSuccess(null);
      setFormError(err instanceof Error ? err.message : "Update failed.");
    },
  });

  const avatarMutation = useMutation({
    mutationFn: (file: File) => uploadAvatar(auth.getAccessToken, file),
    onSuccess: async () => {
      setAvatarError(null);
      setSuccess("Avatar updated.");
      await queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
    onError: (err) => {
      setSuccess(null);
      if (err instanceof ApiClientError) {
        setAvatarError(err.message);
      } else {
        setAvatarError(err instanceof Error ? err.message : "Upload failed.");
      }
    },
  });

  function handleFile(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    if (file.size > MAX_AVATAR_BYTES) {
      setAvatarError("Avatar exceeds the 5MB limit.");
      setSuccess(null);
      return;
    }
    if (!file.type.startsWith("image/")) {
      setAvatarError("File must be an image.");
      setSuccess(null);
      return;
    }

    setAvatarError(null);
    avatarMutation.mutate(file);
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!displayName.trim()) {
      setFormError("Display name is required.");
      return;
    }
    setSuccess(null);
    updateMutation.mutate();
  }

  return (
    <AppShell
      active="dashboard"
      user={{
        displayName: auth.session?.displayName ?? "EduLife learner",
        email: auth.session?.email ?? "",
      }}
      onLogout={auth.logout}
      header={
        <div>
          <p className="text-sm font-semibold text-foreground">Profile</p>
          <p className="text-xs text-muted-foreground">
            Manage your display name, bio, and avatar.
          </p>
        </div>
      }
    >
      {profileQuery.isLoading ? (
        <StateCard title="Loading profile..." detail="Fetching your account details." />
      ) : profileQuery.isError ? (
        <StateCard
          title="Profile unavailable"
          detail={
            profileQuery.error instanceof Error
              ? profileQuery.error.message
              : "Could not load profile."
          }
        />
      ) : !profileQuery.data ? (
        <StateCard title="No profile" detail="Profile data could not be loaded." />
      ) : (
        <div className="space-y-6">
          <section className="flex flex-col gap-6 rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft sm:flex-row sm:items-center">
            <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-full border border-border bg-muted">
              {profileQuery.data.avatarUrl ? (
                <img
                  src={profileQuery.data.avatarUrl}
                  alt={profileQuery.data.displayName}
                  className="h-full w-full object-cover"
                />
              ) : (
                <UserCircle2 className="h-12 w-12 text-muted-foreground" />
              )}
            </div>
            <div className="flex-1">
              <p className="text-sm font-semibold text-foreground">
                {profileQuery.data.displayName}
              </p>
              <p className="text-xs text-muted-foreground">{profileQuery.data.email}</p>
              <p className="mt-3 text-xs text-muted-foreground">
                PNG, JPG, or WebP — max 5MB.
              </p>
              <div className="mt-3">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleFile}
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={avatarMutation.isPending}
                  className="inline-flex items-center gap-2 rounded-full bg-foreground px-4 py-2 text-xs font-semibold text-background disabled:opacity-50"
                >
                  <ImagePlus className="h-3.5 w-3.5" />
                  {avatarMutation.isPending ? "Uploading..." : "Upload new avatar"}
                </button>
              </div>
              {avatarError ? (
                <p className="mt-3 inline-flex items-center gap-2 text-xs text-destructive">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  {avatarError}
                </p>
              ) : null}
            </div>
          </section>

          <form
            onSubmit={handleSubmit}
            className="space-y-5 rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft"
          >
            <div className="space-y-1.5">
              <label
                htmlFor="displayName"
                className="block text-xs uppercase tracking-[0.16em] text-muted-foreground"
              >
                Display name
              </label>
              <input
                id="displayName"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                maxLength={100}
                required
                className="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10"
              />
            </div>

            <div className="space-y-1.5">
              <label
                htmlFor="bio"
                className="block text-xs uppercase tracking-[0.16em] text-muted-foreground"
              >
                Bio
              </label>
              <textarea
                id="bio"
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                maxLength={500}
                rows={4}
                placeholder="Share a short summary about you."
                className="w-full rounded-xl border border-input bg-background px-4 py-3 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10"
              />
              <p className="text-xs text-muted-foreground">{bio.length} / 500</p>
            </div>

            {formError ? (
              <div className="flex items-start gap-2 rounded-2xl border border-destructive/30 bg-destructive/8 p-3 text-sm text-destructive">
                <AlertTriangle className="mt-0.5 h-4 w-4" />
                <p>{formError}</p>
              </div>
            ) : null}

            {success ? (
              <div className="flex items-start gap-2 rounded-2xl border border-primary/20 bg-primary/8 p-3 text-sm text-foreground">
                <CheckCircle2 className="mt-0.5 h-4 w-4 text-primary" />
                <p>{success}</p>
              </div>
            ) : null}

            <button
              type="submit"
              disabled={updateMutation.isPending}
              className="inline-flex items-center gap-2 rounded-full bg-foreground px-5 py-2.5 text-xs font-semibold text-background disabled:opacity-50"
            >
              {updateMutation.isPending ? "Saving..." : "Save changes"}
            </button>
          </form>
        </div>
      )}
    </AppShell>
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
