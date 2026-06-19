package com.baghdad.edulife.core.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.baghdad.edulife.core.web.UrlSecurityPolicy.Decision;

import org.junit.Test;

import java.util.Set;

/**
 * Host-JVM tests for the lesson URL safety decision. They lock in which schemes are blocked,
 * which hosts are allowed inside the in-app WebView vs. opened externally, and the lower-casing
 * behavior that makes the allowlist case-insensitive.
 */
public class UrlSecurityPolicyTest {

    private final Set<String> trusted = UrlSecurityPolicy.trustedHosts(
            "api.edulife.com",
            "www.youtube.com");

    @Test public void trustedHttpsHost_allowsInApp() {
        assertEquals(Decision.ALLOW_IN_APP,
                UrlSecurityPolicy.classify("https://api.edulife.com/v1/lesson", trusted));
    }

    @Test public void hostMatchIsCaseInsensitive() {
        assertEquals(Decision.ALLOW_IN_APP,
                UrlSecurityPolicy.classify("https://API.EDULIFE.COM/v1", trusted));
    }

    @Test public void untrustedHttpsHost_allowsExternalOnly() {
        assertEquals(Decision.ALLOW_EXTERNAL,
                UrlSecurityPolicy.classify("https://malicious.example.com/x", trusted));
    }

    @Test public void httpScheme_isBlocked() {
        assertEquals(Decision.BLOCK,
                UrlSecurityPolicy.classify("http://api.edulife.com/v1/lesson", trusted));
    }

    @Test public void fileScheme_isBlocked() {
        assertEquals(Decision.BLOCK,
                UrlSecurityPolicy.classify("file:///etc/passwd", trusted));
    }

    @Test public void javascriptScheme_isBlocked() {
        assertEquals(Decision.BLOCK,
                UrlSecurityPolicy.classify("javascript:alert(1)", trusted));
    }

    @Test public void intentScheme_isBlocked() {
        assertEquals(Decision.BLOCK, UrlSecurityPolicy.classify(
                "intent://launch#Intent;scheme=evil;end", trusted));
    }

    @Test public void contentScheme_isBlocked() {
        assertEquals(Decision.BLOCK, UrlSecurityPolicy.classify(
                "content://com.example.provider/file", trusted));
    }

    @Test public void unknownScheme_isBlocked() {
        assertEquals(Decision.BLOCK,
                UrlSecurityPolicy.classify("totallymadeup://path", trusted));
    }

    @Test public void nullUrl_isBlocked() {
        assertEquals(Decision.BLOCK, UrlSecurityPolicy.classify(null, trusted));
    }

    @Test public void blankUrl_isBlocked() {
        assertEquals(Decision.BLOCK, UrlSecurityPolicy.classify("   ", trusted));
    }

    @Test public void malformedUrl_isBlocked() {
        assertEquals(Decision.BLOCK, UrlSecurityPolicy.classify("not a url at all", trusted));
    }

    @Test public void httpsWithoutHost_isBlocked() {
        assertEquals(Decision.BLOCK, UrlSecurityPolicy.classify("https:///path", trusted));
    }

    @Test public void nullAllowlist_treatsEveryHttpsAsExternalOnly() {
        // No in-app allowlist means nothing ever loads inline — every HTTPS URL goes external.
        assertEquals(Decision.ALLOW_EXTERNAL,
                UrlSecurityPolicy.classify("https://example.com", null));
    }

    @Test public void isAllowedInApp_isShorthandForClassify() {
        assertTrue(UrlSecurityPolicy.isAllowedInApp("https://www.youtube.com/watch", trusted));
        assertFalse(UrlSecurityPolicy.isAllowedInApp("https://elsewhere.example.com", trusted));
        assertFalse(UrlSecurityPolicy.isAllowedInApp("http://www.youtube.com/watch", trusted));
    }

    @Test public void trustedHostsLowercasesInput() {
        Set<String> set = UrlSecurityPolicy.trustedHosts("WWW.Example.COM", "  api.x.org  ");
        assertTrue(set.contains("www.example.com"));
        assertTrue(set.contains("api.x.org"));
    }

    @Test public void hostOfReturnsLowercaseHost() {
        assertEquals("api.edulife.com", UrlSecurityPolicy.hostOf("https://API.EduLife.com/x"));
        assertEquals("", UrlSecurityPolicy.hostOf("not a url"));
    }
}
