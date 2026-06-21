package com.baghdad.edulife.features.gamification.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.gamification.model.LeaderboardEntryResponse;
import com.baghdad.edulife.features.gamification.viewmodel.GamificationViewModel;

import java.util.List;

public class LeaderboardFragment extends Fragment {

    private GamificationViewModel viewModel;
    private LeaderboardAdapter adapter;

    private View podiumContainer;
    private View loadingState;
    private View errorState;
    private View emptyState;
    private RecyclerView recyclerView;

    // Podium views
    private TextView podium1stInitials, podium1stName, podium1stXp;
    private TextView podium2ndInitials, podium2ndName, podium2ndXp;
    private TextView podium3rdInitials, podium3rdName, podium3rdXp;

    private String currentUserId;
    private boolean hasData = false;

    public LeaderboardFragment() {
        super(R.layout.fragment_leaderboard);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);
        currentUserId = new SessionStorage(requireContext()).getUserId();

        View header = view.findViewById(R.id.leaderboardHeader);
        final int origTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), origTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        bindViews(view);
        setupRecycler();

        view.findViewById(R.id.retryButton).setOnClickListener(v -> {
            showLoading();
            viewModel.loadLeaderboard(20);
        });

        observeLeaderboard();

        showLoading();
        viewModel.loadLeaderboard(20);
    }

    private void bindViews(@NonNull View view) {
        podiumContainer = view.findViewById(R.id.podiumContainer);
        loadingState = view.findViewById(R.id.loadingState);
        errorState = view.findViewById(R.id.errorState);
        emptyState = view.findViewById(R.id.leaderboardEmpty);
        recyclerView = view.findViewById(R.id.leaderboardRecyclerView);

        podium1stInitials = view.findViewById(R.id.podium1stInitials);
        podium1stName = view.findViewById(R.id.podium1stName);
        podium1stXp = view.findViewById(R.id.podium1stXp);

        podium2ndInitials = view.findViewById(R.id.podium2ndInitials);
        podium2ndName = view.findViewById(R.id.podium2ndName);
        podium2ndXp = view.findViewById(R.id.podium2ndXp);

        podium3rdInitials = view.findViewById(R.id.podium3rdInitials);
        podium3rdName = view.findViewById(R.id.podium3rdName);
        podium3rdXp = view.findViewById(R.id.podium3rdXp);
    }

    private void setupRecycler() {
        adapter = new LeaderboardAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observeLeaderboard() {
        viewModel.leaderboard.observe(getViewLifecycleOwner(), entries -> {
            if (entries == null || entries.isEmpty()) {
                showEmpty();
                return;
            }
            hasData = true;
            showContent();
            renderPodium(entries);
            adapter.submitList(entries, currentUserId);
        });

        viewModel.leaderboardLoading.observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading) && !hasData) {
                showLoading();
            }
        });

        viewModel.leaderboardError.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !hasData) {
                showError();
            }
        });
    }

    private void renderPodium(List<LeaderboardEntryResponse> entries) {
        LeaderboardEntryResponse first = entries.size() > 0 ? findByRank(entries, 1) : null;
        LeaderboardEntryResponse second = entries.size() > 1 ? findByRank(entries, 2) : null;
        LeaderboardEntryResponse third = entries.size() > 2 ? findByRank(entries, 3) : null;

        if (first == null) {
            podiumContainer.setVisibility(View.GONE);
            return;
        }

        podiumContainer.setVisibility(View.VISIBLE);

        bindPodiumSlot(podium1stInitials, podium1stName, podium1stXp, first);

        View podium2ndView = requireView().findViewById(R.id.podium2nd);
        if (second != null) {
            podium2ndView.setVisibility(View.VISIBLE);
            bindPodiumSlot(podium2ndInitials, podium2ndName, podium2ndXp, second);
        } else {
            podium2ndView.setVisibility(View.INVISIBLE);
        }

        View podium3rdView = requireView().findViewById(R.id.podium3rd);
        if (third != null) {
            podium3rdView.setVisibility(View.VISIBLE);
            bindPodiumSlot(podium3rdInitials, podium3rdName, podium3rdXp, third);
        } else {
            podium3rdView.setVisibility(View.INVISIBLE);
        }
    }

    private void bindPodiumSlot(TextView initials, TextView name, TextView xp,
                                LeaderboardEntryResponse entry) {
        initials.setText(getInitials(entry.displayName));
        name.setText(entry.displayName != null ? entry.displayName : "Learner");
        xp.setText(getString(R.string.leaderboard_xp_label, entry.totalXp));
    }

    @Nullable
    private static LeaderboardEntryResponse findByRank(List<LeaderboardEntryResponse> entries, int rank) {
        for (LeaderboardEntryResponse e : entries) {
            if (e.rank == rank) return e;
        }
        return rank <= entries.size() ? entries.get(rank - 1) : null;
    }

    private void showLoading() {
        loadingState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void showError() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void showEmpty() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        podiumContainer.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private static String getInitials(@Nullable String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder(2);
        for (String part : parts) {
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0)));
            if (out.length() == 2) break;
        }
        return out.length() == 0 ? "?" : out.toString();
    }
}
