package com.baghdad.edulife.features.analytics.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.analytics.model.DayStudyActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight, dependency-free weekly bar chart. Draws one rounded bar per day with a value on top
 * and a weekday label below. The "today" bar uses the brand primary colour; other bars use a soft
 * surface tint. Heights scale to the max value so the tallest bar fills the plot area.
 *
 * Intentionally simple: no animations, no axes, no chart library. Configure via {@link #setData}.
 */
public class WeeklyBarChartView extends View {

    private final List<DayStudyActivity> days = new ArrayList<>();

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private int colorBarToday;
    private int colorBarDefault;
    private int colorTrack;
    private int colorLabel;
    private int colorValue;

    private final float cornerRadius = dp(6);
    private final float barWidth = dp(20);
    private final float labelGap = dp(10);
    private final float valueGap = dp(6);

    public WeeklyBarChartView(Context context) {
        super(context);
        init(context);
    }

    public WeeklyBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeeklyBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        colorBarToday = ContextCompat.getColor(context, R.color.brand_primary);
        colorBarDefault = ContextCompat.getColor(context, R.color.brand_primary_border);
        colorTrack = ContextCompat.getColor(context, R.color.brand_surface_muted);
        colorLabel = ContextCompat.getColor(context, R.color.brand_text_secondary);
        colorValue = ContextCompat.getColor(context, R.color.brand_text_primary);

        trackPaint.setColor(colorTrack);

        labelPaint.setColor(colorLabel);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(11));

        valuePaint.setColor(colorValue);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(sp(11));
        valuePaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /** Replaces the chart data and requests a redraw. Null/empty is rendered as an empty plot. */
    public void setData(@Nullable List<DayStudyActivity> data) {
        days.clear();
        if (data != null) days.addAll(data);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Default to a comfortable fixed height when the parent allows wrap_content.
        int desiredHeight = (int) dp(180);
        int width = resolveSize((int) dp(280), widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (days.isEmpty()) return;

        float labelArea = labelPaint.getTextSize() + labelGap;
        float valueArea = valuePaint.getTextSize() + valueGap;
        float plotTop = getPaddingTop() + valueArea;
        float plotBottom = getHeight() - getPaddingBottom() - labelArea;
        float plotHeight = Math.max(0, plotBottom - plotTop);

        int count = days.size();
        float usableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float slot = usableWidth / count;

        int max = 1;
        for (DayStudyActivity d : days) max = Math.max(max, d.lessonsCompleted);

        for (int i = 0; i < count; i++) {
            DayStudyActivity d = days.get(i);
            float cx = getPaddingLeft() + slot * i + slot / 2f;
            float left = cx - barWidth / 2f;
            float right = cx + barWidth / 2f;

            // Faint full-height track so zero-activity days still read as a slot.
            barRect.set(left, plotTop, right, plotBottom);
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, trackPaint);

            float fraction = max == 0 ? 0 : (float) d.lessonsCompleted / max;
            float barHeight = plotHeight * fraction;
            float barTop = plotBottom - barHeight;

            if (d.lessonsCompleted > 0) {
                barPaint.setColor(d.today ? colorBarToday : colorBarDefault);
                barRect.set(left, barTop, right, plotBottom);
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint);
            }

            // Value above the bar (or above the track baseline when zero).
            float valueBaseline = (d.lessonsCompleted > 0 ? barTop : plotBottom) - valueGap;
            valuePaint.setColor(d.today ? colorBarToday : colorValue);
            canvas.drawText(String.valueOf(d.lessonsCompleted), cx, valueBaseline, valuePaint);

            // Weekday label below the plot.
            labelPaint.setFakeBoldText(d.today);
            float labelBaseline = plotBottom + labelGap + labelPaint.getTextSize() * 0.8f;
            canvas.drawText(d.label, cx, labelBaseline, labelPaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
