package com.edulife.analytics.repository;

/**
 * Read-only projection for a completion funnel. Each value counts distinct (user, course)
 * enrollment grains in scope that reached a given stage:
 *   enrolled  -> active enrollment exists
 *   started   -> >= 1 lesson completed in that course
 *   completed -> every lesson completed (course_progress total>0 and completed>=total)
 *   passed    -> a passing exam attempt for that course
 *   certified -> a certificate issued for that (user, course)
 * Stages are monotonically non-increasing for a clean funnel.
 */
public interface FunnelProjection {
    long getEnrolled();
    long getStarted();
    long getCompleted();
    long getPassed();
    long getCertified();
}
