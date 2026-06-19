package com.baghdad.edulife.features.courses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.baghdad.edulife.features.courses.model.LessonContentTypeResolver;
import com.baghdad.edulife.features.courses.model.LessonContentTypeResolver.Display;
import com.baghdad.edulife.features.courses.model.LessonContentTypeResolver.Result;

import org.junit.Test;

/**
 * Host-JVM tests for the lesson display decision extracted from LessonPlayerFragment. They lock
 * in which single content surface each lesson type maps to (and when the video header is shown),
 * including the fall-through rules and the safe fallback for unknown/empty content.
 */
public class LessonContentTypeResolverTest {

    @Test
    public void videoWithoutBody_showsVideoHeaderOnly() {
        Result r = LessonContentTypeResolver.resolve("VIDEO", "https://cdn/video.mp4", "");
        assertEquals(Display.VIDEO_ONLY, r.display);
        assertTrue(r.videoHeader);
    }

    @Test
    public void videoWithBody_showsVideoHeaderPlusText() {
        Result r = LessonContentTypeResolver.resolve("VIDEO", "https://cdn/video.mp4", "Lecture notes");
        assertEquals(Display.VIDEO_WITH_TEXT, r.display);
        assertTrue(r.videoHeader);
    }

    @Test
    public void lessonTypeIsCaseInsensitive() {
        Result r = LessonContentTypeResolver.resolve("video", null, null);
        assertEquals(Display.VIDEO_ONLY, r.display);
        assertTrue(r.videoHeader);
    }

    @Test
    public void textWithBody_showsTextAndHidesVideoHeader() {
        Result r = LessonContentTypeResolver.resolve("TEXT", null, "Some rich text");
        assertEquals(Display.TEXT, r.display);
        assertFalse(r.videoHeader);
    }

    @Test
    public void textWithoutBodyButWithUrl_fallsBackToArticle() {
        Result r = LessonContentTypeResolver.resolve("TEXT", "https://example.com/article", "");
        assertEquals(Display.ARTICLE, r.display);
        assertTrue(r.actionEnabled);
        assertFalse(r.videoHeader);
    }

    @Test
    public void textWithNothing_showsFallback() {
        Result r = LessonContentTypeResolver.resolve("TEXT", "", "");
        assertEquals(Display.FALLBACK, r.display);
        assertFalse(r.actionEnabled);
    }

    @Test
    public void articleWithUrl_showsArticleCard() {
        Result r = LessonContentTypeResolver.resolve("ARTICLE", "https://example.com/post", null);
        assertEquals(Display.ARTICLE, r.display);
        assertTrue(r.actionEnabled);
        assertFalse(r.videoHeader);
    }

    @Test
    public void linkTypeBehavesLikeArticle() {
        Result r = LessonContentTypeResolver.resolve("LINK", "https://example.com/post", null);
        assertEquals(Display.ARTICLE, r.display);
        assertTrue(r.actionEnabled);
    }

    @Test
    public void articleWithoutUrlButWithBody_showsText() {
        Result r = LessonContentTypeResolver.resolve("ARTICLE", null, "Inline body");
        assertEquals(Display.TEXT, r.display);
    }

    @Test
    public void articleWithNothing_showsFallback() {
        Result r = LessonContentTypeResolver.resolve("ARTICLE", null, null);
        assertEquals(Display.FALLBACK, r.display);
        assertFalse(r.actionEnabled);
    }

    @Test
    public void pdfWithUrl_showsPdfResourceWithEnabledAction() {
        Result r = LessonContentTypeResolver.resolve("PDF", "https://cdn/file.pdf", null);
        assertEquals(Display.PDF, r.display);
        assertTrue(r.actionEnabled);
        assertFalse(r.videoHeader);
    }

    @Test
    public void pdfWithoutUrl_showsPdfResourceWithDisabledAction() {
        Result r = LessonContentTypeResolver.resolve("PDF", "", null);
        assertEquals(Display.PDF, r.display);
        assertFalse(r.actionEnabled);
    }

    @Test
    public void resourceWithUrl_showsResourceWithEnabledAction() {
        Result r = LessonContentTypeResolver.resolve("RESOURCE", "https://cdn/file.zip", null);
        assertEquals(Display.RESOURCE, r.display);
        assertTrue(r.actionEnabled);
    }

    @Test
    public void resourceWithoutUrl_showsResourceWithDisabledAction() {
        Result r = LessonContentTypeResolver.resolve("RESOURCE", null, null);
        assertEquals(Display.RESOURCE, r.display);
        assertFalse(r.actionEnabled);
    }

    @Test
    public void unknownTypeWithUrl_fallsBackToArticle() {
        Result r = LessonContentTypeResolver.resolve("SOMETHING_NEW", "https://example.com/x", null);
        assertEquals(Display.ARTICLE, r.display);
        assertTrue(r.actionEnabled);
        assertFalse(r.videoHeader);
    }

    @Test
    public void unknownTypeWithBodyOnly_showsText() {
        Result r = LessonContentTypeResolver.resolve("MYSTERY", null, "Body");
        assertEquals(Display.TEXT, r.display);
    }

    @Test
    public void nullTypeWithNothing_showsFallback_noCrash() {
        Result r = LessonContentTypeResolver.resolve(null, null, null);
        assertEquals(Display.FALLBACK, r.display);
        assertFalse(r.videoHeader);
        assertFalse(r.actionEnabled);
    }

    @Test
    public void blankAndWhitespaceContent_treatedAsAbsent() {
        Result r = LessonContentTypeResolver.resolve("TEXT", "   ", "   ");
        assertEquals(Display.FALLBACK, r.display);
    }

    // ── resolveViewerUrl: PDF is no longer wrapped in Google Docs Viewer ──

    @Test
    public void pdfTypeUrl_isReturnedRawSoCallerCanRouteToAuthenticatedDownload() {
        String url = "https://cdn/file.pdf";
        // P2 audit (2026-06): we stopped wrapping PDFs in https://docs.google.com/gview to
        // avoid leaking private lesson URLs to a third party. PDFs now go through the
        // download-to-private-cache path; this method just returns the URL unchanged.
        assertEquals(url, LessonContentTypeResolver.resolveViewerUrl("PDF", url));
    }

    @Test
    public void pdfExtensionUrl_isReturnedRawEvenWhenTypeIsNotPdf() {
        String url = "https://cdn/report.PDF";
        assertEquals(url, LessonContentTypeResolver.resolveViewerUrl("RESOURCE", url));
    }

    @Test
    public void nonPdfUrl_isLoadedDirectly() {
        String url = "https://example.com/page";
        assertEquals(url, LessonContentTypeResolver.resolveViewerUrl("ARTICLE", url));
    }

    // ── shouldDownloadInsteadOfInline: PDF routing ──

    @Test
    public void pdfLessonType_routesToDownload() {
        assertTrue(LessonContentTypeResolver.shouldDownloadInsteadOfInline("PDF", "https://cdn/x"));
    }

    @Test
    public void pdfExtensionUrl_routesToDownload() {
        assertTrue(LessonContentTypeResolver.shouldDownloadInsteadOfInline("RESOURCE", "https://cdn/x.pdf"));
    }

    @Test
    public void nonPdf_doesNotRouteToDownload() {
        assertFalse(LessonContentTypeResolver.shouldDownloadInsteadOfInline("ARTICLE", "https://example.com/post"));
        assertFalse(LessonContentTypeResolver.shouldDownloadInsteadOfInline("VIDEO", "https://youtube.com/watch?v=abc"));
    }
}
