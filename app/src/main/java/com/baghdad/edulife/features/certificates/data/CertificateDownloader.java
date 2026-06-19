package com.baghdad.edulife.features.certificates.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Downloads a certificate PDF through the authenticated Retrofit pipeline and writes it to
 * app-private storage (filesDir/certificates/). Replaces the previous DownloadManager flow,
 * which bypassed {@code FirebaseTokenAuthenticator}'s 401-refresh logic and wrote the PDF
 * (with learner name + cert number + verification hash) to the public Downloads folder.
 *
 * The caller is expected to hand the resulting File to FileProvider for an explicit
 * VIEW/SEND intent — keeping the file out of MediaStore until the learner asks to export it.
 */
public final class CertificateDownloader {

    public interface Callback {
        /** Successful download. The file lives in app-private storage. */
        @MainThread void onDownloaded(@NonNull File pdf);
        /** Network/IO failure (no response, write failed, etc.). */
        @MainThread void onNetworkError();
        /** Server returned a non-2xx response (including 401 after the auto-retry). */
        @MainThread void onServerError(int httpCode);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CertificateDownloader() {
    }

    public static void download(@NonNull Context context,
                                @NonNull String certificateId,
                                @NonNull String certificateNumber,
                                @NonNull Callback callback) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = api.downloadCertificatePdf(certificateId);
        Context appContext = context.getApplicationContext();
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    int code = response.code();
                    if (response.body() != null) response.body().close();
                    MAIN.post(() -> callback.onServerError(code));
                    return;
                }
                try {
                    File pdf = writeToPrivateStorage(appContext, certificateNumber, response.body());
                    MAIN.post(() -> callback.onDownloaded(pdf));
                } catch (IOException e) {
                    MAIN.post(callback::onNetworkError);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                MAIN.post(callback::onNetworkError);
            }
        });
    }

    private static File writeToPrivateStorage(Context context, String certificateNumber, ResponseBody body)
            throws IOException {
        File dir = new File(context.getFilesDir(), "certificates");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create private certificates directory");
        }
        File out = new File(dir, sanitizeFileName(certificateNumber) + ".pdf");
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

    /**
     * Certificate numbers come from the backend ("EDU-2026-…"), but defensive sanitization
     * prevents a future format change from yielding a name that escapes the certificates dir
     * (path traversal) or contains characters that confuse downstream file viewers.
     */
    static String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) return "certificate";
        String stripped = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_");
        if (stripped.isEmpty()) return "certificate";
        // Avoid ".." / "." which would resolve outside the directory.
        if (stripped.equals(".") || stripped.equals("..")) return "certificate";
        return "certificate-" + stripped;
    }
}
