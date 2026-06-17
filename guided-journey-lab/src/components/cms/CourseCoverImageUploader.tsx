import { useCallback, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ImagePlus, Loader2, Upload, X } from "lucide-react";
import { uploadCourseCoverImage } from "../../lib/api/client";
import { useAuth } from "../../lib/auth/auth-context";

const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_SIZE_BYTES = 5 * 1024 * 1024;
const FALLBACK_GRADIENT = "linear-gradient(135deg, #1e293b, #091426)";

interface CourseCoverImageUploaderProps {
  courseId: string;
  currentImageUrl: string | null;
}

export function CourseCoverImageUploader({
  courseId,
  currentImageUrl,
}: CourseCoverImageUploaderProps) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [imgBroken, setImgBroken] = useState(false);

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadCourseCoverImage(auth.getAccessToken, courseId, file),
    onSuccess: () => {
      setPreview(null);
      setError(null);
      setImgBroken(false);
      queryClient.invalidateQueries({ queryKey: ["cms", "courses"] });
    },
    onError: (err: Error) => {
      setError(err.message);
    },
  });

  const validateAndUpload = useCallback(
    (file: File) => {
      setError(null);

      if (!ACCEPTED_TYPES.includes(file.type)) {
        setError("Please upload a JPG, PNG, or WebP image.");
        return;
      }
      if (file.size > MAX_SIZE_BYTES) {
        setError("Image must be smaller than 5MB.");
        return;
      }

      const url = URL.createObjectURL(file);
      setPreview(url);
      uploadMutation.mutate(file);
    },
    [uploadMutation],
  );

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) validateAndUpload(file);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) validateAndUpload(file);
  };

  const displayUrl = imgBroken ? null : (preview || currentImageUrl);

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-muted-foreground">
        <ImagePlus className="h-3.5 w-3.5" />
        Cover image
      </div>

      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        className={`relative overflow-hidden rounded-2xl border-2 border-dashed transition-all ${
          dragOver ? "border-primary bg-primary/5" : "border-border hover:border-primary/40"
        }`}
      >
        {displayUrl ? (
          <div className="relative aspect-video w-full">
            <img
              src={displayUrl!}
              alt="Course cover"
              className="h-full w-full rounded-2xl object-cover"
              onError={() => setImgBroken(true)}
            />
            {uploadMutation.isPending && (
              <div className="absolute inset-0 flex items-center justify-center rounded-2xl bg-background/60 backdrop-blur-sm">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
              </div>
            )}
          </div>
        ) : (
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="flex w-full flex-col items-center gap-3 px-6 py-10"
          >
            <span className="grid h-12 w-12 place-items-center rounded-2xl bg-primary/10 text-primary">
              <Upload className="h-5 w-5" />
            </span>
            <div className="text-center">
              <p className="text-sm font-medium text-foreground">
                Drop an image here or click to browse
              </p>
              <p className="mt-1 text-xs text-muted-foreground">JPG, PNG, or WebP · Max 5MB</p>
            </div>
          </button>
        )}
      </div>

      {displayUrl && (
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadMutation.isPending}
            className="inline-flex h-9 items-center gap-1.5 rounded-full border border-border px-4 text-xs font-medium text-foreground transition-colors hover:bg-accent disabled:opacity-40"
          >
            <Upload className="h-3 w-3" />
            Change image
          </button>
          {preview && !uploadMutation.isPending && (
            <button
              type="button"
              onClick={() => {
                setPreview(null);
                setError(null);
              }}
              className="inline-flex h-9 items-center gap-1 rounded-full text-xs text-muted-foreground transition-colors hover:text-foreground"
            >
              <X className="h-3 w-3" />
              Cancel
            </button>
          )}
        </div>
      )}

      {error && (
        <p
          role="alert"
          className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          {error}
        </p>
      )}

      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={handleFileChange}
        className="hidden"
      />
    </div>
  );
}
