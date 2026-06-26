package com.baghdad.edulife.features.courses.model;

import java.util.List;

/**
 * Generic DTO mapping the backend's paginated Spring Page response, carrying a content list and pagination metadata.
 */
public class CoursePageResponse<T> {
    public List<T> content;
    public int number;
    public int size;
    public int totalPages;
    public long totalElements;
    public boolean first;
    public boolean last;
}
