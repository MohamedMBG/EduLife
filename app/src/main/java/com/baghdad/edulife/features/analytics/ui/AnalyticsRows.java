package com.baghdad.edulife.features.analytics.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.analytics.model.AnalyticsFormat;
import com.baghdad.edulife.features.analytics.model.Funnel;
import com.baghdad.edulife.features.analytics.model.MonthCount;

import java.util.List;

/**
 * Builds label+value rows into a container by inflating item_analytics_stat_row. Used for the
 * dynamic cohort/trend/funnel lists so no extra RecyclerView/adapter is needed for short,
 * non-recycling lists. Pure view code; all values come pre-scoped from the backend.
 */
final class AnalyticsRows {

    private AnalyticsRows() {}

    /** Removes any previously-inflated rows so a reload does not duplicate content. */
    static void clear(LinearLayout container) {
        container.removeAllViews();
    }

    /** Inflates one label+value row into the container. */
    static void addRow(LinearLayout container, String label, String value) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View row = inflater.inflate(R.layout.item_analytics_stat_row, container, false);
        ((TextView) row.findViewById(R.id.statRowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.statRowValue)).setText(value);
        container.addView(row);
    }

    /** Renders monthly buckets (cohorts/trends) in order, newest behavior preserved by backend sort. */
    static void renderMonths(LinearLayout container, List<MonthCount> months) {
        clear(container);
        for (MonthCount m : months) {
            addRow(container, m.month, AnalyticsFormat.count(m.count));
        }
    }

    /** Renders the five funnel stages as ordered rows. */
    static void renderFunnel(LinearLayout container, Funnel f) {
        clear(container);
        Context ctx = container.getContext();
        addRow(container, ctx.getString(R.string.analytics_funnel_enrolled), AnalyticsFormat.count(f.enrolled));
        addRow(container, ctx.getString(R.string.analytics_funnel_started), AnalyticsFormat.count(f.started));
        addRow(container, ctx.getString(R.string.analytics_funnel_completed), AnalyticsFormat.count(f.completed));
        addRow(container, ctx.getString(R.string.analytics_funnel_passed), AnalyticsFormat.count(f.passed));
        addRow(container, ctx.getString(R.string.analytics_funnel_certified), AnalyticsFormat.count(f.certified));
    }
}
