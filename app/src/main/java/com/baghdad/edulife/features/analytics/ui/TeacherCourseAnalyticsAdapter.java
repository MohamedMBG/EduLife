package com.baghdad.edulife.features.analytics.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.analytics.model.AnalyticsFormat;
import com.baghdad.edulife.features.analytics.model.TeacherCourseAnalytics;

/**
 * Renders one card per course the teacher owns. Display only — all values come pre-scoped from the
 * backend; the adapter never computes or filters analytics, it just formats numbers for display.
 */
public class TeacherCourseAnalyticsAdapter
        extends ListAdapter<TeacherCourseAnalytics, TeacherCourseAnalyticsAdapter.ViewHolder> {

    public TeacherCourseAnalyticsAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_course_analytics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView status;
        private final TextView enrolled;
        private final TextView completion;
        private final TextView passRate;
        private final TextView certificates;
        private final TextView attempts;

        ViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.analyticsCourseTitle);
            status = view.findViewById(R.id.analyticsCourseStatus);
            enrolled = view.findViewById(R.id.analyticsEnrolled);
            completion = view.findViewById(R.id.analyticsCompletion);
            passRate = view.findViewById(R.id.analyticsPassRate);
            certificates = view.findViewById(R.id.analyticsCertificates);
            attempts = view.findViewById(R.id.analyticsAttempts);
        }

        void bind(@NonNull TeacherCourseAnalytics item) {
            title.setText(item.title != null ? item.title : "Untitled");
            status.setText(item.status != null ? item.status : "");
            enrolled.setText(AnalyticsFormat.count(item.activeEnrollments));
            completion.setText(AnalyticsFormat.percent(item.completionRatePercent));
            passRate.setText(AnalyticsFormat.percent(item.passRatePercent));
            certificates.setText(AnalyticsFormat.count(item.certificatesIssued));
            attempts.setText(AnalyticsFormat.passedOfAttempts(item.examsPassed, item.examAttempts));
        }
    }

    private static final DiffUtil.ItemCallback<TeacherCourseAnalytics> DIFF =
            new DiffUtil.ItemCallback<TeacherCourseAnalytics>() {
                @Override
                public boolean areItemsTheSame(@NonNull TeacherCourseAnalytics a,
                                               @NonNull TeacherCourseAnalytics b) {
                    return a.courseId != null && a.courseId.equals(b.courseId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull TeacherCourseAnalytics a,
                                                  @NonNull TeacherCourseAnalytics b) {
                    return a.activeEnrollments == b.activeEnrollments
                            && a.learnersCompleted == b.learnersCompleted
                            && a.completionRatePercent == b.completionRatePercent
                            && a.examAttempts == b.examAttempts
                            && a.examsPassed == b.examsPassed
                            && a.passRatePercent == b.passRatePercent
                            && a.certificatesIssued == b.certificatesIssued;
                }
            };
}
