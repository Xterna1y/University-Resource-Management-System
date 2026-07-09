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
 * Sidebar navigation used throughout the application.
 * MainView still wires navigation through the setters below.
 */
public class NavBar extends VBox {

    private final Label logo = new Label("TU");

    private final Button dashboardButton = createButton("Dashboard");
    private final Button facilitiesButton = createButton("Facilities");
    private final Button historyButton = createButton("Bookings");
    private final Button assistantButton = createButton("Assistant");
    private final Button profileButton = createButton("Profile");
    private final Button logoutButton = createButton("Logout");

    public NavBar() {

        setPrefWidth(180);
        setMinWidth(180);
        setMaxWidth(180);

        setSpacing(8);
        setPadding(new Insets(20));

        setStyle(
                "-fx-background-color:#2D2F33;"
        );

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
                profileButton,
                spacer,
                logoutButton
        );

        setVisible(false);
        setManaged(false);
    }

    private Button createButton(String text) {

        Button b = new Button(text);

        b.setAlignment(Pos.CENTER_LEFT);

        b.setPrefWidth(Double.MAX_VALUE);

        b.setPrefHeight(42);

        b.setStyle(
                "-fx-background-color:transparent;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:14;" +
                "-fx-font-weight:bold;"
        );

        b.setOnMouseEntered(e ->
                b.setStyle(
                        "-fx-background-color:#D32F2F;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:14;" +
                        "-fx-font-weight:bold;"
                ));

        b.setOnMouseExited(e ->
                b.setStyle(
                        "-fx-background-color:transparent;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:14;" +
                        "-fx-font-weight:bold;"
                ));

        return b;
    }

    /** Shown once a student logs in; hidden on login/register screens. */
    public void setLoggedIn(boolean loggedIn) {
        setVisible(loggedIn);
        setManaged(loggedIn);
    }

    public void setOnDashboard(Runnable r) {
        dashboardButton.setOnAction(e -> r.run());
    }

    public void setOnFacilities(Runnable r) {
        facilitiesButton.setOnAction(e -> r.run());
    }

    public void setOnHistory(Runnable r) {
        historyButton.setOnAction(e -> r.run());
    }

    public void setOnAssistant(Runnable r) {
        assistantButton.setOnAction(e -> r.run());
    }

    public void setOnLogout(Runnable r) {
        logoutButton.setOnAction(e -> r.run());
    }
}