import { useCallback, useEffect, useState } from "react";

const NOTES_PREFIX = "edulife_lesson_notes:";

function storageKey(lessonId: string) {
  return `${NOTES_PREFIX}${lessonId}`;
}

function readNote(lessonId: string): string {
  if (typeof window === "undefined") return "";
  return window.localStorage.getItem(storageKey(lessonId)) ?? "";
}

export function useLessonNote(lessonId: string) {
  const [note, setNote] = useState<string>(() => readNote(lessonId));
  const [savedAt, setSavedAt] = useState<number | null>(() =>
    typeof window !== "undefined" && window.localStorage.getItem(storageKey(lessonId)) ? Date.now() : null,
  );

  useEffect(() => {
    setNote(readNote(lessonId));
    setSavedAt(
      typeof window !== "undefined" && window.localStorage.getItem(storageKey(lessonId))
        ? Date.now()
        : null,
    );
  }, [lessonId]);

  useEffect(() => {
    // Debounce writes so each keystroke does not hit localStorage. 400ms feels instant but
    // avoids hammering the synchronous storage API on fast typists.
    const handle = window.setTimeout(() => {
      if (note.length === 0) {
        window.localStorage.removeItem(storageKey(lessonId));
        setSavedAt(null);
        return;
      }
      window.localStorage.setItem(storageKey(lessonId), note);
      setSavedAt(Date.now());
    }, 400);

    return () => window.clearTimeout(handle);
  }, [lessonId, note]);

  const clear = useCallback(() => {
    setNote("");
    window.localStorage.removeItem(storageKey(lessonId));
    setSavedAt(null);
  }, [lessonId]);

  return { note, setNote, savedAt, clear };
}
