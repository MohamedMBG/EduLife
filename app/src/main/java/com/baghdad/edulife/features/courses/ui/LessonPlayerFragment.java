package com.baghdad.edulife.features.courses.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.baghdad.edulife.BuildConfig;
import com.baghdad.edulife.R;
import com.baghdad.edulife.core.web.UrlSecurityPolicy;
import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.data.LessonPdfDownloader;
import com.baghdad.edulife.features.gamification.data.GamificationRepository;
import com.baghdad.edulife.features.gamification.model.GamificationUiState;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseDetailUiState;
import com.baghdad.edulife.features.courses.model.CourseSection;
import com.baghdad.edulife.features.courses.model.LessonContentTypeResolver;
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.LessonSummary;
import com.baghdad.edulife.features.courses.model.LessonWebViewHosts;
import com.baghdad.edulife.features.courses.viewmodel.CourseDetailViewModel;
import com.baghdad.edulife.features.courses.viewmodel.LessonPlayerViewModel;

import java.io.File;
import java.util.Locale;
import java.util.Set;

public class LessonPlayerFragment extends Fragment {

    private LinearLayout viewerContainer;
    private WebView viewerWebView;
    private ProgressBar viewerProgress;
    private TextView viewerTitle;
    private Button markCompleteButton;

    private FrameLayout playerVideoHeader;
    private LinearLayout compactTopBar;
    private ProgressBar lessonContentLoading;
    private LinearLayout lessonTextContentArea;
    private TextView lessonTextContent;
    private LinearLayout lessonArticleCard;
    private TextView lessonArticleUrl;
    private Button lessonOpenArticleButton;
    private LinearLayout lessonResourceCard;
    private TextView lessonResourceLabel;
    private Button lessonOpenResourceButton;
    private LinearLayout lessonFallbackCard;
    private LinearLayout lessonAboutSection;

    private String courseId = "";
    private String lessonId = "";
    private String lessonType = "";
    private boolean isPreview;
    private boolean viewerOpened;
    private boolean xpAwarded;
    private boolean contentBound;

    private LessonPlayerViewModel viewModel;
    private OnBackPressedCallback viewerBackCallback;
    private final Set<String> webViewTrustedHosts =
            LessonWebViewHosts.forApiBaseUrl(BuildConfig.API_BASE_URL);

    public LessonPlayerFragment() {
        super(R.layout.fragment_lesson_player);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LessonPlayerViewModel.class);

        Bundle args           = requireArguments();
        courseId              = args.getString("courseId", "");
        lessonId              = args.getString("lessonId", "");
        String lessonTitle    = args.getString("lessonTitle", "");
        String lessonSummary  = args.getString("lessonSummary", "");
        lessonType            = args.getString("lessonType", "");
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

        // Content-type views
        playerVideoHeader    = view.findViewById(R.id.playerVideoHeader);
        compactTopBar        = view.findViewById(R.id.compactTopBar);
        lessonContentLoading = view.findViewById(R.id.lessonContentLoading);
        lessonTextContentArea = view.findViewById(R.id.lessonTextContentArea);
        lessonTextContent    = view.findViewById(R.id.lessonTextContent);
        lessonArticleCard    = view.findViewById(R.id.lessonArticleCard);
        lessonArticleUrl     = view.findViewById(R.id.lessonArticleUrl);
        lessonOpenArticleButton  = view.findViewById(R.id.lessonOpenArticleButton);
        lessonResourceCard   = view.findViewById(R.id.lessonResourceCard);
        lessonResourceLabel  = view.findViewById(R.id.lessonResourceLabel);
        lessonOpenResourceButton = view.findViewById(R.id.lessonOpenResourceButton);
        lessonFallbackCard   = view.findViewById(R.id.lessonFallbackCard);
        lessonAboutSection   = view.findViewById(R.id.lessonAboutSection);

        if (lessonSummary == null || lessonSummary.isBlank()) {
            lessonAboutSection.setVisibility(View.GONE);
        }

        // Set header based on lesson type
        boolean isVideoType = "VIDEO".equalsIgnoreCase(lessonType);
        if (isVideoType) {
            playerVideoHeader.setVisibility(View.VISIBLE);
            compactTopBar.setVisibility(View.GONE);
        } else {
            playerVideoHeader.setVisibility(View.GONE);
            compactTopBar.setVisibility(View.VISIBLE);
        }

