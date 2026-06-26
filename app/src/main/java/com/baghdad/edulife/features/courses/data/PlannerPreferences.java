package com.baghdad.edulife.features.courses.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.baghdad.edulife.features.courses.model.PlannerTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persists learner planning configurations locally on the device.
 * Since study planning is a client-centric companion to the MVP learning loop,
 * SharedPreferences is used to ensure fast, responsive, and offline-ready storage.
 */
public class PlannerPreferences {

    private static final String PREFS_NAME = "edulife_study_planner";

    private static final String KEY_WEEKLY_GOAL = "weekly_goal";
    private static final String KEY_TARGET_HOURS = "target_hours";
    private static final String KEY_COMPLETED_HOURS = "completed_hours";
    private static final String KEY_STUDY_DAYS = "study_days";
    private static final String KEY_FOCUS_COURSES = "focus_courses";
    private static final String KEY_TASKS = "planner_tasks";

    private static final int DEFAULT_TARGET_HOURS = 10;

    private final SharedPreferences prefs;
    private final Gson gson;

    public PlannerPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveWeeklyGoal(String goal) {
        prefs.edit().putString(KEY_WEEKLY_GOAL, goal).apply();
    }

    public String getWeeklyGoal() {
        return prefs.getString(KEY_WEEKLY_GOAL, "");
    }

    /** Saves the weekly target hours, clamped to the [1, 40] range. */
    public void saveTargetHours(int hours) {
        if (hours < 1) hours = 1;
        if (hours > 40) hours = 40;
        prefs.edit().putInt(KEY_TARGET_HOURS, hours).apply();
    }

    public int getTargetHours() {
        return prefs.getInt(KEY_TARGET_HOURS, DEFAULT_TARGET_HOURS);
    }

    public void saveCompletedHours(float hours) {
        if (hours < 0) hours = 0;
        prefs.edit().putFloat(KEY_COMPLETED_HOURS, hours).apply();
    }

    public float getCompletedHours() {
        return prefs.getFloat(KEY_COMPLETED_HOURS, 0.0f);
    }

    public void saveStudyDays(Set<String> days) {
        prefs.edit().putStringSet(KEY_STUDY_DAYS, days).apply();
    }

    public Set<String> getStudyDays() {
        Set<String> defaultDays = new HashSet<>();
        // Default to Mon, Wed, Fri for a fresh planner
        defaultDays.add("Monday");
        defaultDays.add("Wednesday");
        defaultDays.add("Friday");
        return prefs.getStringSet(KEY_STUDY_DAYS, defaultDays);
    }

    public void saveFocusCourses(Set<String> courseIds) {
        prefs.edit().putStringSet(KEY_FOCUS_COURSES, courseIds).apply();
    }

    public Set<String> getFocusCourses() {
        return prefs.getStringSet(KEY_FOCUS_COURSES, new HashSet<>());
    }

    public void saveTasks(List<PlannerTask> tasks) {
        String json = gson.toJson(tasks);
        prefs.edit().putString(KEY_TASKS, json).apply();
    }

    /** Deserializes planner tasks from JSON, returning an empty list on missing or corrupt data. */
    public List<PlannerTask> getTasks() {
        String json = prefs.getString(KEY_TASKS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<PlannerTask>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            // Safe fallback on parsing error
            return new ArrayList<>();
        }
    }

    /**
     * Resets weekly progress. This is used when a learner wants to transition
     * to a new study cycle/week.
     * Keeps the focus goal and planned days (so they don't have to retype them),
     * but resets logged hours to 0.0f and filters out completed tasks (keeping uncompleted ones).
     */
    public void startNewWeek() {
        // 1. Reset completed study hours
        saveCompletedHours(0.0f);

        // 2. Filter out completed tasks from list (keeps outstanding tasks)
        List<PlannerTask> allTasks = getTasks();
        List<PlannerTask> remainingTasks = new ArrayList<>();
        for (PlannerTask task : allTasks) {
            if (!task.isCompleted()) {
                remainingTasks.add(task);
            }
        }
        saveTasks(remainingTasks);
    }
}
