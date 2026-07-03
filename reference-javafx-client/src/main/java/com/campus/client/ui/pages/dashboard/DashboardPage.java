package com.campus.client.ui.pages.dashboard;

import com.campus.client.model.User;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** DashboardPage - landing screen after login, with quick links into the app's main features. */
public class DashboardPage extends VBox {

    private final Button browseFacilitiesButton = new Button("Browse Facilities");
    private final Button bookingHistoryButton = new Button("My Booking History");
    private final Button assistantButton = new Button("Ask the Campus Assistant");

    private Runnable onBrowseFacilities;
    private Runnable onBookingHistory;
    private Runnable onAssistant;

    public DashboardPage(User user) {
        setSpacing(16);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_LEFT);

        Label welcome = new Label("Welcome, " + user.getName() + "!");
        welcome.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("What would you like to do today?");
        subtitle.setStyle("-fx-text-fill: #666666;");

        for (Button b : new Button[]{browseFacilitiesButton, bookingHistoryButton, assistantButton}) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(48);
        }

        browseFacilitiesButton.setOnAction(e -> { if (onBrowseFacilities != null) onBrowseFacilities.run(); });
        bookingHistoryButton.setOnAction(e -> { if (onBookingHistory != null) onBookingHistory.run(); });
        assistantButton.setOnAction(e -> { if (onAssistant != null) onAssistant.run(); });

        VBox actions = new VBox(12, browseFacilitiesButton, bookingHistoryButton, assistantButton);
        actions.setMaxWidth(420);

        getChildren().addAll(welcome, subtitle, actions);
    }

    public void setOnBrowseFacilities(Runnable r) { this.onBrowseFacilities = r; }
    public void setOnBookingHistory(Runnable r) { this.onBookingHistory = r; }
    public void setOnAssistant(Runnable r) { this.onAssistant = r; }
}