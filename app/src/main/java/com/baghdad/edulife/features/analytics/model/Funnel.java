package com.baghdad.edulife.features.analytics.model;

/**
 * Completion funnel mirroring the backend FunnelDto. Stage counts are non-increasing
 * (enrolled ≥ started ≥ completed ≥ passed ≥ certified). Display only — computed server-side.
 */
public class Funnel {
    public long enrolled;
    public long started;
    public long completed;
    public long passed;
    public long certified;
}
