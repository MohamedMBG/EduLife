import { Download, ExternalLink, FileText, Film, Video } from "lucide-react";
import type { LessonDetail } from "../../lib/api/types";
import { resolveLessonMedia } from "../../lib/lesson/media";

interface Props {
  lesson: LessonDetail;
}

export function LessonContentRenderer({ lesson }: Props) {
  const media = resolveLessonMedia(lesson.contentUrl);
  const hasBody = Boolean(lesson.contentBody && lesson.contentBody.trim().length > 0);

  return (
    <div className="space-y-6">
      {media.kind === "youtube" || media.kind === "vimeo" ? (
        <div className="overflow-hidden rounded-2xl border border-border bg-black shadow-elevated">
          <div className="relative aspect-video w-full">
            <iframe
              src={media.embedUrl}
              title={lesson.title}
              className="absolute inset-0 h-full w-full"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
              loading="lazy"
              referrerPolicy="strict-origin-when-cross-origin"
            />
          </div>
        </div>
      ) : null}

      {media.kind === "video-file" && media.rawUrl ? (
        <div className="overflow-hidden rounded-2xl border border-border bg-black shadow-elevated">
          <video src={media.rawUrl} controls preload="metadata" className="aspect-video w-full">
            <track kind="captions" />
          </video>
        </div>
      ) : null}

      {media.kind === "pdf" && media.embedUrl ? (
        <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-soft">
          <div className="flex items-center justify-between border-b border-border bg-surface-elevated px-4 py-3">
            <div className="flex items-center gap-2 text-sm font-medium text-foreground">
              <FileText className="h-4 w-4 text-primary" />
              <span className="truncate">{media.fileName || "PDF document"}</span>
            </div>
            <a
              href={media.rawUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 rounded-full bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground"
            >
              <Download className="h-3.5 w-3.5" />
              Download
            </a>
          </div>
          <iframe
            src={`${media.embedUrl}#toolbar=1&navpanes=0`}
            title={lesson.title}
            className="h-[720px] w-full bg-background"
            loading="lazy"
          />
        </div>
      ) : null}

      {hasBody ? (
        <article className="rounded-3xl border border-border bg-surface-elevated p-6 shadow-soft">
          <div className="flex items-center gap-2">
            <FileText className="h-4 w-4 text-primary" />
            <p className="text-sm font-semibold text-foreground">Lesson notes</p>
          </div>
          <MarkdownLite source={lesson.contentBody!} />
        </article>
      ) : null}

      {media.kind === "link" && media.rawUrl ? (
        <ExternalLinkCard
          url={media.rawUrl}
          icon={
            lesson.lessonType?.toUpperCase() === "VIDEO" ? (
              <Video className="h-5 w-5 text-primary" />
            ) : (
              <Film className="h-5 w-5 text-primary" />
            )
          }
          label="Open external resource"
          hint="This lesson is delivered from an external URL."
        />
      ) : null}

      {media.kind === "none" && !hasBody ? (
        <div className="rounded-2xl border border-border bg-background p-6 text-sm text-muted-foreground">
          No lesson body or media is configured yet for this lesson.
        </div>
      ) : null}
    </div>
  );
}

function ExternalLinkCard({
  url,
  icon,
  label,
  hint,
}: {
  url: string;
  icon: React.ReactNode;
  label: string;
  hint: string;
}) {
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-border bg-surface-elevated p-5 shadow-soft">
      <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-primary/10">
        {icon}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-foreground">{label}</p>
        <p className="mt-1 truncate text-xs text-muted-foreground">{hint}</p>
      </div>
      <a
        href={url}
        target="_blank"
        rel="noreferrer"
        className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground"
      >
        <ExternalLink className="h-3.5 w-3.5" />
        Open
      </a>
    </div>
  );
}

// Tiny markdown subset: paragraphs, headings (#/##/###), bullet lists, **bold**, `code`, links.
// Kept inline to avoid pulling in react-markdown for a few formatting cases.
function MarkdownLite({ source }: { source: string }) {
  const blocks = source.split(/\n{2,}/);

  return (
    <div className="mt-5 space-y-3 text-sm leading-relaxed text-foreground">
      {blocks.map((block, index) => {
        const trimmed = block.trim();
        if (!trimmed) return null;

        if (trimmed.startsWith("### ")) {
          return (
            <h4 key={index} className="text-display text-lg text-foreground">
              {renderInline(trimmed.slice(4))}
            </h4>
          );
        }
        if (trimmed.startsWith("## ")) {
          return (
            <h3 key={index} className="text-display text-xl text-foreground">
              {renderInline(trimmed.slice(3))}
            </h3>
          );
        }
        if (trimmed.startsWith("# ")) {
          return (
            <h2 key={index} className="text-display text-2xl text-foreground">
              {renderInline(trimmed.slice(2))}
            </h2>
          );
        }

        const lines = trimmed.split("\n");
        const isBulletList = lines.every((line) => /^[-*]\s+/.test(line));
        if (isBulletList) {
          return (
            <ul key={index} className="list-disc space-y-1 pl-5">
              {lines.map((line, lineIndex) => (
                <li key={lineIndex}>{renderInline(line.replace(/^[-*]\s+/, ""))}</li>
              ))}
            </ul>
          );
        }

        return (
          <p key={index} className="whitespace-pre-wrap">
            {renderInline(trimmed)}
          </p>
        );
      })}
    </div>
  );
}

function renderInline(input: string): React.ReactNode[] {
  // Tokenize bold (**), inline code (`), and markdown-style links [text](url).
  const tokens: React.ReactNode[] = [];
  const regex = /\*\*(.+?)\*\*|`([^`]+)`|\[([^\]]+)\]\(([^)]+)\)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let key = 0;

  while ((match = regex.exec(input)) !== null) {
    if (match.index > lastIndex) {
      tokens.push(input.slice(lastIndex, match.index));
    }

    if (match[1] !== undefined) {
      tokens.push(
        <strong key={`b-${key++}`} className="font-semibold text-foreground">
          {match[1]}
        </strong>,
      );
    } else if (match[2] !== undefined) {
      tokens.push(
        <code key={`c-${key++}`} className="rounded bg-muted px-1.5 py-0.5 text-xs">
          {match[2]}
        </code>,
      );
    } else if (match[3] !== undefined && match[4] !== undefined) {
      tokens.push(
        <a
          key={`l-${key++}`}
          href={match[4]}
          target="_blank"
          rel="noreferrer"
          className="text-primary underline-offset-4 hover:underline"
        >
          {match[3]}
        </a>,
      );
    }

    lastIndex = regex.lastIndex;
  }

  if (lastIndex < input.length) {
    tokens.push(input.slice(lastIndex));
  }

  return tokens;
}
