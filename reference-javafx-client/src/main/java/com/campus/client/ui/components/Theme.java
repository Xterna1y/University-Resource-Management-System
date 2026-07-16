package com.campus.client.ui.components;

/**
 * Shared style constants matching the Taylor's University mockups
 * (red primary #C62828, dark sidebar #2D2F33, light card backgrounds).
 * Not a graded architecture class - just a place to avoid repeating hex
 * codes across every screen.
 */
public final class Theme {

    public static final String RED = "#C62828";
    public static final String RED_DARK = "#B71C1C";
    public static final String DARK = "#2D2F33";
    public static final String GREY_BG = "#F5F5F7";
    public static final String GREY_BORDER = "#E0E0E0";
    public static final String TEXT_MUTED = "#777777";
    public static final String GREEN_BG = "#E6F4EA";
    public static final String GREEN_TEXT = "#1E7E34";
    public static final String RED_BG = "#FDEAEA";
    public static final String RED_TEXT = "#C62828";

    private Theme() {
    }

    public static String primaryButton() {
        return "-fx-background-color:" + RED + "; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 5; -fx-padding: 10 20;";
    }

    public static String secondaryButton() {
        return "-fx-background-color: white; -fx-text-fill:" + DARK + "; -fx-font-weight: bold;"
                + " -fx-background-radius: 5; -fx-padding: 10 20; -fx-border-color:" + GREY_BORDER + "; -fx-border-radius: 5;";
    }

    /** A small rounded status pill, e.g. for booking status (Confirmed/Cancelled). */
    public static String statusPill(boolean positive) {
        String bg = positive ? GREEN_BG : RED_BG;
        String fg = positive ? GREEN_TEXT : RED_TEXT;
        return "-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-font-weight: bold;"
                + " -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-size: 11px;";
    }

    public static String card() {
        return "-fx-background-color: white; -fx-border-color:" + GREY_BORDER + "; -fx-border-radius: 8;"
                + " -fx-background-radius: 8;";
    }

    public static String title() {
        return "-fx-text-fill:" + DARK + ";" +
                "-fx-font-size:20px;" +
                "-fx-font-weight:bold;";
    }

    public static String subtitle() {
        return "-fx-text-fill:" + TEXT_MUTED + ";" +
                "-fx-font-size:13px;";
    }
}