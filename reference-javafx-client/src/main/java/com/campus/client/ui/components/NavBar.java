package com.campus.client.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Sidebar navigation. Same callback setters as before (setOnDashboard,
 * setOnFacilities, setOnHistory, setOnAssistant, setOnLogout).
 * Changes: Profile removed, and the current page is now highlighted red
 * (matching the mockup) via setActive(String).
 */
public class NavBar extends VBox {

    public static final String DASHBOARD = "dashboard";
    public static final String FACILITIES = "facilities";
    public static final String BOOKINGS = "bookings";
    public static final String ASSISTANT = "assistant";

    private final Label logo = new Label("TU");

    private final Button dashboardButton = createButton("Dashboard");
    private final Button facilitiesButton = createButton("Facilities");
    private final Button historyButton = createButton("Bookings");
    private final Button assistantButton = createButton("Assistant");
    private final Button logoutButton = createButton("Logout");

    private String activePage = DASHBOARD;

    public NavBar() {
        setPrefWidth(180);
        setMinWidth(180);
        setMaxWidth(180);

        setSpacing(8);
        setPadding(new Insets(20));

        setStyle("-fx-background-color:#2D2F33;");

        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        logo.setStyle(
                "-fx-text-fill:white;" +
                "-fx-background-color:#D32F2F;" +
                "-fx-background-radius:8;" +
                "-fx-padding:10 18;"
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                logo,
                dashboardButton,
                facilitiesButton,
                historyButton,
                assistantButton,
                spacer,
                logoutButton
        );

        setVisible(false);
        setManaged(false);

        applyActiveStyles();
    }

    private Button createButton(String text) {
        Button b = new Button(text);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPrefWidth(Double.MAX_VALUE);
        b.setPrefHeight(42);
        return b;
    }

    /** Shown once a student logs in; hidden on login/register screens. */
    public void setLoggedIn(boolean loggedIn) {
        setVisible(loggedIn);
        setManaged(loggedIn);
    }

    /** Highlights the given page red (matching the mockup) and un-highlights the rest. */
    public void setActive(String page) {
        this.activePage = page;
        applyActiveStyles();
    }

    private void applyActiveStyles() {
        styleButton(dashboardButton, DASHBOARD.equals(activePage));
        styleButton(facilitiesButton, FACILITIES.equals(activePage));
        styleButton(historyButton, BOOKINGS.equals(activePage));
        styleButton(assistantButton, ASSISTANT.equals(activePage));
        // Logout is never "active" - it's an action, not a page.
        styleButton(logoutButton, false);
    }

    private void styleButton(Button b, boolean active) {
        String activeStyle = "-fx-background-color:#D32F2F; -fx-text-fill:white; -fx-font-size:14; -fx-font-weight:bold;";
        String inactiveStyle = "-fx-background-color:transparent; -fx-text-fill:white; -fx-font-size:14; -fx-font-weight:bold;";
        b.setStyle(active ? activeStyle : inactiveStyle);
        b.setOnMouseEntered(e -> { if (!active) b.setStyle(inactiveStyle + "-fx-background-color:#3D3F44;"); });
        b.setOnMouseExited(e -> b.setStyle(active ? activeStyle : inactiveStyle));
    }

    public void setOnDashboard(Runnable r) { dashboardButton.setOnAction(e -> r.run()); }
    public void setOnFacilities(Runnable r) { facilitiesButton.setOnAction(e -> r.run()); }
    public void setOnHistory(Runnable r) { historyButton.setOnAction(e -> r.run()); }
    public void setOnAssistant(Runnable r) { assistantButton.setOnAction(e -> r.run()); }
    public void setOnLogout(Runnable r) { logoutButton.setOnAction(e -> r.run()); }
}