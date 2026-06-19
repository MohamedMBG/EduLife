package com.baghdad.edulife.features.certificates.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.certificates.data.CertificateDownloader;
import com.baghdad.edulife.features.certificates.data.CertificatePdfIntents;
import com.baghdad.edulife.features.certificates.model.CertificateDetail;
import com.baghdad.edulife.features.certificates.viewmodel.CertificateDetailViewModel;

import java.io.File;
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
                .setText(cert.learnerName != null ? cert.learnerName : "");
        ((TextView) view.findViewById(R.id.certDetailIssuer))
                .setText(cert.teacherName != null ? cert.teacherName : "");
        ((TextView) view.findViewById(R.id.certDetailHash))
                .setText(cert.verificationHash != null ? cert.verificationHash : "");
        // This sentence mirrors the backend certificate snapshot so the on-screen view
        // and downloaded PDF describe the same issued credential.
        ((TextView) view.findViewById(R.id.certDetailCompletionText))
                .setText(getString(R.string.cert_detail_completion_sentence,
                        cert.learnerName != null ? cert.learnerName : "",
                        cert.courseTitle != null ? cert.courseTitle : "",
                        cert.courseLevel != null ? cert.courseLevel : "",
                        cert.teacherName != null ? cert.teacherName : ""));

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
        if (cert == null || cert.id == null || cert.id.isBlank()) return;
        Toast.makeText(requireContext(), R.string.cert_download_started, Toast.LENGTH_SHORT).show();
        CertificateDownloader.download(
                requireContext(),
                cert.id,
                cert.certificateNumber != null ? cert.certificateNumber : cert.id,
                new CertificateDownloader.Callback() {
                    @Override
                    public void onDownloaded(@NonNull File pdf) {
                        if (!isAdded()) return;
                        try {
                            startActivity(CertificatePdfIntents.viewIntent(requireContext(), pdf));
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(requireContext(),
                                    R.string.cert_no_pdf_viewer, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onNetworkError() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                R.string.cert_download_network_error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onServerError(int httpCode) {
                        if (!isAdded()) return;
                        int msg = httpCode == 401
                                ? R.string.cert_download_auth_error
                                : R.string.cert_download_network_error;
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void shareCert(CertificateDetail cert) {
        if (cert == null || cert.id == null || cert.id.isBlank()) return;
        Toast.makeText(requireContext(), R.string.cert_share_preparing, Toast.LENGTH_SHORT).show();
        // Share is gated behind a fresh download to private storage so the resulting Intent can
        // attach the PDF via FileProvider — explicit per-grant access only, no public file path.
        CertificateDownloader.download(
                requireContext(),
                cert.id,
                cert.certificateNumber != null ? cert.certificateNumber : cert.id,
                new CertificateDownloader.Callback() {
                    @Override
                    public void onDownloaded(@NonNull File pdf) {
                        if (!isAdded()) return;
                        Intent share = CertificatePdfIntents.shareIntent(requireContext(), pdf);
                        String summary = buildShareSummary(cert);
                        if (!summary.isEmpty()) {
                            share.putExtra(Intent.EXTRA_TEXT, summary);
                        }
                        try {
                            startActivity(Intent.createChooser(share,
                                    getString(R.string.cert_detail_share)));
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(requireContext(),
                                    R.string.cert_no_pdf_viewer, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onNetworkError() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                R.string.cert_download_network_error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onServerError(int httpCode) {
                        if (!isAdded()) return;
                        int msg = httpCode == 401
                                ? R.string.cert_download_auth_error
                                : R.string.cert_download_network_error;
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String buildShareSummary(CertificateDetail cert) {
        String date = cert.issuedAt != null ? formatDate(cert.issuedAt) : "";
        return getString(R.string.cert_share_text,
                cert.learnerName != null ? cert.learnerName : "",
                cert.courseTitle != null ? cert.courseTitle : "",
                cert.courseLevel != null ? cert.courseLevel : "",
                cert.teacherName != null ? cert.teacherName : "",
                cert.certificateNumber != null ? cert.certificateNumber : "",
                date,
                cert.verificationHash != null ? cert.verificationHash : "");
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
}
