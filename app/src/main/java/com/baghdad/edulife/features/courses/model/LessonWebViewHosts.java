package com.baghdad.edulife.features.courses.model;

import com.baghdad.edulife.core.web.UrlSecurityPolicy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Curated allowlist of hosts the lesson player is willing to render inside its own WebView.
 *
 * Anything not in this set is either opened in the system browser (HTTPS) or rejected
 * outright (anything else). The list is deliberately small: the EduLife backend hosts video
 * and PDF lessons, and YouTube/Vimeo cover the embedded video lessons authored by teachers.
 * Adding a host here means trusting it to run scripts inside the in-app WebView, so the
 * approval bar is high — the rule of thumb is "would I be comfortable showing an attacker's
 * page from this host inside my app?".
 */
public final class LessonWebViewHosts {

    /**
     * Hard-coded providers used by teacher-authored video embeds. They are added explicitly
     * (not derived) so a future content URL on a random domain can't silently become "trusted".
     */
    private static final String[] STATIC_VIDEO_PROVIDERS = new String[]{
            "www.youtube.com",
            "youtube.com",
            "m.youtube.com",
            "youtu.be",
            "www.youtube-nocookie.com",
            "player.vimeo.com",
            "vimeo.com"
    };

    private LessonWebViewHosts() {
    }

    /**
     * Builds the in-app allowlist for the given API base URL. The backend host is added so
     * authenticated PDFs/articles served by EduLife itself can render inline; the static video
     * providers are added so embedded video lessons keep working.
     */
    public static Set<String> forApiBaseUrl(String apiBaseUrl) {
        LinkedHashSet<String> hosts = new LinkedHashSet<>();
        String backendHost = extractHost(apiBaseUrl);
        if (!backendHost.isEmpty()) hosts.add(backendHost);
        for (String provider : STATIC_VIDEO_PROVIDERS) hosts.add(provider);
        return UrlSecurityPolicy.trustedHosts(hosts.toArray(new String[0]));
    }

    private static String extractHost(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            String host = new URI(url.trim()).getHost();
            return host == null ? "" : host.toLowerCase();
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
