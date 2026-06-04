package com.baghdad.edulife.features.certificates.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.ViewHolder> {

    public interface OnDownloadClick {
        void onDownload(CertificateSummary cert);
    }

    public interface OnItemClick {
        void onOpen(CertificateSummary cert);
    }

    private final List<CertificateSummary> items = new ArrayList<>();
    private final OnDownloadClick downloadListener;
    private final OnItemClick itemListener;

    private static final SimpleDateFormat ISO_PARSE =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);

    public CertificateAdapter(OnDownloadClick downloadListener, OnItemClick itemListener) {
        this.downloadListener = downloadListener;
        this.itemListener = itemListener;
    }

    public void setItems(List<CertificateSummary> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_certificate, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CertificateSummary cert = items.get(position);

        holder.courseTitle.setText(cert.courseTitle != null ? cert.courseTitle : "Course");
        holder.certNumber.setText(cert.certificateNumber != null ? cert.certificateNumber : "");

        if (cert.issuedAt != null) {
            try {
                // Trim to date portion before parsing (ISO-8601 e.g. "2026-05-30T10:00:00Z")
                String datePart = cert.issuedAt.length() >= 10
                        ? cert.issuedAt.substring(0, 10) : cert.issuedAt;
                Date parsed = ISO_PARSE.parse(datePart);
                holder.issuedDate.setText(parsed != null ? DATE_FMT.format(parsed) : datePart);
            } catch (ParseException e) {
                holder.issuedDate.setText(cert.issuedAt);
            }
        }

        holder.downloadButton.setOnClickListener(v -> downloadListener.onDownload(cert));
        holder.itemView.setOnClickListener(v -> itemListener.onOpen(cert));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView courseTitle;
        final TextView certNumber;
        final TextView issuedDate;
        final LinearLayout downloadButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseTitle    = itemView.findViewById(R.id.certCourseTitle);
            certNumber     = itemView.findViewById(R.id.certNumber);
            issuedDate     = itemView.findViewById(R.id.certIssuedDate);
            downloadButton = itemView.findViewById(R.id.certDownloadButton);
        }
    }
}
