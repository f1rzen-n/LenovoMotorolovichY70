package com.moto.resolutionmaster;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public final String packageName;
    public final String label;
    public final Drawable icon;
    public final boolean isSystem;
    public boolean isEnabled;
    public final String description;
    public final int safetyLevel;
    public final String safetyLabel;
    public final int safetyColor;

    public AppInfo(String packageName, String label, Drawable icon, boolean isSystem, boolean isEnabled,
                   String description, int safetyLevel, String safetyLabel, int safetyColor) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.isSystem = isSystem;
        this.isEnabled = isEnabled;
        this.description = description;
        this.safetyLevel = safetyLevel;
        this.safetyLabel = safetyLabel;
        this.safetyColor = safetyColor;
    }
}