        // Back buttons
        view.findViewById(R.id.lessonBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
        view.findViewById(R.id.lessonBackButtonCompact).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        // In-app viewer setup
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

        // Eagerly load lesson detail for all types so content renders without extra tap
        if (!courseId.isBlank() && !lessonId.isBlank()) {
            lessonContentLoading.setVisibility(View.VISIBLE);
            viewModel.loadLessonDetail(courseId, lessonId);
        }
    }

    private void observeViewModel() {
        viewModel.detail.observe(getViewLifecycleOwner(), detail -> {
            if (detail == null) return;
            lessonContentLoading.setVisibility(View.GONE);
            if (viewerOpened) loadDetailInWebView(detail);
            if (!contentBound) {
                contentBound = true;
                bindLessonContent(detail);
            }
            if (detail.completed && markCompleteButton != null) {
                markCompleteButton.setEnabled(false);
                markCompleteButton.setText(R.string.lesson_player_completed);
            }
        });

        viewModel.detailLoading.observe(getViewLifecycleOwner(), loading -> {
            if (viewerProgress == null) return;
            viewerProgress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.detailError.observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isBlank()) return;
            lessonContentLoading.setVisibility(View.GONE);
            Toast.makeText(requireContext(),
                    R.string.lesson_viewer_load_error, Toast.LENGTH_SHORT).show();
            if (!"VIDEO".equalsIgnoreCase(lessonType)) {
                lessonFallbackCard.setVisibility(View.VISIBLE);
            }
        });

        viewModel.completed.observe(getViewLifecycleOwner(), completed -> {
            if (!Boolean.TRUE.equals(completed)) return;
            if (markCompleteButton != null) {
                markCompleteButton.setEnabled(false);
                markCompleteButton.setText(R.string.lesson_player_completed);
            }
            if (!isPreview && !courseId.isBlank() && !lessonId.isBlank() && !xpAwarded) {
                xpAwarded = true;
                new GamificationRepository().loadMyState(new GamificationRepository.StateCallback() {
                    @Override public void onSuccess(GamificationUiState ignored) {}
                    @Override public void onError(String ignored) {}
                });
            }
        });

