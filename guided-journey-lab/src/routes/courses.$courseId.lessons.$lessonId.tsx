import { createFileRoute, Link } from "@tanstack/react-router";
import { motion, AnimatePresence } from "framer-motion";
import { useState, useRef, useEffect, useCallback } from "react";
import {
  ArrowLeft, ArrowRight, CheckCircle, PlayCircle, Clock,
  Zap, ChevronLeft, ChevronRight, FileQuestion,
  Volume2, VolumeX, Maximize, Minimize, SkipBack, SkipForward,
  FileText, BookOpen, PenLine, HelpCircle,
  ChevronUp, ChevronDown, ZoomIn, ZoomOut,
  Upload, Paperclip, Send, RotateCcw, XCircle,
  Pause, Play,
} from "lucide-react";

export const Route = createFileRoute("/courses/$courseId/lessons/$lessonId")({
  component: LessonPlayerPage,
  head: () => ({ meta: [{ title: "Lesson — EduLife" }] }),
});

// ─── Types ────────────────────────────────────────────────────────────────────

type ContentTab = "video" | "pdf" | "quiz" | "homework";

interface QuizQuestion {
  id: string;
  text: string;
  options: { id: string; text: string }[];
  correct: string;
  explanation: string;
}

interface LessonData {
  id: string;
  title: string;
  description: string;
  duration: string;
  content: string;
  hasPdf: boolean;
  hasQuiz: boolean;
  hasHomework: boolean;
  pdfTitle: string;
  pdfPages: { heading: string; body: string }[];
  quiz: QuizQuestion[];
  homework: {
    title: string;
    description: string;
    deadline: string;
    points: number;
    instructions: string[];
  };
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const LESSON_DATA: Record<string, LessonData> = {
  "1": {
    id: "1", title: "Introduction to HTML", duration: "12m",
    description: "Learn the building blocks of every webpage. We cover document structure, the DOCTYPE declaration, and the essential head and body sections.",
    hasPdf: true, hasQuiz: true, hasHomework: true,
    content: `HTML (HyperText Markup Language) is the standard markup language for creating web pages.

## What you'll learn
- The anatomy of an HTML document
- How browsers parse and render HTML
- Writing valid, semantic markup
- Nesting elements correctly

## Key concepts

Every HTML document starts with a \`<!DOCTYPE html>\` declaration. This tells the browser which version of HTML the document uses. The \`<html>\` element is the root, followed by \`<head>\` (metadata) and \`<body>\` (content).`,
    pdfTitle: "HTML Fundamentals — Lesson Slides",
    pdfPages: [
      {
        heading: "What is HTML?",
        body: "HTML stands for HyperText Markup Language. It is the standard language for creating web pages and web applications. HTML describes the structure of a webpage using markup elements called tags.\n\nHTML was first created by Tim Berners-Lee in 1991 and has been evolving ever since. The current version is HTML5.",
      },
      {
        heading: "Document Structure",
        body: "Every valid HTML document follows a standard structure:\n\n• <!DOCTYPE html> — declares the document type\n• <html> — the root element\n• <head> — metadata container (title, styles, scripts)\n• <body> — visible page content\n\nThis structure is mandatory for browsers to render pages correctly.",
      },
      {
        heading: "Common HTML Elements",
        body: "Headings: <h1> through <h6> define headings of decreasing importance.\n\nParagraphs: <p> wraps blocks of text.\n\nLinks: <a href='url'>text</a> creates hyperlinks.\n\nImages: <img src='url' alt='description'> embeds images.\n\nLists: <ul> (unordered) and <ol> (ordered) with <li> items.",
      },
      {
        heading: "Semantic HTML5",
        body: "HTML5 introduced semantic elements that describe meaning:\n\n• <header> — introductory content\n• <nav> — navigation links\n• <main> — main content area\n• <article> — self-contained content\n• <section> — thematic grouping\n• <footer> — footer content\n\nSemantic HTML improves accessibility and SEO.",
      },
      {
        heading: "Best Practices",
        body: "Always use lowercase for tags and attributes.\n\nAlways quote attribute values.\n\nUse semantic elements over generic divs when possible.\n\nInclude alt text on all images.\n\nValidate your HTML at validator.w3.org.\n\nIndent nested elements consistently for readability.",
      },
    ],
    quiz: [
      {
        id: "q1",
        text: "What does HTML stand for?",
        options: [
          { id: "a", text: "Hyper Text Markup Language" },
          { id: "b", text: "High Transfer Markup Language" },
          { id: "c", text: "Home Tool Markup Language" },
          { id: "d", text: "Hyperlink and Text Markup Language" },
        ],
        correct: "a",
        explanation: "HTML stands for HyperText Markup Language — the standard language for structuring web pages.",
      },
      {
        id: "q2",
        text: "Which tag defines the largest heading in HTML?",
        options: [
          { id: "a", text: "<h6>" },
          { id: "b", text: "<header>" },
          { id: "c", text: "<h1>" },
          { id: "d", text: "<heading>" },
        ],
        correct: "c",
        explanation: "<h1> is the highest-level heading. Headings go from <h1> (largest/most important) to <h6> (smallest).",
      },
      {
        id: "q3",
        text: "Which declaration is required at the start of every HTML5 document?",
        options: [
          { id: "a", text: "<html5>" },
          { id: "b", text: "<!DOCTYPE html>" },
          { id: "c", text: "<meta charset='utf-8'>" },
          { id: "d", text: "<!-- HTML5 -->" },
        ],
        correct: "b",
        explanation: "<!DOCTYPE html> tells the browser this is an HTML5 document. Without it, browsers may enter quirks mode.",
      },
    ],
    homework: {
      title: "Build your first HTML page",
      deadline: "2026-06-03",
      points: 100,
      description: "Create a well-structured personal profile page using HTML5 elements.",
      instructions: [
        "Create an index.html file with valid HTML5 boilerplate",
        "Add a <header> with your name as an <h1> heading",
        "Write a short <main> section with at least two <section> elements",
        "Include a <nav> with at least 3 internal anchor links",
        "Add a <footer> with the current year",
        "Use semantic elements throughout — avoid unnecessary <div>s",
        "Validate at validator.w3.org before submitting",
      ],
    },
  },
  "7": {
    id: "7", title: "CSS Flexbox & Grid", duration: "18m",
    description: "Master the two most powerful CSS layout tools. Build responsive layouts with Flexbox for one-dimensional flows and Grid for two-dimensional designs.",
    hasPdf: true, hasQuiz: true, hasHomework: true,
    content: `Flexbox and Grid are the backbone of modern CSS layout.

## Flexbox

One-dimensional layout model. Key properties:
- \`display: flex\` — activates Flexbox
- \`justify-content\` — main axis alignment
- \`align-items\` — cross axis alignment
- \`flex-wrap\` — whether items wrap

## CSS Grid

Two-dimensional system for rows AND columns:
- \`display: grid\` — activates Grid
- \`grid-template-columns\` — column tracks
- \`grid-template-rows\` — row tracks
- \`gap\` — gutters between cells`,
    pdfTitle: "Flexbox & Grid — Reference Guide",
    pdfPages: [
      {
        heading: "Why Flexbox?",
        body: "Before Flexbox, CSS layout relied on floats, inline-blocks, and table hacks. Flexbox provides a more efficient way to lay out, align, and distribute space among items in a container.\n\nActivate it with display: flex on the parent. All direct children become flex items.",
      },
      {
        heading: "Flexbox Axes",
        body: "Flexbox works along two axes:\n\n• Main axis — defined by flex-direction (default: row, left-to-right)\n• Cross axis — perpendicular to the main axis\n\njustify-content aligns items on the main axis.\nalign-items aligns items on the cross axis.\nalign-self overrides alignment for a single item.",
      },
      {
        heading: "CSS Grid Basics",
        body: "CSS Grid is a two-dimensional layout system. Unlike Flexbox, it handles both rows and columns simultaneously.\n\nDefine a grid:\ndisplay: grid;\ngrid-template-columns: 1fr 1fr 1fr;\ngrid-template-rows: auto;\ngap: 1rem;\n\nThis creates a 3-column grid with equal widths.",
      },
      {
        heading: "Grid Template Areas",
        body: "Grid allows you to name areas of the layout:\n\ngrid-template-areas:\n  'header header'\n  'sidebar content'\n  'footer footer';\n\nThen assign elements:\nheader { grid-area: header; }\n.sidebar { grid-area: sidebar; }\n\nThis creates extremely readable, maintainable layouts.",
      },
      {
        heading: "When to Use Which",
        body: "Use Flexbox when:\n• You need to distribute items in a single row or column\n• Content size should determine the layout\n• You're building nav bars, button groups, card rows\n\nUse Grid when:\n• You need a two-dimensional layout\n• You're building page-level structure\n• Items need to align across both rows and columns",
      },
    ],
    quiz: [
      {
        id: "q1",
        text: "Which CSS property aligns flex items along the main axis?",
        options: [
          { id: "a", text: "align-items" },
          { id: "b", text: "justify-content" },
          { id: "c", text: "align-content" },
          { id: "d", text: "flex-align" },
        ],
        correct: "b",
        explanation: "justify-content controls alignment on the main axis. align-items controls the cross axis.",
      },
      {
        id: "q2",
        text: "What CSS value creates a 3-column grid with equal widths?",
        options: [
          { id: "a", text: "grid-template-columns: 33% 33% 33%" },
          { id: "b", text: "grid-columns: 3" },
          { id: "c", text: "grid-template-columns: 1fr 1fr 1fr" },
          { id: "d", text: "columns: 3" },
        ],
        correct: "c",
        explanation: "The fr unit represents a fraction of the available space. 1fr 1fr 1fr divides the row into 3 equal columns.",
      },
      {
        id: "q3",
        text: "Flexbox is best suited for which type of layout?",
        options: [
          { id: "a", text: "Two-dimensional page structure" },
          { id: "b", text: "Full-page grid with named areas" },
          { id: "c", text: "One-dimensional row or column layouts" },
          { id: "d", text: "Fixed-position overlays" },
        ],
        correct: "c",
        explanation: "Flexbox shines for one-dimensional layouts — distributing items along a single row or column.",
      },
    ],
    homework: {
      title: "Build a responsive layout",
      deadline: "2026-06-05",
      points: 120,
      description: "Create a responsive page layout that uses both Flexbox and CSS Grid.",
      instructions: [
        "Build a page with a header, sidebar, main content, and footer",
        "Use CSS Grid for the overall page structure",
        "Use Flexbox for the header navigation links",
        "Use Flexbox for a 'featured cards' row in main content",
        "Make the layout responsive: stack columns on mobile",
        "Use CSS custom properties (variables) for colors and spacing",
        "Submit both HTML and CSS files",
      ],
    },
  },
};

const COURSE_LESSONS = [
  { id: "1", title: "Introduction to HTML" },
  { id: "2", title: "HTML Elements & Attributes" },
  { id: "3", title: "Semantic HTML5" },
  { id: "4", title: "CSS Fundamentals" },
  { id: "5", title: "Selectors & Specificity" },
  { id: "6", title: "Box Model Deep Dive" },
  { id: "7", title: "CSS Flexbox & Grid" },
  { id: "8", title: "Responsive Design" },
];

// ─── Video Player ─────────────────────────────────────────────────────────────

function VideoPlayer({ duration }: { duration: string }) {
  const [playing,    setPlaying]    = useState(false);
  const [progress,   setProgress]   = useState(0);
  const [volume,     setVolume]     = useState(80);
  const [muted,      setMuted]      = useState(false);
  const [speed,      setSpeed]      = useState(1);
  const [fullscreen, setFullscreen] = useState(false);
  const [showSpeed,  setShowSpeed]  = useState(false);
  const [buffered,   setBuffered]   = useState(38);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2];

