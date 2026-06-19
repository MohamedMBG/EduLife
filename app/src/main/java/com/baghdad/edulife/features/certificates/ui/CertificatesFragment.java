package com.baghdad.edulife.features.certificates.ui;

import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.certificates.data.CertificateDownloader;
import com.baghdad.edulife.features.certificates.data.CertificatePdfIntents;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;
import com.baghdad.edulife.features.certificates.viewmodel.CertificateViewModel;

import java.io.File;

public class CertificatesFragment extends Fragment {

    private CertificateViewModel viewModel;
    private CertificateAdapter adapter;

    public CertificatesFragment() {
        super(R.layout.fragment_certificates);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CertificateViewModel.class);

        View header = view.findViewById(R.id.certsHeaderLayout);
        final int origTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), origTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        view.findViewById(R.id.certsBackButton).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        RecyclerView recycler = view.findViewById(R.id.certsRecycler);
        adapter = new CertificateAdapter(this::downloadCertificate, this::openCertDetail);
        int spanCount = getResources().getInteger(R.integer.cert_grid_span);
        if (spanCount > 1) {
            recycler.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount));
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        recycler.setAdapter(adapter);

        ProgressBar progress = view.findViewById(R.id.certsProgress);
        View        emptyView = view.findViewById(R.id.certsEmpty);
        TextView    errorView = view.findViewById(R.id.certsError);

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getCertificates().observe(getViewLifecycleOwner(), certs -> {
            adapter.setItems(certs);
            boolean empty = certs == null || certs.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                errorView.setVisibility(View.VISIBLE);
                errorView.setText(err);
                recycler.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
            } else {
                errorView.setVisibility(View.GONE);
            }
        });

        viewModel.load();
    }

    private void openCertDetail(CertificateSummary cert) {
        Bundle args = new Bundle();
        args.putString("certId", cert.id != null ? cert.id : "");
        Navigation.findNavController(requireView())
                .navigate(R.id.action_certificatesFragment_to_certificateDetailFragment, args);
    }

    private void downloadCertificate(CertificateSummary cert) {
        if (cert == null || cert.id == null || cert.id.isBlank()) return;
        if (!isAdded()) return;

        Toast.makeText(requireContext(), R.string.cert_download_started, Toast.LENGTH_SHORT).show();
        // Goes through the same OkHttp pipeline as every other API call, so a stale Firebase
        // ID token is refreshed once by FirebaseTokenAuthenticator before the body is streamed
        // to app-private storage. DownloadManager bypassed both behaviors.
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
}