        viewModel.completionError.observe(getViewLifecycleOwner(), reason -> {
            if (reason == null) return;
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

    // ─── Content-type rendering ───────────────────────────────────────────────

    private void bindLessonContent(LessonDetail detail) {
        lessonTextContentArea.setVisibility(View.GONE);
        lessonArticleCard.setVisibility(View.GONE);
        lessonResourceCard.setVisibility(View.GONE);
        lessonFallbackCard.setVisibility(View.GONE);

        String type = detail.lessonType != null ? detail.lessonType.toUpperCase(Locale.ROOT) : "";
        String url  = detail.contentUrl  != null ? detail.contentUrl.trim()  : "";
        String body = detail.contentBody != null ? detail.contentBody.trim() : "";

        // The which-surface-to-show decision is pure and unit-tested in
        // LessonContentTypeResolver; this method only maps the result onto views.
        LessonContentTypeResolver.Result result =
                LessonContentTypeResolver.resolve(type, url, body);

        playerVideoHeader.setVisibility(result.videoHeader ? View.VISIBLE : View.GONE);
        compactTopBar.setVisibility(result.videoHeader ? View.GONE : View.VISIBLE);

        switch (result.display) {
            case VIDEO_ONLY:
                break;

            case VIDEO_WITH_TEXT:
            case TEXT:
                lessonTextContentArea.setVisibility(View.VISIBLE);
                renderTextContent(body);
                break;

            case ARTICLE:
                showArticleCard(url, articleButtonLabel(type));
                break;

            case PDF:
                lessonResourceCard.setVisibility(View.VISIBLE);
                lessonResourceLabel.setText(R.string.lesson_player_pdf_label);
                lessonOpenResourceButton.setText(R.string.lesson_player_view_pdf);
                if (result.actionEnabled) {
                    // The previous flow wrapped the URL in Google Docs Viewer and loaded it into
                    // the in-app WebView, which disclosed private lesson URLs to a third party
                    // (audit M6/M9). Now download to private cache and open via FileProvider.
                    String pdfUrl = url;
                    lessonOpenResourceButton.setOnClickListener(v -> downloadAndOpenPdf(pdfUrl));
                } else {
                    lessonOpenResourceButton.setEnabled(false);
                    lessonOpenResourceButton.setAlpha(0.4f);
                }
                break;

            case RESOURCE:
                lessonResourceCard.setVisibility(View.VISIBLE);
                lessonResourceLabel.setText(R.string.lesson_player_resource_label);
                lessonOpenResourceButton.setText(R.string.lesson_player_open_resource);
                if (result.actionEnabled) {
                    lessonOpenResourceButton.setOnClickListener(v -> openExternalUrl(url));
                } else {
                    lessonOpenResourceButton.setEnabled(false);
                    lessonOpenResourceButton.setAlpha(0.4f);
                }
                break;

            case FALLBACK:
            default:
                lessonFallbackCard.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Picks the article card's button label so it matches the original per-type wording:
     * TEXT lessons say "view full content", ARTICLE/LINK say "open article", and any other
     * URL-bearing type (the default branch) says "open resource".
     */
    private int articleButtonLabel(String type) {
        if ("TEXT".equals(type)) {
            return R.string.lesson_player_view_full_content;
        }
        if ("ARTICLE".equals(type) || "LINK".equals(type)) {
            return R.string.lesson_player_open_article;
        }
        return R.string.lesson_player_open_resource;
    }

    private void showArticleCard(String url, int buttonTextRes) {
        lessonArticleCard.setVisibility(View.VISIBLE);
        lessonArticleUrl.setText(url);
        lessonOpenArticleButton.setText(buttonTextRes);
        lessonOpenArticleButton.setOnClickListener(v -> openExternalUrl(url));
    }

    @SuppressWarnings("deprecation")
    private void renderTextContent(String contentBody) {
        String html = contentBody.replace("\n", "<br/>");
        Spanned parsed;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            parsed = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        } else {
            parsed = Html.fromHtml(html);
        }

        // Lesson bodies are teacher/admin-authored and may embed <a href> links. Html.fromHtml
        // turns those into URLSpans that LinkMovementMethod would launch via a raw ACTION_VIEW —
        // bypassing UrlSecurityPolicy and letting a file://, intent://, or javascript: href escape
        // the player's URL policy (audit 2026-06-19 P2-1). Re-wrap every URLSpan so a tap routes
        // through openExternalUrl(), which classifies the URL before launching anything.
        SpannableStringBuilder safe = new SpannableStringBuilder(parsed);
        for (URLSpan span : safe.getSpans(0, safe.length(), URLSpan.class)) {
            int start = safe.getSpanStart(span);
            int end = safe.getSpanEnd(span);
            int flags = safe.getSpanFlags(span);
            final String linkUrl = span.getURL();
            safe.removeSpan(span);
            safe.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    openExternalUrl(linkUrl);
                }
            }, start, end, flags);
        }

        lessonTextContent.setText(safe);
        lessonTextContent.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void openExternalUrl(String url) {
        if (url == null || url.isBlank()) {
            Toast.makeText(requireContext(), R.string.lesson_player_no_link, Toast.LENGTH_SHORT).show();
            return;
        }
        String normalized = url.trim();
        // Backend may carry a bare host (no scheme); promote to https before classification so
        // policy decisions reflect the actual link being launched.
        if (!normalized.contains("://")) {
            normalized = "https://" + normalized;
        }

        UrlSecurityPolicy.Decision decision = UrlSecurityPolicy.classify(normalized, webViewTrustedHosts);
        if (decision == UrlSecurityPolicy.Decision.BLOCK) {
            // file://, javascript:, intent://, http://, unknown schemes — all rejected here so a
            // teacher-authored content URL can never coerce the player into rendering arbitrary
            // local files or launching attacker-chosen components.
            Toast.makeText(requireContext(),
                    R.string.lesson_player_unsafe_link, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(normalized)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.lesson_player_cannot_open_link, Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAndOpenPdf(String url) {
        if (url == null || url.isBlank()) {
            Toast.makeText(requireContext(), R.string.lesson_player_no_link, Toast.LENGTH_SHORT).show();
            return;
        }
        // PDF flow always goes to private cache + FileProvider open. Authenticated bearer is
        // only attached for backend-host PDFs (see LessonPdfDownloader.isBackendHost); third-
        // party hosts are fetched with a separate un-authenticated client so the Firebase token
        // never leaves the EduLife origin.
        Toast.makeText(requireContext(), R.string.lesson_player_pdf_downloading, Toast.LENGTH_SHORT).show();
        LessonPdfDownloader.download(requireContext(), url, deriveFileNameHint(url),
                new LessonPdfDownloader.Callback() {
                    @Override
                    public void onDownloaded(@NonNull File pdf) {
                        if (!isAdded()) return;
                        openPrivatePdfFile(pdf);
                    }

                    @Override
                    public void onUnsafeUrl() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                R.string.lesson_player_unsafe_link, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNetworkError() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                R.string.lesson_player_pdf_download_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openPrivatePdfFile(File pdf) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                pdf);
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/pdf");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(),
                    R.string.cert_no_pdf_viewer, Toast.LENGTH_LONG).show();
        }
    }

    private String deriveFileNameHint(String url) {
        int slash = url.lastIndexOf('/');
        String tail = slash >= 0 && slash < url.length() - 1 ? url.substring(slash + 1) : url;
        int q = tail.indexOf('?');
        if (q > 0) tail = tail.substring(0, q);
        return tail.isEmpty() ? "lesson" : tail;
    }

    // ─── In-app WebView viewer ────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void configureWebView() {
        WebSettings settings = viewerWebView.getSettings();
        // JS is OFF by default. It is only re-enabled right before loading a trusted-host video
        // embed (loadDetailInWebView). Inline body HTML, which never needs JS, stays sandboxed.
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(true);

        viewerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String requestUrl = request.getUrl() != null ? request.getUrl().toString() : null;
                UrlSecurityPolicy.Decision decision =
                        UrlSecurityPolicy.classify(requestUrl, webViewTrustedHosts);
                if (decision == UrlSecurityPolicy.Decision.ALLOW_IN_APP) {
                    return false; // let WebView load it
                }
                if (decision == UrlSecurityPolicy.Decision.ALLOW_EXTERNAL) {
                    // Send navigations to non-allowlisted HTTPS hosts to the system browser so
                    // the user sees the destination origin and the in-app WebView doesn't run
                    // arbitrary third-party JavaScript inside the player surface.
                    openExternalUrl(requestUrl);
                    return true;
                }
                // BLOCK: file://, javascript:, intent://, http://, unknown schemes.
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            R.string.lesson_player_unsafe_link, Toast.LENGTH_SHORT).show();
                }
                return true;
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

        // PDFs no longer load through the WebView — they go through the authenticated download
        // path. Anything still routed here that the resolver flags as PDF gets bumped to that
        // safer path instead of falling back into the WebView and re-introducing the GDocs leak.
        if (!url.isEmpty()
                && LessonContentTypeResolver.shouldDownloadInsteadOfInline(detail.lessonType, url)) {
            viewerOpened = false;
            downloadAndOpenPdf(url);
            return;
        }

        viewerContainer.setVisibility(View.VISIBLE);
        if (viewerBackCallback != null) {
            viewerBackCallback.setEnabled(true);
        }

        if (!url.isEmpty()) {
            UrlSecurityPolicy.Decision decision =
                    UrlSecurityPolicy.classify(url, webViewTrustedHosts);
            if (decision == UrlSecurityPolicy.Decision.ALLOW_IN_APP) {
                // Trusted-host video embeds (YouTube/Vimeo/backend) need JS to play. Enable it
                // for the load and rely on the allowlist + WebViewClient to keep navigation
                // inside the trusted origin.
                viewerWebView.getSettings().setJavaScriptEnabled(true);
                viewerWebView.getSettings().setDomStorageEnabled(true);
                viewerWebView.loadUrl(LessonContentTypeResolver.resolveViewerUrl(detail.lessonType, url));
                return;
            }
            // Non-allowlisted HTTPS / blocked: don't bring the URL into the in-app surface.
            // ALLOW_EXTERNAL goes to the system browser; BLOCK toasts and aborts.
            viewerContainer.setVisibility(View.GONE);
            viewerOpened = false;
            if (viewerBackCallback != null) viewerBackCallback.setEnabled(false);
            if (decision == UrlSecurityPolicy.Decision.ALLOW_EXTERNAL) {
                openExternalUrl(url);
            } else {
                Toast.makeText(requireContext(),
                        R.string.lesson_player_unsafe_link, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Inline body — render as data URL with JS explicitly off. setJavaScriptEnabled(false)
        // is reapplied here in case a previous load enabled it for a trusted video.
        viewerWebView.getSettings().setJavaScriptEnabled(false);
        viewerWebView.getSettings().setDomStorageEnabled(false);
        String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>"
                + "<style>body{font-family:sans-serif;padding:16px;line-height:1.6;color:#222}</style>"
                + "</head><body>" + body.replace("\n", "<br/>") + "</body></html>";
        viewerWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void closeInAppViewer() {
        if (!viewerOpened) return;
        viewerOpened = false;

        if (viewerWebView != null) viewerWebView.loadUrl("about:blank");
        viewerContainer.setVisibility(View.GONE);
        if (viewerBackCallback != null) {
            viewerBackCallback.setEnabled(false);
        }

        if (!isPreview && !courseId.isBlank() && !lessonId.isBlank()) {
            viewModel.markComplete(courseId, lessonId);
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

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

    // ─── Next lesson navigation ───────────────────────────────────────────────

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