  const totalSecs = (() => {
    const [m, s] = duration.replace("m", "").split(":").length > 1
      ? duration.replace("m", "").split(":").map(Number)
      : [parseInt(duration), 0];
    return m * 60 + s;
  })();

  const elapsed = Math.round((progress / 100) * totalSecs);
  const fmt = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

  useEffect(() => {
    if (playing) {
      timerRef.current = setInterval(() => {
        setProgress(p => {
          if (p >= 100) { setPlaying(false); return 100; }
          return Math.min(100, p + (100 / totalSecs) * speed * 0.5);
        });
        setBuffered(b => Math.min(100, b + 0.3));
      }, 500);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [playing, speed, totalSecs]);

  function seek(e: React.MouseEvent<HTMLDivElement>) {
    const rect = e.currentTarget.getBoundingClientRect();
    const pct  = ((e.clientX - rect.left) / rect.width) * 100;
    setProgress(Math.max(0, Math.min(100, pct)));
  }

  function skip(secs: number) {
    const delta = (secs / totalSecs) * 100;
    setProgress(p => Math.max(0, Math.min(100, p + delta)));
  }

  return (
    <div
      ref={containerRef}
      className={`relative bg-black group select-none ${fullscreen ? "fixed inset-0 z-50" : "w-full"}`}
      style={fullscreen ? {} : { aspectRatio: "16/9", maxHeight: "480px" }}
    >
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-br from-[oklch(0.12_0.05_145)] to-[oklch(0.06_0.04_160)]" />
      <div className="absolute inset-0 opacity-15" style={{ backgroundImage: "radial-gradient(ellipse at 25% 25%, oklch(0.45 0.18 145), transparent 55%)" }} />

      {/* Simulated video content — chapter title */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div className="text-center opacity-0 group-hover:opacity-0">
          {/* intentionally blank — looks like paused frame */}
        </div>
      </div>

      {/* Big play/pause hit area */}
      <button
        onClick={() => setPlaying(v => !v)}
        className="absolute inset-0 w-full h-full flex items-center justify-center z-10"
        aria-label={playing ? "Pause" : "Play"}
      >
        <AnimatePresence>
          {!playing && (
            <motion.div
              key="play"
              initial={{ scale: 0.7, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 1.3, opacity: 0 }}
              transition={{ duration: 0.18 }}
              className="grid h-16 w-16 place-items-center rounded-full border-2 border-white/30 bg-black/40 backdrop-blur-sm"
            >
              <Play className="h-7 w-7 text-white ml-0.5" fill="white" strokeWidth={0} />
            </motion.div>
          )}
        </AnimatePresence>
      </button>

      {/* Controls overlay — always show on mobile, fade in on desktop hover */}
      <div className="absolute bottom-0 inset-x-0 z-20 bg-gradient-to-t from-black/80 via-black/30 to-transparent pt-8 pb-3 px-4 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity duration-200">

        {/* Progress bar */}
        <div
          className="relative h-4 flex items-center cursor-pointer mb-2 group/seek"
          onClick={seek}
        >
          <div className="w-full h-1 rounded-full bg-white/20 overflow-hidden group-hover/seek:h-1.5 transition-all">
            {/* Buffered */}
            <div className="absolute top-0 h-full rounded-full bg-white/25 transition-all" style={{ width: `${buffered}%` }} />
            {/* Played */}
            <div className="absolute top-0 h-full rounded-full bg-primary transition-none" style={{ width: `${progress}%` }} />
          </div>
          {/* Thumb */}
          <div
            className="absolute top-1/2 -translate-y-1/2 h-3.5 w-3.5 rounded-full bg-white shadow-md opacity-0 group-hover/seek:opacity-100 transition-opacity"
            style={{ left: `calc(${progress}% - 7px)` }}
          />
        </div>

        {/* Controls row */}
        <div className="flex items-center gap-3">
          {/* Skip back */}
          <button onClick={() => skip(-10)} className="text-white/70 hover:text-white transition-colors" title="Back 10s">
            <SkipBack className="h-4 w-4" strokeWidth={1.75} />
          </button>

          {/* Play/Pause */}
          <button
            onClick={() => setPlaying(v => !v)}
            className="grid h-8 w-8 place-items-center rounded-full bg-white text-black hover:scale-105 active:scale-95 transition-transform"
          >
            {playing
              ? <Pause className="h-3.5 w-3.5" fill="black" strokeWidth={0} />
              : <Play  className="h-3.5 w-3.5 ml-0.5" fill="black" strokeWidth={0} />
            }
          </button>

          {/* Skip forward */}
          <button onClick={() => skip(10)} className="text-white/70 hover:text-white transition-colors" title="Forward 10s">
            <SkipForward className="h-4 w-4" strokeWidth={1.75} />
          </button>

          {/* Time */}
          <span className="text-xs text-white/70 tabular-nums ml-1">
            {fmt(elapsed)} / {duration}
          </span>

          <div className="flex-1" />

          {/* Volume */}
          <div className="flex items-center gap-1.5 group/vol">
            <button onClick={() => setMuted(v => !v)} className="text-white/70 hover:text-white transition-colors">
              {muted || volume === 0
                ? <VolumeX className="h-4 w-4" strokeWidth={1.75} />
                : <Volume2 className="h-4 w-4" strokeWidth={1.75} />
              }
            </button>
            <div className="hidden group-hover/vol:flex items-center w-20">
              <input
                type="range" min={0} max={100} value={muted ? 0 : volume}
                onChange={e => { setVolume(Number(e.target.value)); setMuted(false); }}
                className="w-full h-1 accent-white cursor-pointer"
              />
            </div>
          </div>

          {/* Speed */}
          <div className="relative">
            <button
              onClick={() => setShowSpeed(v => !v)}
              className="h-6 px-2 rounded-md border border-white/20 text-[11px] font-semibold text-white/70 hover:text-white hover:border-white/40 transition-all"
            >
              {speed === 1 ? "1×" : `${speed}×`}
            </button>
            <AnimatePresence>
              {showSpeed && (
                <motion.div
                  initial={{ opacity: 0, y: 4, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 4, scale: 0.95 }}
                  transition={{ duration: 0.12 }}
                  className="absolute bottom-full right-0 mb-1.5 rounded-xl border border-white/15 bg-black/90 backdrop-blur-md overflow-hidden min-w-[72px]"
                >
                  {SPEEDS.map(s => (
                    <button
                      key={s}
                      onClick={() => { setSpeed(s); setShowSpeed(false); }}
                      className={`w-full px-3 py-1.5 text-xs text-left transition-colors ${
                        speed === s ? "bg-white/15 text-white font-semibold" : "text-white/60 hover:bg-white/8 hover:text-white"
                      }`}
                    >
                      {s === 1 ? "Normal" : `${s}×`}
                    </button>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Fullscreen */}
          <button
            onClick={() => setFullscreen(v => !v)}
            className="text-white/70 hover:text-white transition-colors"
          >
            {fullscreen
              ? <Minimize className="h-4 w-4" strokeWidth={1.75} />
              : <Maximize className="h-4 w-4" strokeWidth={1.75} />
            }
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── PDF Reader ───────────────────────────────────────────────────────────────

function PdfReader({ pdfTitle, pages }: { pdfTitle: string; pages: LessonData["pdfPages"] }) {
  const [page,  setPage]  = useState(0);
  const [zoom,  setZoom]  = useState(100);

  const prev = () => setPage(p => Math.max(0, p - 1));
  const next = () => setPage(p => Math.min(pages.length - 1, p + 1));

  const current = pages[page];

  return (
    <div className="flex flex-col h-full min-h-[600px]">
      {/* Toolbar */}
      <div className="flex items-center gap-3 px-4 py-2.5 border-b border-border/60 bg-surface-elevated/80 backdrop-blur-sm sticky top-0 z-10">
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <FileText className="h-3.5 w-3.5 shrink-0" />
          <span className="font-medium text-foreground truncate max-w-[200px]">{pdfTitle}</span>
        </div>
        <div className="flex-1" />

        {/* Zoom */}
        <div className="flex items-center gap-1 border border-border/70 rounded-xl overflow-hidden">
          <button
            onClick={() => setZoom(z => Math.max(70, z - 10))}
            className="grid h-7 w-7 place-items-center text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-all"
          >
            <ZoomOut className="h-3.5 w-3.5" />
          </button>
          <span className="px-2 text-xs font-medium text-foreground tabular-nums w-12 text-center">{zoom}%</span>
          <button
            onClick={() => setZoom(z => Math.min(150, z + 10))}
            className="grid h-7 w-7 place-items-center text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-all"
          >
            <ZoomIn className="h-3.5 w-3.5" />
          </button>
        </div>

        {/* Page navigation */}
        <div className="flex items-center gap-1 border border-border/70 rounded-xl overflow-hidden">
          <button
            onClick={prev} disabled={page === 0}
            className="grid h-7 w-7 place-items-center text-muted-foreground hover:text-foreground hover:bg-accent/60 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
          >
            <ChevronUp className="h-3.5 w-3.5" />
          </button>
          <span className="px-2 text-xs text-muted-foreground tabular-nums">
            {page + 1} / {pages.length}
          </span>
          <button
            onClick={next} disabled={page === pages.length - 1}
            className="grid h-7 w-7 place-items-center text-muted-foreground hover:text-foreground hover:bg-accent/60 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
          >
            <ChevronDown className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {/* Page area */}
      <div className="flex-1 overflow-y-auto bg-[oklch(0.94_0.006_100)] dark:bg-[oklch(0.16_0.008_100)] p-6 flex items-start justify-center">
        <AnimatePresence mode="wait">
          <motion.div
            key={page}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2 }}
            className="w-full bg-white dark:bg-[oklch(0.20_0.008_100)] rounded-lg shadow-[0_4px_24px_rgba(0,0,0,0.12)] overflow-hidden"
            style={{
              maxWidth: `${Math.round(620 * (zoom / 100))}px`,
              minHeight: "740px",
            }}
          >
            {/* PDF page header bar */}
            <div className="h-2 bg-gradient-to-r from-primary to-primary-glow" />

            {/* Simulated PDF content */}
            <div className="px-12 py-10 space-y-5">
              {/* Lesson title watermark */}
              <div className="flex items-center justify-between border-b border-gray-100 dark:border-white/8 pb-4 mb-6">
                <span className="text-[11px] font-semibold uppercase tracking-widest text-gray-300 dark:text-white/20">EduLife</span>
                <span className="text-[11px] text-gray-300 dark:text-white/20 tabular-nums">Page {page + 1} of {pages.length}</span>
              </div>

              <h2 className="text-2xl font-bold text-gray-800 dark:text-white/90 leading-tight" style={{ fontFamily: "Georgia, serif" }}>
                {current.heading}
              </h2>

              <div className="space-y-4">
                {current.body.split("\n\n").map((para, i) => {
                  if (para.startsWith("•")) {
                    return (
                      <ul key={i} className="space-y-2">
                        {para.split("\n").filter(l => l.trim()).map((line, j) => (
                          <li key={j} className="flex items-start gap-2.5 text-sm text-gray-600 dark:text-white/65 leading-relaxed" style={{ fontFamily: "Georgia, serif" }}>
                            <span className="mt-2 h-1.5 w-1.5 rounded-full bg-primary/60 shrink-0" />
                            {line.replace(/^•\s*/, "")}
                          </li>
                        ))}
                      </ul>
                    );
                  }
                  if (para.includes("display: flex") || para.includes("display: grid") || para.includes("<!DOCTYPE")) {
                    return (
                      <pre key={i} className="rounded-lg bg-gray-50 dark:bg-white/5 border border-gray-100 dark:border-white/8 px-4 py-3 text-[12px] font-mono text-gray-700 dark:text-white/75 whitespace-pre-wrap leading-relaxed overflow-x-auto">
                        {para}
                      </pre>
                    );
                  }
                  return (
                    <p key={i} className="text-sm text-gray-600 dark:text-white/65 leading-relaxed" style={{ fontFamily: "Georgia, serif" }}>
                      {para}
                    </p>
                  );
                })}
              </div>

              {/* Page number footer */}
              <div className="pt-10 mt-auto border-t border-gray-100 dark:border-white/8 flex items-center justify-between">
                <span className="text-[11px] text-gray-300 dark:text-white/20">{pdfTitle}</span>
                <span className="text-[11px] text-gray-300 dark:text-white/20 tabular-nums">{page + 1}</span>
              </div>
            </div>
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Bottom pagination */}
      <div className="flex items-center justify-between px-6 py-3 border-t border-border/60 bg-surface-elevated">
        <button
          onClick={prev} disabled={page === 0}
          className="flex items-center gap-1.5 h-8 rounded-xl border border-border/80 px-3 text-xs font-medium text-muted-foreground hover:text-foreground hover:border-primary/30 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
        >
          <ChevronLeft className="h-3.5 w-3.5" />
          Previous page
        </button>

        <div className="flex items-center gap-1.5">
          {pages.map((_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
              className={`h-6 w-6 rounded-md text-[10px] font-semibold transition-all ${
                i === page
                  ? "bg-primary text-primary-foreground"
                  : "border border-border/70 text-muted-foreground hover:border-primary/30"
              }`}
            >
              {i + 1}
            </button>
          ))}
        </div>

        <button
          onClick={next} disabled={page === pages.length - 1}
          className="flex items-center gap-1.5 h-8 rounded-xl border border-border/80 px-3 text-xs font-medium text-muted-foreground hover:text-foreground hover:border-primary/30 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
        >
          Next page
          <ChevronRight className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
}

// ─── Mini Quiz ────────────────────────────────────────────────────────────────

function MiniQuiz({ questions }: { questions: QuizQuestion[] }) {
  const [current,   setCurrent]   = useState(0);
  const [answers,   setAnswers]   = useState<Record<string, string>>({});
  const [revealed,  setRevealed]  = useState<Set<string>>(new Set());
  const [finished,  setFinished]  = useState(false);

  const q        = questions[current];
  const selected = answers[q.id];
  const isCorrect = selected === q.correct;
  const showed    = revealed.has(q.id);

  function choose(optId: string) {
    if (showed) return;
    setAnswers(prev => ({ ...prev, [q.id]: optId }));
    setRevealed(prev => new Set([...prev, q.id]));
  }

  function retry() {
    setAnswers({});
    setRevealed(new Set());
    setCurrent(0);
    setFinished(false);
  }

  const correct = questions.filter(q => answers[q.id] === q.correct).length;
  const score   = finished ? Math.round((correct / questions.length) * 100) : 0;

  if (finished) {
    return (
      <div className="max-w-xl mx-auto px-6 py-8 space-y-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.92 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
          className={`rounded-2xl p-6 text-center ${
            score >= 67
              ? "bg-gradient-to-br from-primary to-primary-glow"
              : "bg-gradient-to-br from-[oklch(0.60_0.12_25)] to-[oklch(0.50_0.14_20)]"
          }`}
        >
          <div className="grid h-14 w-14 place-items-center rounded-2xl border border-white/20 bg-white/15 mx-auto mb-3">
            {score >= 67
              ? <CheckCircle className="h-7 w-7 text-white" strokeWidth={1.75} />
              : <XCircle     className="h-7 w-7 text-white" strokeWidth={1.75} />
            }
          </div>
          <p className="text-3xl font-bold text-white tabular-nums">{score}%</p>
          <p className="text-sm text-white/70 mt-1">{correct} / {questions.length} correct</p>
        </motion.div>

        {/* Answer review */}
        <div className="space-y-3">
          {questions.map((qq, i) => {
            const a = answers[qq.id];
            const ok = a === qq.correct;
            const correctOpt = qq.options.find(o => o.id === qq.correct);
            return (
              <div key={qq.id} className={`rounded-xl border p-4 space-y-2 ${ok ? "border-teal/25 bg-teal/4" : "border-destructive/20 bg-destructive/4"}`}>
                <div className="flex items-start gap-2">
                  <div className={`grid h-5 w-5 shrink-0 place-items-center rounded-full mt-0.5 ${ok ? "bg-teal/15 text-teal" : "bg-destructive/15 text-destructive"}`}>
                    {ok ? <CheckCircle className="h-3.5 w-3.5" strokeWidth={2} /> : <XCircle className="h-3.5 w-3.5" strokeWidth={2} />}
                  </div>
                  <p className="text-sm font-medium text-foreground">{i + 1}. {qq.text}</p>
                </div>
                <p className="text-xs text-muted-foreground pl-7">
                  ✓ <span className="font-medium text-foreground">{correctOpt?.text}</span>
                </p>
                <p className="text-xs text-muted-foreground/80 pl-7 italic">{qq.explanation}</p>
              </div>
            );
          })}
        </div>

        <button
          onClick={retry}
          className="w-full flex items-center justify-center gap-2 h-10 rounded-xl border border-border/80 text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-accent transition-all"
        >
          <RotateCcw className="h-4 w-4" />
          Retry quiz
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-xl mx-auto px-6 py-8 space-y-6">
      {/* Progress */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>Question {current + 1} of {questions.length}</span>
          <span className="tabular-nums">{Math.round(((current) / questions.length) * 100)}% done</span>
        </div>
        <div className="h-1.5 rounded-full bg-border overflow-hidden">
          <motion.div
            className="h-full rounded-full bg-primary"
            animate={{ width: `${(current / questions.length) * 100}%` }}
            transition={{ duration: 0.3 }}
          />
        </div>
      </div>

      <AnimatePresence mode="wait">
        <motion.div
          key={q.id}
          initial={{ opacity: 0, x: 16 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -16 }}
          transition={{ duration: 0.22 }}
          className="space-y-5"
        >
          {/* Question */}
          <div className="rounded-2xl border border-border/70 bg-surface-elevated p-5" style={{ boxShadow: "var(--shadow-soft)" }}>
            <p className="text-xs font-semibold text-primary uppercase tracking-[0.16em] mb-2">Question {current + 1}</p>
            <p className="text-base font-semibold text-foreground leading-relaxed">{q.text}</p>
          </div>

          {/* Options */}
          <div className="space-y-2">
            {q.options.map(opt => {
              const isSelected = selected === opt.id;
              const isRight    = opt.id === q.correct;
              let style = "border-border/70 bg-surface-elevated hover:border-primary/30 hover:bg-primary/3";
              if (showed) {
                if (isRight)             style = "border-teal/50 bg-teal/8";
                else if (isSelected)     style = "border-destructive/40 bg-destructive/8";
                else                     style = "border-border/40 bg-surface opacity-50";
              } else if (isSelected) {
                style = "border-primary/50 bg-primary/8";
              }
              return (
                <button
                  key={opt.id}
                  onClick={() => choose(opt.id)}
                  disabled={showed}
                  className={`w-full flex items-center gap-3.5 rounded-xl border p-4 text-left transition-all duration-200 ${style}`}
                >
                  <div className={`grid h-6 w-6 shrink-0 place-items-center rounded-full border text-xs font-bold transition-all ${
                    showed && isRight   ? "border-teal bg-teal text-white" :
                    showed && isSelected && !isRight ? "border-destructive bg-destructive text-white" :
                    isSelected ? "border-primary bg-primary text-primary-foreground" :
                    "border-border/80 text-muted-foreground"
                  }`}>
                    {showed && isRight    ? <CheckCircle className="h-3.5 w-3.5" strokeWidth={2.5} /> :
                     showed && isSelected && !isRight ? <XCircle className="h-3.5 w-3.5" strokeWidth={2.5} /> :
                     opt.id.toUpperCase()}
                  </div>
                  <span className={`text-sm font-medium ${
                    showed && isRight ? "text-teal" :
                    showed && isSelected && !isRight ? "text-destructive" :
                    "text-foreground"
                  }`}>{opt.text}</span>
                </button>
              );
            })}
          </div>

          {/* Explanation */}
          <AnimatePresence>
            {showed && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: "auto" }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.25 }}
                className={`overflow-hidden rounded-xl border p-4 ${isCorrect ? "border-teal/30 bg-teal/6" : "border-amber-400/30 bg-amber-400/6"}`}
              >
                <p className={`text-xs font-semibold mb-1 ${isCorrect ? "text-teal" : "text-amber-600"}`}>
                  {isCorrect ? "Correct!" : "Not quite"}
                </p>
                <p className="text-xs text-muted-foreground leading-relaxed">{q.explanation}</p>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      </AnimatePresence>

      {/* Navigation */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => setCurrent(v => Math.max(0, v - 1))}
          disabled={current === 0}
          className="flex items-center gap-1.5 h-9 rounded-xl border border-border/80 px-4 text-sm font-medium text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed transition-all"
        >
          <ChevronLeft className="h-4 w-4" /> Back
        </button>

        {current < questions.length - 1 ? (
          <button
            onClick={() => setCurrent(v => v + 1)}
            disabled={!showed}
            className="flex items-center gap-1.5 h-9 rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition-opacity shadow-soft"
          >
            Next <ChevronRight className="h-4 w-4" />
          </button>
        ) : (
          <button
            onClick={() => setFinished(true)}
            disabled={!showed}
            className="flex items-center gap-1.5 h-9 rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition-opacity shadow-soft"
          >
            See results <CheckCircle className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Homework ─────────────────────────────────────────────────────────────────

function HomeworkPanel({ hw }: { hw: LessonData["homework"] }) {
  const [text,       setText]      = useState("");
  const [files,      setFiles]     = useState<string[]>([]);
  const [submitted,  setSubmitted] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const deadline   = new Date(hw.deadline);
  const daysLeft   = Math.ceil((deadline.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
  const deadlineStr = deadline.toLocaleDateString("en-GB", { day: "numeric", month: "long" });

  function fakeAttach() {
    const names = ["solution.html", "styles.css", "screenshot.png", "index.html"];
    const pick  = names[Math.floor(Math.random() * names.length)];
    if (!files.includes(pick)) setFiles(prev => [...prev, pick]);
  }

  if (submitted) {
    return (
      <div className="max-w-xl mx-auto px-6 py-12 flex flex-col items-center text-center space-y-4">
        <motion.div
          initial={{ scale: 0.6, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="grid h-20 w-20 place-items-center rounded-3xl bg-gradient-to-br from-teal to-[oklch(0.55_0.15_185)] shadow-elevated"
        >
          <CheckCircle className="h-10 w-10 text-white" strokeWidth={1.5} />
        </motion.div>
        <h3 className="text-display text-xl text-foreground">Homework submitted!</h3>
        <p className="text-sm text-muted-foreground">Your work has been sent for review. You'll be notified when it's graded.</p>
        <div className="flex items-center gap-2 rounded-full border border-teal/25 bg-teal/8 px-4 py-1.5 text-xs font-medium text-teal">
          <Zap className="h-3.5 w-3.5" />
          +{hw.points} XP pending review
        </div>
        <button
          onClick={() => { setSubmitted(false); setText(""); setFiles([]); }}
          className="text-xs text-muted-foreground hover:text-foreground transition-colors underline underline-offset-2"
        >
          Submit a revision
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-6 py-8 space-y-6">

      {/* Assignment header */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-2xl border border-border/70 bg-surface-elevated p-6 space-y-4"
        style={{ boxShadow: "var(--shadow-soft)" }}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <span className="h-1 w-1 rounded-full bg-primary/60" />
              <h3 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Assignment</h3>
            </div>
            <h2 className="text-base font-semibold text-display text-foreground">{hw.title}</h2>
            <p className="text-sm text-muted-foreground mt-1">{hw.description}</p>
          </div>
          <div className="shrink-0 text-right">
            <p className="text-lg font-bold text-display text-foreground tabular-nums">{hw.points}</p>
            <p className="text-[10px] text-muted-foreground">points</p>
          </div>
        </div>

        <div className="flex items-center gap-3 text-xs">
          <div className={`flex items-center gap-1.5 rounded-full px-3 py-1 font-medium ${
            daysLeft <= 2
              ? "bg-destructive/10 text-destructive border border-destructive/20"
              : daysLeft <= 5
              ? "bg-gold/10 text-gold border border-gold/20"
              : "bg-teal/10 text-teal border border-teal/20"
          }`}>
            <Clock className="h-3 w-3" />
            Due {deadlineStr} · {daysLeft > 0 ? `${daysLeft} days left` : "Overdue"}
          </div>
        </div>

        {/* Instructions */}
        <div className="space-y-2">
          <p className="text-xs font-semibold text-foreground">Instructions:</p>
          <ol className="space-y-1.5">
            {hw.instructions.map((step, i) => (
              <li key={i} className="flex items-start gap-2.5 text-sm text-muted-foreground">
                <span className="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-primary/10 text-[10px] font-bold text-primary mt-0.5">
                  {i + 1}
                </span>
                {step}
              </li>
            ))}
          </ol>
        </div>
      </motion.div>

      {/* Submission area */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="rounded-2xl border border-border/70 bg-surface-elevated p-6 space-y-4"
        style={{ boxShadow: "var(--shadow-soft)" }}
      >
        <div className="flex items-center gap-3">
          <span className="h-1 w-1 rounded-full bg-primary/60" />
          <h3 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Your submission</h3>
        </div>

        {/* Text area */}
        <div>
          <label className="block text-xs font-medium text-foreground mb-1.5">Notes / explanation</label>
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            rows={5}
            placeholder="Describe your approach, link to a live demo, or add notes for your instructor…"
            className="w-full rounded-xl border border-border/80 bg-surface px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/40 outline-none focus:border-primary/40 focus:ring-2 focus:ring-ring/15 transition-all resize-none"
          />
          <p className="text-[10px] text-muted-foreground mt-1 text-right tabular-nums">{text.length} chars</p>
        </div>

        {/* File attachments */}
        <div>
          <label className="block text-xs font-medium text-foreground mb-1.5">Attachments</label>
          <input ref={fileInputRef} type="file" multiple className="hidden" onChange={() => fakeAttach()} />

          {files.length > 0 && (
            <div className="space-y-1.5 mb-2">
              {files.map((f, i) => (
                <div key={i} className="flex items-center gap-2.5 rounded-xl border border-border/60 bg-surface px-3 py-2">
                  <Paperclip className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                  <span className="text-xs text-foreground flex-1">{f}</span>
                  <button onClick={() => setFiles(prev => prev.filter((_, j) => j !== i))} className="text-muted-foreground hover:text-destructive transition-colors">
                    <XCircle className="h-3.5 w-3.5" strokeWidth={1.75} />
                  </button>
                </div>
              ))}
            </div>
          )}

          <button
            onClick={() => fileInputRef.current?.click()}
            className="flex items-center gap-2 h-9 rounded-xl border border-dashed border-border/80 px-4 text-xs font-medium text-muted-foreground hover:text-foreground hover:border-primary/30 transition-all w-full justify-center"
          >
            <Upload className="h-3.5 w-3.5" />
            Attach files
          </button>
        </div>

        {/* Submit */}
        <div className="flex items-center gap-3 pt-1">
          <button
            onClick={() => setSubmitted(true)}
            disabled={text.trim().length === 0 && files.length === 0}
            className="flex items-center gap-2 h-10 rounded-2xl bg-primary px-6 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition-opacity shadow-soft"
          >
            <Send className="h-4 w-4" />
            Submit homework
          </button>
          <p className="text-xs text-muted-foreground">
            {hw.points} points · graded within 48h
          </p>
        </div>
      </motion.div>

    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

function LessonPlayerPage() {
  const { courseId, lessonId } = Route.useParams();
  const [completed, setCompleted] = useState(false);
  const [activeTab, setActiveTab] = useState<ContentTab>("video");

  const lessonIndex = COURSE_LESSONS.findIndex(l => l.id === lessonId);
  const prevLesson  = lessonIndex > 0 ? COURSE_LESSONS[lessonIndex - 1] : null;
  const nextLesson  = lessonIndex < COURSE_LESSONS.length - 1 ? COURSE_LESSONS[lessonIndex + 1] : null;
  const isLast      = !nextLesson;

  const lesson = LESSON_DATA[lessonId] ?? LESSON_DATA["7"];

  const tabs: { id: ContentTab; label: string; icon: React.ElementType; available: boolean }[] = [
    { id: "video",    label: "Video",    icon: PlayCircle, available: true },
    { id: "pdf",      label: "Reading",  icon: FileText,   available: lesson.hasPdf },
    { id: "quiz",     label: "Quiz",     icon: HelpCircle, available: lesson.hasQuiz },
    { id: "homework", label: "Homework", icon: PenLine,    available: lesson.hasHomework },
  ];

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">

      {/* Top bar */}
      <header className="flex h-14 shrink-0 items-center gap-4 border-b border-border/60 bg-surface-elevated/90 backdrop-blur-md px-6 sticky top-0 z-20">
        <Link
          to="/courses/$courseId"
          params={{ courseId }}
          className="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          <span className="hidden sm:inline">Course</span>
        </Link>

        {/* Lesson nav */}
        <div className="flex items-center gap-1 mx-auto">
          {prevLesson && (
            <Link
              to="/courses/$courseId/lessons/$lessonId"
              params={{ courseId, lessonId: prevLesson.id }}
              className="flex items-center gap-1 h-8 rounded-xl px-3 text-xs font-medium text-muted-foreground border border-border/70 hover:text-foreground hover:border-primary/30 transition-all"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
              Prev
            </Link>
          )}
          <span className="px-3 text-xs text-muted-foreground tabular-nums">
            {lessonIndex + 1} / {COURSE_LESSONS.length}
          </span>
          {nextLesson && (
            <Link
              to="/courses/$courseId/lessons/$lessonId"
              params={{ courseId, lessonId: nextLesson.id }}
              className="flex items-center gap-1 h-8 rounded-xl px-3 text-xs font-medium text-muted-foreground border border-border/70 hover:text-foreground hover:border-primary/30 transition-all"
            >
              Next
              <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          )}
        </div>

        <div className="flex items-center gap-2 ml-auto">
          {completed ? (
            <span className="flex items-center gap-1.5 text-xs font-medium text-teal">
              <CheckCircle className="h-3.5 w-3.5" />
              Done
            </span>
          ) : (
            <button
              onClick={() => setCompleted(true)}
              className="flex items-center gap-1.5 h-8 rounded-xl bg-primary px-4 text-xs font-semibold text-primary-foreground hover:opacity-90 transition-opacity shadow-soft"
            >
              <CheckCircle className="h-3.5 w-3.5" />
              Mark complete
            </button>
          )}
        </div>
      </header>

      <div className="flex flex-1 min-h-0">

        {/* Main content */}
        <div className="flex-1 overflow-y-auto flex flex-col">

          {/* Video always on top */}
          <VideoPlayer duration={lesson.duration} />

          {/* Lesson title + XP */}
          <div className="max-w-4xl w-full mx-auto px-6 lg:px-8 pt-6 pb-2">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h1 className="text-display text-xl sm:text-2xl text-foreground leading-tight">{lesson.title}</h1>
                <p className="mt-1 text-sm text-muted-foreground leading-relaxed max-w-xl">{lesson.description}</p>
              </div>
              <div className={`shrink-0 mt-1 inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium ${
                completed
                  ? "border-teal/25 bg-teal/8 text-teal"
                  : "border-gold/25 bg-gold/8 text-gold"
              }`}>
                {completed
                  ? <><CheckCircle className="h-3.5 w-3.5" />+20 XP</>
                  : <><Zap className="h-3.5 w-3.5" />+20 XP</>
                }
              </div>
            </div>
          </div>

          {/* Tabs */}
          <div className="max-w-4xl w-full mx-auto px-6 lg:px-8 pt-4">
            <div className="flex items-center gap-1 border-b border-border/60">
              {tabs.filter(t => t.available).map(tab => {
                const Icon = tab.icon;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-all duration-200 ${
                      activeTab === tab.id
                        ? "border-primary text-primary"
                        : "border-transparent text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    <Icon className="h-3.5 w-3.5" strokeWidth={activeTab === tab.id ? 2 : 1.75} />
                    {tab.label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Tab content */}
          <div className="flex-1">
            <AnimatePresence mode="wait">
              <motion.div
                key={activeTab}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.2 }}
              >
                {activeTab === "video" && (
                  <div className="max-w-4xl mx-auto px-6 lg:px-8 py-6 space-y-5 pb-10">

                    {/* Notes */}
                    <div className="rounded-2xl border border-border/70 bg-surface-elevated p-6" style={{ boxShadow: "var(--shadow-soft)" }}>
                      <div className="flex items-center gap-3 mb-4">
                        <span className="h-1 w-1 rounded-full bg-primary/60" />
                        <h2 className="text-xs uppercase tracking-[0.2em] text-muted-foreground font-medium">Lesson notes</h2>
                      </div>
                      <div className="space-y-3">
                        {lesson.content.split("\n\n").map((block, i) => {
                          if (block.startsWith("## ")) return <h3 key={i} className="text-sm font-semibold text-foreground mt-3 mb-1">{block.slice(3)}</h3>;
                          if (block.startsWith("- ")) return (
                            <ul key={i} className="space-y-1">
                              {block.split("\n").map((l, j) => (
                                <li key={j} className="flex items-start gap-2 text-sm text-muted-foreground">
                                  <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-primary/60 shrink-0" />
                                  <span>{l.slice(2).replace(/`([^`]+)`/g, (_, c) => c)}</span>
                                </li>
                              ))}
                            </ul>
                          );
                          return <p key={i} className="text-sm text-muted-foreground leading-relaxed">{block}</p>;
                        })}
                      </div>
                    </div>

                    {/* Bottom nav */}
                    <div className="flex items-center justify-between pt-2">
                      {prevLesson ? (
                        <Link
                          to="/courses/$courseId/lessons/$lessonId"
                          params={{ courseId, lessonId: prevLesson.id }}
                          className="flex items-center gap-2 h-10 rounded-2xl border border-border/80 px-5 text-sm font-medium text-muted-foreground hover:text-foreground hover:border-primary/30 transition-all"
                        >
                          <ArrowLeft className="h-4 w-4" /> Previous
                        </Link>
                      ) : <div />}
                      {isLast ? (
                        <Link
                          to="/courses/$courseId/exam"
                          params={{ courseId }}
                          className="flex items-center gap-2 h-10 rounded-2xl bg-primary px-6 text-sm font-semibold text-primary-foreground hover:opacity-90 transition-opacity shadow-soft"
                        >
                          <FileQuestion className="h-4 w-4" /> Take exam
                        </Link>
                      ) : nextLesson ? (
                        <Link
                          to="/courses/$courseId/lessons/$lessonId"
                          params={{ courseId, lessonId: nextLesson.id }}
                          className="flex items-center gap-2 h-10 rounded-2xl bg-primary px-6 text-sm font-semibold text-primary-foreground hover:opacity-90 transition-opacity shadow-soft"
                        >
                          Next lesson <ArrowRight className="h-4 w-4" />
                        </Link>
                      ) : null}
                    </div>
                  </div>
                )}

                {activeTab === "pdf" && (
                  <PdfReader pdfTitle={lesson.pdfTitle} pages={lesson.pdfPages} />
                )}

                {activeTab === "quiz" && (
                  <MiniQuiz questions={lesson.quiz} />
                )}

                {activeTab === "homework" && (
                  <HomeworkPanel hw={lesson.homework} />
                )}
              </motion.div>
            </AnimatePresence>
          </div>
        </div>

        {/* Lesson list sidebar */}
        <aside className="hidden xl:flex w-64 shrink-0 flex-col border-l border-border/60 bg-surface-elevated">
          <div className="px-4 py-3.5 border-b border-border/60">
            <p className="text-[10px] uppercase tracking-[0.18em] text-muted-foreground/60 font-medium">Course content</p>
          </div>
          <div className="divide-y divide-border/50 flex-1 overflow-y-auto">
            {COURSE_LESSONS.map((l, i) => {
              const isCurrent = l.id === lessonId;
              const isDone    = parseInt(l.id) < parseInt(lessonId);
              return (
                <Link
                  key={l.id}
                  to="/courses/$courseId/lessons/$lessonId"
                  params={{ courseId, lessonId: l.id }}
                  className={`flex items-center gap-3 px-4 py-3 transition-colors ${
                    isCurrent ? "bg-primary/8 text-primary" : "text-muted-foreground hover:bg-accent/40 hover:text-foreground"
                  }`}
                >
                  <div className={`grid h-6 w-6 shrink-0 place-items-center rounded-lg text-[10px] font-bold ${
                    isCurrent ? "bg-primary text-primary-foreground" :
                    isDone    ? "bg-primary/10 text-primary" :
                                "bg-border/60 text-muted-foreground"
                  }`}>
                    {isDone ? <CheckCircle className="h-3.5 w-3.5" strokeWidth={2.5} /> : i + 1}
                  </div>
                  <span className="leading-snug line-clamp-2 text-xs">{l.title}</span>
                </Link>
              );
            })}
          </div>
        </aside>

      </div>
    </div>
  );
}
