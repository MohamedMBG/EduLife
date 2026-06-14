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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.BuildConfig;
import com.baghdad.edulife.R;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;
import com.baghdad.edulife.features.certificates.viewmodel.CertificateViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CertificatesFragment extends Fragment {

    private CertificateViewModel viewModel;
    private CertificateAdapter adapter;
    private BroadcastReceiver downloadReceiver;
    private long pendingDownloadId = -1;

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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(false).addOnSuccessListener(result -> {
            String token = result.getToken();
            if (token == null) return;

            String baseUrl = BuildConfig.API_BASE_URL;
            if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
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

            // Register only if the Fragment is currently started; otherwise onStart will
            // pick this up when the user returns. Avoids leaking the receiver while paused.
            ensureReceiverRegistered();

            Toast.makeText(requireContext(),
                    R.string.cert_download_started, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
            Toast.makeText(requireContext(),
                    R.string.cert_download_auth_error, Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Registers the download-complete receiver if the Fragment is started and a download
     * is actually pending. Safe to call multiple times — second call is a no-op.
     *
     * The receiver is rebuilt rather than reused because it captures the DownloadManager
     * instance; that capture must come from the current resumed context.
     */
    private void ensureReceiverRegistered() {
        if (downloadReceiver != null) return;
        if (pendingDownloadId == -1L) return;
        if (!isAdded()) return;

        DownloadManager dm = (DownloadManager) requireContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != pendingDownloadId) return;
                pendingDownloadId = -1L;
                Uri uri = dm.getUriForDownloadedFile(id);
                if (uri == null) return;
                Intent open = new Intent(Intent.ACTION_VIEW);
                open.setDataAndType(uri, "application/pdf");
                open.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(open);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context,
                            R.string.cert_no_pdf_viewer, Toast.LENGTH_LONG).show();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(requireContext(), downloadReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Re-register if a download was already enqueued before the Fragment was stopped so
        // the auto-open intent still fires once the user returns to the screen.
        ensureReceiverRegistered();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unregister whenever the view is not visible. DownloadManager's own system notification
        // continues to surface completion to the user while the receiver is detached.
        if (downloadReceiver != null) {
            requireContext().unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }
}
