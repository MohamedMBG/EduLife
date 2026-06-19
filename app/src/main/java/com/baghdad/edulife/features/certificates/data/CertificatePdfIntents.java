package com.baghdad.edulife.features.certificates.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Builds the FileProvider-backed Intents that show or share a downloaded certificate PDF.
 *
 * The PDF lives in app-private storage; consumers must receive a per-grant content:// URI
 * (FLAG_GRANT_READ_URI_PERMISSION) instead of a raw file path so no other app can read the
 * file unless this app explicitly hands it over.
 */
public final class CertificatePdfIntents {

    private CertificatePdfIntents() {
    }

    /** Authority matches the &lt;provider&gt; declared in AndroidManifest.xml. */
    private static String authority(Context context) {
        return context.getPackageName() + ".fileprovider";
    }

    public static Intent viewIntent(@NonNull Context context, @NonNull File pdf) {
        Uri uri = FileProvider.getUriForFile(context, authority(context), pdf);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent shareIntent(@NonNull Context context, @NonNull File pdf) {
        Uri uri = FileProvider.getUriForFile(context, authority(context), pdf);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }
}
