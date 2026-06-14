package com.baghdad.edulife.features.courses.model;

import java.util.UUID;

/**
 * Model representing a single custom study checklist task.
 * Saved locally on the device as part of the student's study plan.
 */
public class PlannerTask {
    private String id;
    private String title;
    private boolean completed;

    public PlannerTask() {
        this.id = UUID.randomUUID().toString();
        this.completed = false;
    }

    public PlannerTask(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.completed = false;
    }

    public PlannerTask(String id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
