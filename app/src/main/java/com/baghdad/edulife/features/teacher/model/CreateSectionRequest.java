package com.baghdad.edulife.features.teacher.model;

public class CreateSectionRequest {
    public String title;
    public String description;
    public int displayOrder;

    public CreateSectionRequest(String title, String description, int displayOrder) {
        this.title = title;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
