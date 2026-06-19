package com.baghdad.edulife.core.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure URL safety policy for lesson and resource links.
 *
 * EduLife loads backend-controlled URLs into either an in-app WebView (video embeds, rich
 * content) or hands them to the system browser (articles, generic resources). Without this
 * policy, the player would happily render `file://`, `intent://`, `javascript:` and arbitrary
 * cleartext URLs — which an OWASP M4/M5/M8 audit (2026-06) flagged as a real risk because
 * lesson content is admin/teacher-authored.
 *
 * Uses {@link java.net.URI} (not {@code android.net.Uri}) so the policy is host-JVM testable
 * without Robolectric. Android UI code maps {@link Decision} onto WebView/Intent calls.
 */
public final class UrlSecurityPolicy {

    /** What the caller should do with a given URL. */
    public enum Decision {
        /** Trusted HTTPS + allowlisted host. Safe to render inside the in-app WebView. */
        ALLOW_IN_APP,
        /** HTTPS but not on the in-app allowlist. Open in the system browser / custom tab. */
        ALLOW_EXTERNAL,
        /** Cleartext HTTP, unknown scheme, or otherwise unsafe. Reject with a user message. */
        BLOCK
    }

    private UrlSecurityPolicy() {
    }

    /**
     * Builds an immutable, lower-cased host set so call sites don't have to repeat the
     * "host comparison must be case-insensitive" boilerplate every time.
     */
    public static Set<String> trustedHosts(String... hosts) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (hosts != null) {
            for (String host : hosts) {
                if (host == null) continue;
                String trimmed = host.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty()) set.add(trimmed);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    public static Decision classify(String url, Set<String> inAppTrustedHosts) {
        if (url == null) return Decision.BLOCK;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return Decision.BLOCK;

        URI parsed;
        try {
            parsed = new URI(trimmed);
        } catch (URISyntaxException e) {
            return Decision.BLOCK;
        }

        String scheme = parsed.getScheme();
        if (scheme == null) return Decision.BLOCK;
        scheme = scheme.toLowerCase(Locale.ROOT);

        // Anything not http/https is rejected on principle. file:// can leak local files into
        // the WebView, intent:// can launch arbitrary components, javascript: can inject script
        // into a loaded page, and content:// is meant for in-app providers, not user nav.
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return Decision.BLOCK;
        }

        // Cleartext is rejected. Release network security config disallows it anyway; rejecting
        // here gives a deterministic UX message instead of an opaque WebView load error.
        if (!scheme.equals("https")) {
            return Decision.BLOCK;
        }

        String host = parsed.getHost();
        if (host == null || host.isEmpty()) return Decision.BLOCK;
        host = host.toLowerCase(Locale.ROOT);

        if (inAppTrustedHosts != null && inAppTrustedHosts.contains(host)) {
            return Decision.ALLOW_IN_APP;
        }
        return Decision.ALLOW_EXTERNAL;
    }

    /** True when {@link #classify} would return {@link Decision#ALLOW_IN_APP}. */
    public static boolean isAllowedInApp(String url, Set<String> inAppTrustedHosts) {
        return classify(url, inAppTrustedHosts) == Decision.ALLOW_IN_APP;
    }

    /** Lower-cased host or empty string. Convenience for callers that just need the host. */
    public static String hostOf(String url) {
        if (url == null) return "";
        try {
            String host = new URI(url.trim()).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
