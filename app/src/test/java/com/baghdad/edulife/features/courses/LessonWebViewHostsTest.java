package com.baghdad.edulife.features.courses;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.baghdad.edulife.features.courses.model.LessonWebViewHosts;

import org.junit.Test;

import java.util.Set;

/**
 * Locks in the WebView allowlist: the backend host is derived from the API base URL and the
 * static video providers are added unconditionally so an embedded YouTube/Vimeo lesson keeps
 * playing inside the player while an arbitrary teacher-provided host does not.
 */
public class LessonWebViewHostsTest {

    @Test public void includesBackendHost() {
        Set<String> hosts = LessonWebViewHosts.forApiBaseUrl("https://api.edulife.com/api/v1/");
        assertTrue(hosts.contains("api.edulife.com"));
    }

    @Test public void includesStaticVideoProviders() {
        Set<String> hosts = LessonWebViewHosts.forApiBaseUrl("https://api.edulife.com/api/v1/");
        assertTrue(hosts.contains("www.youtube.com"));
        assertTrue(hosts.contains("youtube.com"));
        assertTrue(hosts.contains("youtu.be"));
        assertTrue(hosts.contains("www.youtube-nocookie.com"));
        assertTrue(hosts.contains("player.vimeo.com"));
        assertTrue(hosts.contains("vimeo.com"));
    }

    @Test public void rejectsArbitraryHosts() {
        Set<String> hosts = LessonWebViewHosts.forApiBaseUrl("https://api.edulife.com/api/v1/");
        assertFalse(hosts.contains("evil.example.com"));
        assertFalse(hosts.contains("docs.google.com"));
    }

    @Test public void emptyBaseUrl_stillIncludesVideoProviders() {
        Set<String> hosts = LessonWebViewHosts.forApiBaseUrl("");
        assertTrue(hosts.contains("www.youtube.com"));
    }
}
