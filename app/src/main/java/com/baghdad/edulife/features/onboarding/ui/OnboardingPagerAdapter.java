package com.baghdad.edulife.features.onboarding.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.onboarding.model.OnboardingItem;

import java.util.List;

class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder> {

    private final Context context;
    private final List<OnboardingItem> items;
    private final float cornerRadius;

    OnboardingPagerAdapter(Context context, List<OnboardingItem> items) {
        this.context = context;
        this.items = items;
        this.cornerRadius = 24 * context.getResources().getDisplayMetrics().density;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_onboarding, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class PageViewHolder extends RecyclerView.ViewHolder {

        private final View illustrationContainer;
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            illustrationContainer = itemView.findViewById(R.id.illustrationContainer);
            iconView = itemView.findViewById(R.id.imgIllustration);
            titleView = itemView.findViewById(R.id.txtPageTitle);
            subtitleView = itemView.findViewById(R.id.txtPageSubtitle);
        }

        void bind(OnboardingItem item) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(context, item.getIllustrationBgColor()));
            bg.setCornerRadius(cornerRadius);
            illustrationContainer.setBackground(bg);

            iconView.setImageResource(item.getIconRes());
            titleView.setText(buildAccentedTitle(item));
            subtitleView.setText(item.getSubtitle());
        }

        private SpannableString buildAccentedTitle(OnboardingItem item) {
            String title = item.getTitle();
            String accent = item.getAccentText();
            SpannableString spannable = new SpannableString(title);
            int start = title.indexOf(accent);
            if (start >= 0) {
                spannable.setSpan(
                        new ForegroundColorSpan(
                                ContextCompat.getColor(context, R.color.onboarding_green)),
                        start, start + accent.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannable;
        }
    }
}
