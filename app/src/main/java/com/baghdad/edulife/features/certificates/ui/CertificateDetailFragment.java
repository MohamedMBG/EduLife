package com.baghdad.edulife.features.certificates.ui;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.BuildConfig;
import com.baghdad.edulife.R;
import com.baghdad.edulife.features.certificates.model.CertificateDetail;
import com.baghdad.edulife.features.certificates.viewmodel.CertificateDetailViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CertificateDetailFragment extends Fragment {

    private static final SimpleDateFormat ISO_PARSE =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);

    private CertificateDetailViewModel viewModel;
    private BroadcastReceiver downloadReceiver;
    private long pendingDownloadId = -1;

    public CertificateDetailFragment() {
        super(R.layout.fragment_certificate_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CertificateDetailViewModel.class);

        view.findViewById(R.id.backButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        String certId = getArguments() != null ? getArguments().getString("certId", "") : "";

        ProgressBar progress = view.findViewById(R.id.certDetailProgress);
        TextView errorView = view.findViewById(R.id.certDetailError);
        View content = view.findViewById(R.id.certDetailContent);

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err == null || err.isBlank()) {
                errorView.setVisibility(View.GONE);
                return;
            }
            errorView.setVisibility(View.VISIBLE);
            errorView.setText(err);
            content.setVisibility(View.GONE);
        });

        viewModel.getDetail().observe(getViewLifecycleOwner(), cert -> {
            if (cert == null) return;
            errorView.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            bindDetail(view, cert);
        });

        viewModel.load(certId);
    }

    private void bindDetail(View view, CertificateDetail cert) {
        ((TextView) view.findViewById(R.id.certDetailCourseTitle))
                .setText(cert.courseTitle != null ? cert.courseTitle : "");
        ((TextView) view.findViewById(R.id.certDetailNumber))
                .setText(cert.certificateNumber != null ? cert.certificateNumber : "");
        ((TextView) view.findViewById(R.id.certDetailStudentName))
                .setText(cert.studentName != null ? cert.studentName : "—");
        ((TextView) view.findViewById(R.id.certDetailIssuer))
                .setText(cert.issuerName != null ? cert.issuerName : "EduLife");
        ((TextView) view.findViewById(R.id.certDetailHash))
                .setText(cert.verificationHash != null ? cert.verificationHash : "—");

        TextView dateView = view.findViewById(R.id.certDetailIssuedDate);
        if (cert.issuedAt != null) {
            try {
                String datePart = cert.issuedAt.length() >= 10
                        ? cert.issuedAt.substring(0, 10) : cert.issuedAt;
                Date parsed = ISO_PARSE.parse(datePart);
                dateView.setText(parsed != null ? DATE_FMT.format(parsed) : datePart);
            } catch (ParseException e) {
                dateView.setText(cert.issuedAt);
            }
        }

        view.findViewById(R.id.certDownloadBtn).setOnClickListener(v -> downloadPdf(cert));
        view.findViewById(R.id.certShareBtn).setOnClickListener(v -> shareCert(cert));
    }

    private void downloadPdf(CertificateDetail cert) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(false).addOnSuccessListener(result -> {
            String token = result.getToken();
            if (token == null) return;

            String baseUrl = BuildConfig.API_BASE_URL;
            if (!baseUrl.endsWith("/")) baseUrl += "/";
            String url = baseUrl + "certificates/" + cert.id + "/download";
            String fileName = "certificate-" + cert.certificateNumber + ".pdf";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(getString(R.string.cert_download_notification_title, cert.courseTitle))
                    .setDescription(cert.certificateNumber)
                    .addRequestHeader("Authorization", "Bearer " + token)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setMimeType("application/pdf");

            DownloadManager dm = (DownloadManager) requireContext()
                    .getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = dm.enqueue(request);
            registerDownloadReceiver(dm);

            Toast.makeText(requireContext(), R.string.cert_download_started, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), R.string.cert_download_auth_error, Toast.LENGTH_SHORT).show()
        );
    }

    private void shareCert(CertificateDetail cert) {
        String date = cert.issuedAt != null ? formatDate(cert.issuedAt) : "";
        String text = getString(R.string.cert_share_text,
                cert.courseTitle != null ? cert.courseTitle : "",
                cert.certificateNumber != null ? cert.certificateNumber : "",
                date,
                cert.verificationHash != null ? cert.verificationHash : "");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent,
                getString(R.string.cert_detail_share)));
    }

    private String formatDate(String issuedAt) {
        try {
            String datePart = issuedAt.length() >= 10 ? issuedAt.substring(0, 10) : issuedAt;
            Date parsed = ISO_PARSE.parse(datePart);
            return parsed != null ? DATE_FMT.format(parsed) : datePart;
        } catch (ParseException e) {
            return issuedAt;
        }
    }

    private void registerDownloadReceiver(DownloadManager dm) {
        if (downloadReceiver != null) {
            requireContext().unregisterReceiver(downloadReceiver);
        }
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != pendingDownloadId) return;
                Uri uri = dm.getUriForDownloadedFile(id);
                if (uri == null) return;
                Intent open = new Intent(Intent.ACTION_VIEW);
                open.setDataAndType(uri, "application/pdf");
                open.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(open);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.cert_no_pdf_viewer, Toast.LENGTH_LONG).show();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(requireContext(), downloadReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (downloadReceiver != null) {
            requireContext().unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }
}
