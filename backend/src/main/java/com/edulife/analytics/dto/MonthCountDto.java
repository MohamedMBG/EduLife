package com.edulife.analytics.dto;

/** One monthly bucket ('YYYY-MM') with its count. Used for cohorts and trends. */
public record MonthCountDto(String month, long count) {}
