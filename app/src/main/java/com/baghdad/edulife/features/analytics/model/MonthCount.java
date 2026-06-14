package com.baghdad.edulife.features.analytics.model;

/** One monthly bucket ('YYYY-MM' + count). Mirrors backend MonthCountDto. */
public class MonthCount {
    public String month;
    public long count;
}
