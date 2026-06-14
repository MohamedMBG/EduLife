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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.gamification.model.XpEvent;
import com.baghdad.edulife.features.gamification.ui.XpToastHelper;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseDetailUiState;
import com.baghdad.edulife.features.courses.model.CourseSection;
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.LessonSummary;
import com.baghdad.edulife.features.courses.viewmodel.CourseDetailViewModel;
import com.baghdad.edulife.features.courses.viewmodel.LessonPlayerViewModel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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
    private boolean xpAwarded;

    private LessonPlayerViewModel viewModel;
    private OnBackPressedCallback viewerBackCallback;

    public LessonPlayerFragment() {
        super(R.layout.fragment_lesson_player);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LessonPlayerViewModel.class);

        // Nav graph always supplies the lesson bundle; requireArguments turns a wiring bug into
        // an ISE rather than the ten silent blank-fallback paths below.
        Bundle args           = requireArguments();
        courseId              = args.getString("courseId", "");
        lessonId              = args.getString("lessonId", "");
        String lessonTitle    = args.getString("lessonTitle", "");
        String lessonSummary  = args.getString("lessonSummary", "");
        String lessonType     = args.getString("lessonType", "");
        int    durationMin    = args.getInt("durationMinutes", 0);
        isPreview             = args.getBoolean("isPreview", false);
        String sectionTitle   = args.getString("sectionTitle", "");
        int    orderInSection = args.getInt("orderInSection", 1);

        ((TextView) view.findViewById(R.id.lessonTitle)).setText(lessonTitle);
        ((TextView) view.findViewById(R.id.lessonSectionContext))
                .setText(sectionTitle.isBlank() ? "" : getString(R.string.lesson_player_section_from, sectionTitle));
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(lessonSummary);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText(normalizeLabel(lessonType));
        ((TextView) view.findViewById(R.id.lessonOrderText))
                .setText(getString(R.string.lesson_player_order, orderInSection));
        ((TextView) view.findViewById(R.id.lessonDurationText))
                .setText(getString(R.string.lesson_player_duration_minutes, durationMin));

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
                Navigation.findNavController(view).popBackStack());

        setupNextLessonButton(view);

        markCompleteButton = view.findViewById(R.id.lessonMarkCompleteButton);

        // Hide "Mark as Done" for preview lessons — no enrollment = no progress tracking.
        if (isPreview || courseId.isBlank() || lessonId.isBlank()) {
            markCompleteButton.setVisibility(View.GONE);
        } else {
            markCompleteButton.setOnClickListener(v -> {
                markCompleteButton.setEnabled(false);
                markCompleteButton.setText(R.string.lesson_player_completed);
                viewModel.markComplete(courseId, lessonId);
            });
        }

        observeViewModel();
        installViewerBackHandler();
    }

    private void observeViewModel() {
        viewModel.detail.observe(getViewLifecycleOwner(), detail -> {
            if (detail == null) return;
            if (viewerOpened) loadDetailInWebView(detail);
        });

        viewModel.detailLoading.observe(getViewLifecycleOwner(), loading -> {
            if (viewerProgress == null) return;
            viewerProgress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.detailError.observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isBlank()) return;
            Toast.makeText(requireContext(),
                    R.string.lesson_viewer_load_error, Toast.LENGTH_SHORT).show();
        });

        viewModel.completed.observe(getViewLifecycleOwner(), completed -> {
            if (!Boolean.TRUE.equals(completed)) return;
            if (markCompleteButton != null) {
                markCompleteButton.setEnabled(false);
                markCompleteButton.setText(R.string.lesson_player_completed);
            }
            if (!isPreview && !courseId.isBlank() && !lessonId.isBlank() && !xpAwarded) {
                xpAwarded = true;
                XpToastHelper.award(requireContext(), XpEvent.LESSON_COMPLETE);
            }
        });

        viewModel.completionError.observe(getViewLifecycleOwner(), reason -> {
            if (reason == null) return;
            // Roll back the optimistic disabled / completed label so the learner can retry.
            if (markCompleteButton != null) {
                markCompleteButton.setEnabled(true);
                markCompleteButton.setText(R.string.lesson_player_mark_complete);
            }
            int msg;
            switch (reason) {
                case NOT_ENROLLED:
                    msg = R.string.lesson_player_complete_not_enrolled;
                    break;
                case NETWORK:
                    msg = R.string.lesson_player_complete_network_error;
                    break;
                default:
                    msg = R.string.lesson_player_complete_error;
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            viewModel.clearCompletionError();
        });
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
        LessonDetail cached = viewModel.detail.getValue();
        if (cached != null) {
            viewerOpened = true;
            loadDetailInWebView(cached);
            return;
        }
        if (courseId.isBlank() || lessonId.isBlank()) {
            Toast.makeText(requireContext(),
                    R.string.lesson_viewer_no_content, Toast.LENGTH_SHORT).show();
            return;
        }
        viewerOpened = true;
        viewModel.loadLessonDetail(courseId, lessonId);
    }

    private void loadDetailInWebView(LessonDetail detail) {
        String url = detail.contentUrl == null ? "" : detail.contentUrl.trim();
        String body = detail.contentBody == null ? "" : detail.contentBody.trim();
        if (url.isEmpty() && body.isEmpty()) {
            Toast.makeText(requireContext(),
                    R.string.lesson_viewer_no_content, Toast.LENGTH_SHORT).show();
            viewerOpened = false;
            return;
        }

        viewerContainer.setVisibility(View.VISIBLE);
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
            try {
                String encoded = URLEncoder.encode(contentUrl, StandardCharsets.UTF_8.name());
                return "https://docs.google.com/gview?embedded=true&url=" + encoded;
            } catch (java.io.UnsupportedEncodingException e) {
                return contentUrl;
            }
        }
        return contentUrl;
    }

    private void closeInAppViewer() {
        if (!viewerOpened) return;
        viewerOpened = false;

        // Stop any background playback before hiding the viewer so audio cannot keep playing
        // while the learner is on the summary screen.
        if (viewerWebView != null) viewerWebView.loadUrl("about:blank");
        viewerContainer.setVisibility(View.GONE);
        if (viewerBackCallback != null) {
            viewerBackCallback.setEnabled(false);
        }

        // Closing the viewer counts as engaging with the content for non-preview enrolled
        // lessons. The ViewModel guards against double-marking via its own in-flight flag, so
        // the manual "Mark as Done" button still works without a duplicate request.
        if (!isPreview && !courseId.isBlank() && !lessonId.isBlank()) {
            viewModel.markComplete(courseId, lessonId);
        }
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

    private void setupNextLessonButton(View root) {
        Button nextBtn = root.findViewById(R.id.lessonNextButton);
        try {
            NavController nav = Navigation.findNavController(root);
            NavBackStackEntry courseEntry = nav.getBackStackEntry(R.id.courseDetailFragment);
            CourseDetailViewModel courseVm =
                    new ViewModelProvider(courseEntry).get(CourseDetailViewModel.class);
            CourseDetailUiState state = courseVm.getUiState().getValue();
            if (state == null || state.courseDetail == null) {
                nextBtn.setAlpha(0.4f);
                nextBtn.setEnabled(false);
                return;
            }
            LessonInfo next = findNextLesson(state.courseDetail);
            if (next == null) {
                nextBtn.setAlpha(0.4f);
                nextBtn.setEnabled(false);
                return;
            }
            NavController finalNav = nav;
            nextBtn.setOnClickListener(v -> {
                Bundle nextArgs = new Bundle();
                nextArgs.putString("courseId",       courseId);
                nextArgs.putString("lessonId",       next.id);
                nextArgs.putString("lessonTitle",    next.title);
                nextArgs.putString("lessonSummary",  next.summary);
                nextArgs.putString("lessonType",     next.lessonType);
                nextArgs.putInt("durationMinutes",   next.durationMinutes);
                nextArgs.putBoolean("isPreview",     next.preview);
                nextArgs.putString("sectionTitle",   next.sectionTitle);
                nextArgs.putInt("orderInSection",    next.displayOrder);
                finalNav.navigate(R.id.lessonPlayerFragment, nextArgs);
            });
        } catch (IllegalArgumentException ignored) {
            nextBtn.setAlpha(0.4f);
            nextBtn.setEnabled(false);
        }
    }

    @Nullable
    private LessonInfo findNextLesson(CourseDetail courseDetail) {
        if (courseDetail.sections == null || lessonId.isBlank()) return null;
        boolean found = false;
        for (CourseSection section : courseDetail.sections) {
            if (section.lessons == null) continue;
            for (LessonSummary lesson : section.lessons) {
                if (found && (!isPreview || lesson.preview)) {
                    return new LessonInfo(section.title != null ? section.title : "", lesson);
                }
                if (lessonId.equals(lesson.id)) found = true;
            }
        }
        return null;
    }

    private static final class LessonInfo {
        final String id, title, summary, lessonType, sectionTitle;
        final int durationMinutes, displayOrder;
        final boolean preview;

        LessonInfo(String sectionTitle, LessonSummary lesson) {
            this.id           = lesson.id != null ? lesson.id : "";
            this.title        = lesson.title != null ? lesson.title : "";
            this.summary      = lesson.summary != null ? lesson.summary : "";
            this.lessonType   = lesson.lessonType != null ? lesson.lessonType : "";
            this.sectionTitle = sectionTitle;
            this.durationMinutes = lesson.estimatedDurationMinutes;
            this.displayOrder    = lesson.displayOrder;
            this.preview         = lesson.preview;
        }
    }

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
