export type LessonMediaKind = "youtube" | "vimeo" | "video-file" | "pdf" | "link" | "none";

export interface LessonMedia {
  kind: LessonMediaKind;
  embedUrl?: string;
  rawUrl?: string;
  fileName?: string;
}

const YOUTUBE_HOSTS = new Set(["youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"]);
const VIMEO_HOSTS = new Set(["vimeo.com", "www.vimeo.com", "player.vimeo.com"]);
const VIDEO_FILE_EXT = /\.(mp4|webm|ogg|mov|m4v)(\?.*)?$/i;
const PDF_FILE_EXT = /\.pdf(\?.*)?$/i;

function parseUrl(value: string): URL | null {
  try {
    return new URL(value);
  } catch {
    return null;
  }
}

function getYouTubeId(url: URL): string | null {
  if (url.hostname === "youtu.be") {
    const id = url.pathname.replace(/^\//, "");
    return id || null;
  }

  if (url.pathname === "/watch") {
    return url.searchParams.get("v");
  }

  const embedMatch = url.pathname.match(/^\/embed\/([^/?#]+)/);
  if (embedMatch) return embedMatch[1] ?? null;

  const shortsMatch = url.pathname.match(/^\/shorts\/([^/?#]+)/);
  if (shortsMatch) return shortsMatch[1] ?? null;

  return null;
}

function getVimeoId(url: URL): string | null {
  if (url.hostname === "player.vimeo.com") {
    const match = url.pathname.match(/^\/video\/(\d+)/);
    return match?.[1] ?? null;
  }

  const match = url.pathname.match(/^\/(\d+)/);
  return match?.[1] ?? null;
}

export function resolveLessonMedia(contentUrl: string | null | undefined): LessonMedia {
  if (!contentUrl) return { kind: "none" };

  const parsed = parseUrl(contentUrl);
  if (!parsed) return { kind: "link", rawUrl: contentUrl };

  if (YOUTUBE_HOSTS.has(parsed.hostname)) {
    const id = getYouTubeId(parsed);
    if (id) {
      // youtube-nocookie reduces tracking; rel=0 hides unrelated recommendations after playback.
      return {
        kind: "youtube",
        embedUrl: `https://www.youtube-nocookie.com/embed/${id}?rel=0&modestbranding=1`,
        rawUrl: contentUrl,
      };
    }
  }

  if (VIMEO_HOSTS.has(parsed.hostname)) {
    const id = getVimeoId(parsed);
    if (id) {
      return {
        kind: "vimeo",
        embedUrl: `https://player.vimeo.com/video/${id}?title=0&byline=0`,
        rawUrl: contentUrl,
      };
    }
  }

  if (PDF_FILE_EXT.test(parsed.pathname)) {
    const segments = parsed.pathname.split("/");
    return {
      kind: "pdf",
      embedUrl: contentUrl,
      rawUrl: contentUrl,
      fileName: segments[segments.length - 1] || "document.pdf",
    };
  }

  if (VIDEO_FILE_EXT.test(parsed.pathname)) {
    return { kind: "video-file", rawUrl: contentUrl };
  }

  return { kind: "link", rawUrl: contentUrl };
}

export function isMediaLessonType(lessonType: string) {
  const upper = lessonType?.toUpperCase() ?? "";
  return upper === "VIDEO" || upper === "RESOURCE" || upper === "PDF";
}
