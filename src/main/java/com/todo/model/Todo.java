package com.todo.model;

import java.time.LocalDate;

public class Todo {
    private int id;
    private String title;
    private String description;
    private String priority; // "Low", "Medium", "High"
    private String dueDate;   // YYYY-MM-DD
    private boolean completed;
    private String createdAt;

    public Todo() {
        this.priority = "Medium";
        this.completed = false;
    }

    public Todo(int id, String title, String description, String priority, String dueDate, boolean completed, String createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = (priority == null || priority.isBlank()) ? "Medium" : priority;
        this.dueDate = dueDate;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public Todo(String title, String description, String priority, String dueDate, boolean completed) {
        this(0, title, description, priority, dueDate, completed, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return title;
    }
}
