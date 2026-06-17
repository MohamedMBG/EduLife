package com.baghdad.edulife.features.courses.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * Resolves the URL actually loaded into the in-app WebView. PDF content (by lesson type or
     * by {@code .pdf} extension) is wrapped in the Google Docs viewer; everything else loads
     * directly. Pure and host-JVM testable. Moved verbatim from {@code LessonPlayerFragment}.
     */
    public static String resolveViewerUrl(String lessonType, String contentUrl) {
        String type = lessonType == null ? "" : lessonType.toUpperCase(Locale.ROOT);
        String url = contentUrl.toLowerCase(Locale.ROOT);
        boolean looksLikePdf = type.equals("PDF") || url.endsWith(".pdf");
        if (looksLikePdf) {
            try {
                String encoded = URLEncoder.encode(contentUrl, StandardCharsets.UTF_8.name());
                return "https://docs.google.com/gview?embedded=true&url=" + encoded;
            } catch (UnsupportedEncodingException e) {
                return contentUrl;
            }
        }
        return contentUrl;
    }
}
