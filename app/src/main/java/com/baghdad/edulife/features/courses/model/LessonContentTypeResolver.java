package com.baghdad.edulife.features.courses.model;

import java.util.Locale;

/**
 * Pure decision logic for how a lesson should be presented, extracted from
 * {@code LessonPlayerFragment} so it can be unit-tested without Android Views.
 *
 * Given a lesson type and the available content (URL / inline body), it decides which
 * single content surface the player should show and whether the video header is used.
 * The Fragment maps the resulting {@link Display} onto concrete views and string labels;
 * this class never touches Android UI.
 */
public final class LessonContentTypeResolver {

    /** Which content surface the player should render. */
    public enum Display {
        /** Video header only; no inline content card (VIDEO lesson with no body). */
        VIDEO_ONLY,
        /** Video header plus an inline text body (VIDEO lesson that also carries notes). */
        VIDEO_WITH_TEXT,
        /** Inline rich-text content area. */
        TEXT,
        /** Article/link card with an "open" button. */
        ARTICLE,
        /** PDF resource card (opened through the in-app document viewer). */
        PDF,
        /** Generic downloadable/external resource card. */
        RESOURCE,
        /** Nothing renderable was supplied — show the safe fallback card. */
        FALLBACK
    }

    public static final class Result {
        /** The surface to display. */
        public final Display display;
        /** True when the large video header is shown (VIDEO types); false uses the compact bar. */
        public final boolean videoHeader;
        /**
         * For {@link Display#ARTICLE}, {@link Display#PDF}, {@link Display#RESOURCE}: whether the
         * open/view action is usable (a URL is present). Always true for TEXT/VIDEO surfaces and
         * false for {@link Display#FALLBACK}.
         */
        public final boolean actionEnabled;

        Result(Display display, boolean videoHeader, boolean actionEnabled) {
            this.display = display;
            this.videoHeader = videoHeader;
            this.actionEnabled = actionEnabled;
        }
    }

    /**
     * Coarse content kind used by list rows (icons/badges) that only need the lesson's category,
     * not the full surface decision. Single source of the accepted type-name spellings so callers
     * never re-list "VIDEO"/"ARTICLE"/… strings of their own.
     */
    public enum Kind { VIDEO, ARTICLE, TEXT, PDF, RESOURCE, UNKNOWN }

    /** Maps a raw lesson type to a {@link Kind}; null/blank/unrecognised → {@link Kind#UNKNOWN}. */
    public static Kind classifyKind(String lessonType) {
        String type = lessonType != null ? lessonType.toUpperCase(Locale.ROOT) : "";
        switch (type) {
            case "VIDEO":
                return Kind.VIDEO;
            case "ARTICLE":
            case "LINK":
                return Kind.ARTICLE;
            case "TEXT":
                return Kind.TEXT;
            case "PDF":
                return Kind.PDF;
            case "RESOURCE":
                return Kind.RESOURCE;
            default:
                return Kind.UNKNOWN;
        }
    }

    private LessonContentTypeResolver() {
    }

    /**
     * Resolves the display decision. Mirrors the original switch in
     * {@code LessonPlayerFragment.bindLessonContent} exactly.
     *
     * @param lessonType  raw lesson type (case-insensitive; null/blank treated as unknown)
     * @param contentUrl  external content URL (null/blank when absent)
     * @param contentBody inline content body (null/blank when absent)
     */
    public static Result resolve(String lessonType, String contentUrl, String contentBody) {
        String type = lessonType != null ? lessonType.toUpperCase(Locale.ROOT) : "";
        boolean hasUrl = contentUrl != null && !contentUrl.trim().isEmpty();
        boolean hasBody = contentBody != null && !contentBody.trim().isEmpty();

        switch (type) {
            case "VIDEO":
                return hasBody
                        ? new Result(Display.VIDEO_WITH_TEXT, true, true)
                        : new Result(Display.VIDEO_ONLY, true, true);

            case "TEXT":
                if (hasBody) {
                    return new Result(Display.TEXT, false, true);
                } else if (hasUrl) {
                    return new Result(Display.ARTICLE, false, true);
                }
                return new Result(Display.FALLBACK, false, false);

            case "ARTICLE":
            case "LINK":
                if (hasUrl) {
                    return new Result(Display.ARTICLE, false, true);
                } else if (hasBody) {
                    return new Result(Display.TEXT, false, true);
                }
                return new Result(Display.FALLBACK, false, false);

            case "PDF":
                // The PDF card is always shown; the open action is only usable with a URL.
                return new Result(Display.PDF, false, hasUrl);

            case "RESOURCE":
                // The resource card is always shown; the open action is only usable with a URL.
                return new Result(Display.RESOURCE, false, hasUrl);

            default:
                if (hasUrl) {
                    return new Result(Display.ARTICLE, false, true);
                } else if (hasBody) {
                    return new Result(Display.TEXT, false, true);
                }
                return new Result(Display.FALLBACK, false, false);
        }
    }

    /**
     * Returns the URL the in-app WebView should load. PDF content used to be wrapped in
     * {@code https://docs.google.com/gview} which leaked private (potentially signed) lesson
     * URLs to a third party — the 2026-06 OWASP audit flagged this as M6/M9. The PDF flow now
     * downloads to private cache through the authenticated OkHttp client and opens via
     * FileProvider, so the WebView never needs the GDocs wrapper. The method is kept (returning
     * the raw URL) so a future in-app PDF renderer can plug in here without touching callers.
     */
    public static String resolveViewerUrl(String lessonType, String contentUrl) {
        return contentUrl;
    }

    /**
     * True when the lesson should go through the authenticated download + FileProvider path
     * instead of being rendered inline. PDF lesson types and {@code .pdf} URLs both qualify.
     */
    public static boolean shouldDownloadInsteadOfInline(String lessonType, String contentUrl) {
        String type = lessonType == null ? "" : lessonType.toUpperCase(Locale.ROOT);
        String url = contentUrl == null ? "" : contentUrl.toLowerCase(Locale.ROOT);
        return type.equals("PDF") || url.endsWith(".pdf");
    }
}
