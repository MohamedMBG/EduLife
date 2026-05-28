package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.courses.model.LessonDetail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LessonPlayerFragment extends Fragment {

    private LinearLayout viewerContainer;
    private WebView viewerWebView;
    private ProgressBar viewerProgress;
    private TextView viewerTitle;
    private Button markCompleteButton;

    private String courseId = "";
    private String lessonId = "";
    private boolean isPreview;
    private boolean viewerOpened;
    private boolean completionMarked;
    private LessonDetail cachedDetail;

    private OnBackPressedCallback viewerBackCallback;

    public LessonPlayerFragment() {
        super(R.layout.fragment_lesson_player);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args           = getArguments();
        courseId              = args != null ? args.getString("courseId", "")       : "";
        lessonId              = args != null ? args.getString("lessonId", "")       : "";
        String lessonTitle    = args != null ? args.getString("lessonTitle", "")    : "";
        String lessonSummary  = args != null ? args.getString("lessonSummary", "")  : "";
        String lessonType     = args != null ? args.getString("lessonType", "")     : "";
        int    durationMin    = args != null ? args.getInt("durationMinutes", 0)    : 0;
        isPreview             = args != null && args.getBoolean("isPreview", false);
        String sectionTitle   = args != null ? args.getString("sectionTitle", "")   : "";
        int    orderInSection = args != null ? args.getInt("orderInSection", 1)     : 1;

        ((TextView) view.findViewById(R.id.lessonTitle)).setText(lessonTitle);
        ((TextView) view.findViewById(R.id.lessonSectionContext))
                .setText(sectionTitle.isBlank() ? "" : "From: " + sectionTitle);
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(lessonSummary);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText(normalizeLabel(lessonType));
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText("Lesson " + orderInSection);
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText(durationMin + " min");

        view.findViewById(R.id.lessonPreviewBadge)
                .setVisibility(isPreview ? View.VISIBLE : View.GONE);

        view.findViewById(R.id.lessonBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        viewerContainer = view.findViewById(R.id.lessonViewerContainer);
        viewerWebView   = view.findViewById(R.id.lessonViewerWebView);
        viewerProgress  = view.findViewById(R.id.lessonViewerProgress);
        viewerTitle     = view.findViewById(R.id.lessonViewerTitle);
        viewerTitle.setText(lessonTitle);

        configureWebView();

        ImageButton viewerCloseButton = view.findViewById(R.id.lessonViewerCloseButton);
        viewerCloseButton.setOnClickListener(v -> closeInAppViewer());

        view.findViewById(R.id.playerPlayButton).setOnClickListener(v -> openInAppViewer());

        view.findViewById(R.id.lessonPrevButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to previous lesson — coming next sprint!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.lessonNextButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to next lesson — coming next sprint!", Toast.LENGTH_SHORT).show());

        markCompleteButton = view.findViewById(R.id.lessonMarkCompleteButton);

        // Hide "Mark as Done" for preview lessons — no enrollment = no progress tracking
        if (isPreview || courseId.isBlank() || lessonId.isBlank()) {
            markCompleteButton.setVisibility(View.GONE);
        } else {
            markCompleteButton.setOnClickListener(v -> {
                markCompleteButton.setEnabled(false);
                markCompleteButton.setText(R.string.lesson_player_completed);
                markLessonCompleteIdempotent(false);
            });
        }

        installViewerBackHandler();
    }

    @SuppressWarnings("deprecation")
    private void configureWebView() {
        WebSettings settings = viewerWebView.getSettings();
        // JS is required for Google Docs PDF preview and most embedded video players.
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        // File access stays off so the WebView cannot read app-private files via a malicious URL.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(true);

        viewerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep all navigation inside the in-app viewer; only http(s) targets are honoured.
                String scheme = request.getUrl().getScheme();
                return scheme == null || !(scheme.equals("http") || scheme.equals("https"));
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!isAdded()) return;
                if (request.isForMainFrame()) {
                    Toast.makeText(requireContext(),
                            R.string.lesson_viewer_load_error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewerWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (viewerProgress == null) return;
                viewerProgress.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void installViewerBackHandler() {
        viewerBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                closeInAppViewer();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), viewerBackCallback);
    }

    private void openInAppViewer() {
        if (cachedDetail != null) {
            loadDetailInWebView(cachedDetail);
            return;
        }
        if (courseId.isBlank() || lessonId.isBlank()) {
            Toast.makeText(requireContext(),
                    R.string.lesson_viewer_no_content, Toast.LENGTH_SHORT).show();
            return;
        }

        viewerProgress.setVisibility(View.VISIBLE);
        ApiClient.getClient()
                .create(ApiService.class)
                .getLessonDetail(courseId, lessonId)
                .enqueue(new Callback<LessonDetail>() {
                    @Override
                    public void onResponse(@NonNull Call<LessonDetail> call,
                                           @NonNull Response<LessonDetail> response) {
                        if (!isAdded()) return;
                        viewerProgress.setVisibility(View.GONE);
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(requireContext(),
                                    R.string.lesson_viewer_load_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        cachedDetail = response.body();
                        loadDetailInWebView(cachedDetail);
                    }

                    @Override
                    public void onFailure(@NonNull Call<LessonDetail> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        viewerProgress.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                R.string.lesson_viewer_load_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadDetailInWebView(LessonDetail detail) {
        String url = detail.contentUrl == null ? "" : detail.contentUrl.trim();
        String body = detail.contentBody == null ? "" : detail.contentBody.trim();
        if (url.isEmpty() && body.isEmpty()) {
            Toast.makeText(requireContext(),
                    R.string.lesson_viewer_no_content, Toast.LENGTH_SHORT).show();
            return;
        }

        viewerContainer.setVisibility(View.VISIBLE);
        viewerOpened = true;
        if (viewerBackCallback != null) {
            viewerBackCallback.setEnabled(true);
        }

        if (!url.isEmpty()) {
            viewerWebView.loadUrl(resolveViewerUrl(detail.lessonType, url));
            return;
        }

        // Fallback: render plain text / markdown content as HTML inside the WebView so it never
        // leaks to the system browser.
        String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>"
                + "<style>body{font-family:sans-serif;padding:16px;line-height:1.6;color:#222}</style>"
                + "</head><body>" + body.replace("\n", "<br/>") + "</body></html>";
        viewerWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private static String resolveViewerUrl(String lessonType, String contentUrl) {
        String type = lessonType == null ? "" : lessonType.toUpperCase(Locale.ROOT);
        String url  = contentUrl.toLowerCase(Locale.ROOT);
        boolean looksLikePdf = type.equals("PDF") || url.endsWith(".pdf");
        if (looksLikePdf) {
            // Google Docs viewer embeds the PDF inside the WebView so we keep the user in-app
            // instead of handing off to a system PDF reader.
            String encoded = URLEncoder.encode(contentUrl, StandardCharsets.UTF_8);
            return "https://docs.google.com/gview?embedded=true&url=" + encoded;
        }
        return contentUrl;
    }

    private void closeInAppViewer() {
        if (!viewerOpened) return;
        viewerOpened = false;

        // Stop any background playback before hiding the viewer so audio cannot keep playing
        // while the learner is on the summary screen.
        viewerWebView.loadUrl("about:blank");
        viewerContainer.setVisibility(View.GONE);
        if (viewerBackCallback != null) {
            viewerBackCallback.setEnabled(false);
        }

        // Closing the viewer counts as engaging with the content for non-preview enrolled lessons,
        // so progress is reported even when the learner forgets to tap "Mark as Done".
        markLessonCompleteIdempotent(true);
    }

    private void markLessonCompleteIdempotent(boolean fromViewerClose) {
        if (isPreview || courseId.isBlank() || lessonId.isBlank()) return;
        if (completionMarked) return;
        completionMarked = true;

        ApiClient.getClient()
                .create(ApiService.class)
                .markLessonComplete(courseId, lessonId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            if (markCompleteButton != null) {
                                markCompleteButton.setEnabled(false);
                                markCompleteButton.setText(R.string.lesson_player_completed);
                            }
                            return;
                        }
                        completionMarked = false;
                        if (fromViewerClose) {
                            // Silent failure when triggered by close so we don't surprise the
                            // learner with a toast they didn't ask for.
                            return;
                        }
                        if (markCompleteButton != null) {
                            markCompleteButton.setEnabled(true);
                            markCompleteButton.setText(R.string.lesson_player_mark_complete);
                        }
                        if (response.code() == 403) {
                            Toast.makeText(requireContext(),
                                    "You need to enroll in this course first.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Could not save progress. Try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        completionMarked = false;
                        if (fromViewerClose) return;
                        if (markCompleteButton != null) {
                            markCompleteButton.setEnabled(true);
                            markCompleteButton.setText(R.string.lesson_player_mark_complete);
                        }
                        Toast.makeText(requireContext(),
                                "Network error. Check your connection.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (viewerWebView != null) viewerWebView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewerWebView != null) viewerWebView.onResume();
    }

    @Override
    public void onDestroyView() {
        if (viewerWebView != null) {
            viewerWebView.stopLoading();
            viewerWebView.loadUrl("about:blank");
            viewerWebView.destroy();
            viewerWebView = null;
        }
        super.onDestroyView();
    }

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

}
