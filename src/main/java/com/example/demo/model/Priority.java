package com.example.demo.model;

public enum Priority {
    HIGH("\u9AD8", "text-bg-danger"),
    MEDIUM("\u4E2D", "text-bg-warning"),
    LOW("\u4F4E", "text-bg-success");

    private final String displayName;
    private final String cssClass;

    Priority(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }
}
