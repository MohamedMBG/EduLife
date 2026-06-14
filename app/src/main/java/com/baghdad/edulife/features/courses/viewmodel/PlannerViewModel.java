package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.PlannerPreferences;
import com.baghdad.edulife.features.courses.model.PlannerTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * State management class for the Study Planner page.
 * Exposes LiveData reflecting the user's weekly planner parameters and progress.
 */
public class PlannerViewModel extends AndroidViewModel {

    private final PlannerPreferences preferences;

    private final MutableLiveData<String> weeklyGoal = new MutableLiveData<>();
    private final MutableLiveData<Integer> targetHours = new MutableLiveData<>();
    private final MutableLiveData<Float> completedHours = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> studyDays = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> focusCourses = new MutableLiveData<>();
    private final MutableLiveData<List<PlannerTask>> tasks = new MutableLiveData<>();

    public PlannerViewModel(@NonNull Application application) {
        super(application);
        this.preferences = new PlannerPreferences(application);
        loadAll();
    }

    public LiveData<String> getWeeklyGoal() {
        return weeklyGoal;
    }

    public LiveData<Integer> getTargetHours() {
        return targetHours;
    }

    public LiveData<Float> getCompletedHours() {
        return completedHours;
    }

    public LiveData<Set<String>> getStudyDays() {
        return studyDays;
    }

    public LiveData<Set<String>> getFocusCourses() {
        return focusCourses;
    }

    public LiveData<List<PlannerTask>> getTasks() {
        return tasks;
    }

    /**
     * Loads all planner values from persistent storage and updates LiveData.
     */
    private void loadAll() {
        weeklyGoal.setValue(preferences.getWeeklyGoal());
        targetHours.setValue(preferences.getTargetHours());
        completedHours.setValue(preferences.getCompletedHours());
        studyDays.setValue(new HashSet<>(preferences.getStudyDays()));
        focusCourses.setValue(new HashSet<>(preferences.getFocusCourses()));
        tasks.setValue(preferences.getTasks());
    }

    /**
     * Saves the weekly focus goal text.
     */
    public void setWeeklyGoal(String goal) {
        String trimmedGoal = goal == null ? "" : goal.trim();
        if (!trimmedGoal.equals(weeklyGoal.getValue())) {
            preferences.saveWeeklyGoal(trimmedGoal);
            weeklyGoal.setValue(trimmedGoal);
        }
    }

    /**
     * Increments the study target hours by 1 hour (max 40).
     */
    public void incrementTargetHours() {
        int current = targetHours.getValue() != null ? targetHours.getValue() : 10;
        if (current < 40) {
            int newVal = current + 1;
            preferences.saveTargetHours(newVal);
            targetHours.setValue(newVal);
        }
    }

    /**
     * Decrements the study target hours by 1 hour (min 1).
     */
    public void decrementTargetHours() {
        int current = targetHours.getValue() != null ? targetHours.getValue() : 10;
        if (current > 1) {
            int newVal = current - 1;
            preferences.saveTargetHours(newVal);
            targetHours.setValue(newVal);
        }
    }

    /**
     * Adds logged study time (can be decimal, e.g. 0.5 for 30 minutes, 1.0 for 1 hour).
     */
    public void addCompletedHours(float delta) {
        float current = completedHours.getValue() != null ? completedHours.getValue() : 0.0f;
        float newVal = Math.max(0.0f, current + delta);
        // Cap completed hours at double the target hours to keep progress bars reasonable
        int target = targetHours.getValue() != null ? targetHours.getValue() : 10;
        if (newVal > target * 2) {
            newVal = target * 2;
        }
        preferences.saveCompletedHours(newVal);
        completedHours.setValue(newVal);
    }

    /**
     * Toggles a study day in the planned days list.
     * @param day Day name (e.g. "Monday")
     */
    public void toggleStudyDay(String day) {
        Set<String> currentDays = studyDays.getValue() != null ? new HashSet<>(studyDays.getValue()) : new HashSet<>();
        if (currentDays.contains(day)) {
            currentDays.remove(day);
        } else {
            currentDays.add(day);
        }
        preferences.saveStudyDays(currentDays);
        studyDays.setValue(currentDays);
    }

    /**
     * Toggles a focus course in the planned focus course list.
     * @param courseId Enrolled course ID
     */
    public void toggleFocusCourse(String courseId) {
        Set<String> currentCourses = focusCourses.getValue() != null ? new HashSet<>(focusCourses.getValue()) : new HashSet<>();
        if (currentCourses.contains(courseId)) {
            currentCourses.remove(courseId);
        } else {
            currentCourses.add(courseId);
        }
        preferences.saveFocusCourses(currentCourses);
        focusCourses.setValue(currentCourses);
    }

    /**
     * Adds a new task to the weekly checklist.
     */
    public void addTask(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) return;

        List<PlannerTask> currentTasks = tasks.getValue() != null ? new ArrayList<>(tasks.getValue()) : new ArrayList<>();
        // Enforce checklist limit (e.g., maximum 10 active tasks to avoid UI clutter)
        if (currentTasks.size() >= 10) return;

        currentTasks.add(new PlannerTask(trimmed));
        preferences.saveTasks(currentTasks);
        tasks.setValue(currentTasks);
    }

    /**
     * Toggles the completion status of a checklist task.
     */
    public void toggleTask(String taskId) {
        if (taskId == null) return;
        List<PlannerTask> currentTasks = tasks.getValue() != null ? new ArrayList<>(tasks.getValue()) : new ArrayList<>();
        boolean changed = false;
        for (PlannerTask task : currentTasks) {
            if (taskId.equals(task.getId())) {
                task.setCompleted(!task.isCompleted());
                changed = true;
                break;
            }
        }
        if (changed) {
            preferences.saveTasks(currentTasks);
            tasks.setValue(currentTasks);
        }
    }

    /**
     * Deletes a checklist task.
     */
    public void deleteTask(String taskId) {
        if (taskId == null) return;
        List<PlannerTask> currentTasks = tasks.getValue() != null ? new ArrayList<>(tasks.getValue()) : new ArrayList<>();
        boolean removed = false;
        for (int i = 0; i < currentTasks.size(); i++) {
            if (taskId.equals(currentTasks.get(i).getId())) {
                currentTasks.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            preferences.saveTasks(currentTasks);
            tasks.setValue(currentTasks);
        }
    }

    /**
     * Resets weekly progress (hours logged -> 0.0f, removes completed tasks).
     */
    public void startNewWeek() {
        preferences.startNewWeek();
        loadAll();
    }
}
