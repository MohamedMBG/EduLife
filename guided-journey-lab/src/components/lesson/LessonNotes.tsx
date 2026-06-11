import { Notebook, Trash2 } from "lucide-react";
import { useLessonNote } from "../../lib/lesson/notes";

interface Props {
  lessonId: string;
}

export function LessonNotes({ lessonId }: Props) {
  const { note, setNote, savedAt, clear } = useLessonNote(lessonId);

  const savedLabel = savedAt
    ? `Saved ${new Date(savedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`
    : "Notes stay in this browser only.";

  return (
    <div className="rounded-3xl border border-border bg-surface-elevated p-5 shadow-soft">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Notebook className="h-4 w-4 text-primary" />
          <p className="text-sm font-semibold text-foreground">My notes</p>
        </div>
        {note.length > 0 ? (
          <button
            type="button"
            onClick={clear}
            className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-destructive transition-colors"
            aria-label="Clear notes"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Clear
          </button>
        ) : null}
      </div>

      <textarea
        value={note}
        onChange={(event) => setNote(event.target.value)}
        rows={6}
        placeholder="Jot down key ideas, timestamps, or questions to revisit..."
        className="mt-3 w-full resize-y rounded-2xl border border-border bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground/60 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all"
      />

      <p className="mt-2 text-[11px] text-muted-foreground" aria-live="polite">
        {savedLabel}
      </p>
    </div>
  );
}
