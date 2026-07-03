package com.campus.client.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Simple horizontal navigation bar. MainView wires each button's action via
 * the setters below, so NavBar has no direct knowledge of navigation logic.
 */
public class NavBar extends HBox {

    private final Button dashboardButton = new Button("Dashboard");
    private final Button facilitiesButton = new Button("Facilities");
    private final Button historyButton = new Button("Booking History");
    private final Button assistantButton = new Button("Assistant");
    private final Button logoutButton = new Button("Logout");

    public NavBar() {
        setSpacing(10);
        setPadding(new Insets(10, 20, 10, 20));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(dashboardButton, facilitiesButton, historyButton, assistantButton,
                spacer, logoutButton);

        setVisible(false);
        setManaged(false);
    }

    /** Shown once a student logs in; hidden on the login/register screens. */
    public void setLoggedIn(boolean loggedIn) {
        setVisible(loggedIn);
        setManaged(loggedIn);
    }

    public void setOnDashboard(Runnable r) { dashboardButton.setOnAction(e -> r.run()); }
    public void setOnFacilities(Runnable r) { facilitiesButton.setOnAction(e -> r.run()); }
    public void setOnHistory(Runnable r) { historyButton.setOnAction(e -> r.run()); }
    public void setOnAssistant(Runnable r) { assistantButton.setOnAction(e -> r.run()); }
    public void setOnLogout(Runnable r) { logoutButton.setOnAction(e -> r.run()); }
}