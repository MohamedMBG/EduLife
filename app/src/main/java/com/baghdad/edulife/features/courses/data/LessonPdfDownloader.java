package com.baghdad.edulife.features.courses.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.baghdad.edulife.BuildConfig;
import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.web.UrlSecurityPolicy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads a lesson PDF to app-private cache so it can be opened via FileProvider instead of
 * being rendered through the Google Docs Viewer (which leaked the lesson URL to a third
 * party — flagged by the 2026-06 OWASP audit).
 *
 * Authenticated client (with Firebase bearer) is only reused for PDFs hosted on the EduLife
 * backend. For any other host, an unauthenticated singleton OkHttp client is used so the
 * learner's token never leaves the EduLife origin. URLs that don't pass the global URL
 * security policy (HTTPS + sane scheme) are rejected before any network call.
 */
public final class LessonPdfDownloader {

    public interface Callback {
        @MainThread void onDownloaded(@NonNull File pdf);
        @MainThread void onUnsafeUrl();
        @MainThread void onNetworkError();
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static OkHttpClient unauthenticatedClient;

    private LessonPdfDownloader() {
    }

    public static void download(@NonNull Context context,
                                @NonNull String url,
                                @NonNull String fileNameHint,
                                @NonNull Callback callback) {
        // Reject anything that isn't HTTPS up-front. The decision returns ALLOW_EXTERNAL for
        // HTTPS regardless of host; we don't need ALLOW_IN_APP here because the file is
        // downloaded, not rendered inside the WebView.
        if (UrlSecurityPolicy.classify(url, null) == UrlSecurityPolicy.Decision.BLOCK) {
            MAIN.post(callback::onUnsafeUrl);
            return;
        }

        Context appContext = context.getApplicationContext();
        OkHttpClient client = isBackendHost(url) ? ApiClient.authenticatedClient() : unauthenticatedClient();
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(callback::onNetworkError);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response autoClose = response) {
                    if (!autoClose.isSuccessful() || autoClose.body() == null) {
                        MAIN.post(callback::onNetworkError);
                        return;
                    }
                    try {
                        File pdf = writeToCache(appContext, fileNameHint, autoClose.body());
                        MAIN.post(() -> callback.onDownloaded(pdf));
                    } catch (IOException e) {
                        MAIN.post(callback::onNetworkError);
                    }
                }
            }
        });
    }

    private static synchronized OkHttpClient unauthenticatedClient() {
        if (unauthenticatedClient == null) {
            unauthenticatedClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(45, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
        return unauthenticatedClient;
    }

    static boolean isBackendHost(String url) {
        String backendHost = hostOf(BuildConfig.API_BASE_URL);
        String requestHost = hostOf(url);
        return !backendHost.isEmpty() && backendHost.equals(requestHost);
    }

    private static String hostOf(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            String host = new URI(url.trim()).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private static File writeToCache(Context context, String fileNameHint, ResponseBody body)
            throws IOException {
        File dir = new File(context.getCacheDir(), "lesson_downloads");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create lesson_downloads directory");
        }
        File out = new File(dir, sanitize(fileNameHint) + ".pdf");
        try (InputStream in = body.byteStream();
             FileOutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[8 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        return out;
    }

    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "lesson";
        String stripped = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_");
        if (stripped.isEmpty() || stripped.equals(".") || stripped.equals("..")) {
            return "lesson";
        }
        if (stripped.endsWith(".pdf")) stripped = stripped.substring(0, stripped.length() - 4);
        return "lesson-" + stripped;
    }
}
