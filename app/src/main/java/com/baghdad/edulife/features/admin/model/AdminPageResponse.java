package com.baghdad.edulife.features.admin.model;

import java.util.List;

public class AdminPageResponse<T> {
    public List<T> content;
    public int totalPages;
    public long totalElements;
    public int size;
    public int number;
    public boolean first;
    public boolean last;
    public boolean empty;
}
