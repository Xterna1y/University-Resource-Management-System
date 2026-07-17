package com.campus.client.ui.pages.dashboard;

import com.campus.client.model.User;
import com.campus.client.ui.components.Theme;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * DashboardPage - landing screen after login. Same public API as before
 * (constructor takes only User, same three callback setters); this pass
 * only changes the visual layout to a card-style "Quick Actions" panel
 * matching the mockup. No new data sources were wired in.
 */
public class DashboardPage extends VBox {

    private final Button browseFacilitiesButton;
    private final Button bookingHistoryButton;
    private final Button assistantButton;

    private Runnable onBrowseFacilities;
    private Runnable onBookingHistory;
    private Runnable onAssistant;

    public DashboardPage(User user) {
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color:" + Theme.GREY_BG + ";");

        Label welcome = new Label("Hello, " + user.getName());
        welcome.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        welcome.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        Label heading = new Label("Dashboard");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        Label subtitle = new Label("Overview of your campus activities");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        VBox headerBlock = new VBox(4, welcome, heading, subtitle);

        browseFacilitiesButton = actionRow("\uD83D\uDCC5", "Book a Resource",
                "Browse facilities and check availability");
        bookingHistoryButton = actionRow("\uD83D\uDCCB", "View Booking History",
                "See your upcoming, past and cancelled bookings");
        assistantButton = actionRow("\uD83E\uDD16", "Ask Assistant",
                "Get grounded answers about campus services");

        browseFacilitiesButton.setOnAction(e -> { if (onBrowseFacilities != null) onBrowseFacilities.run(); });
        bookingHistoryButton.setOnAction(e -> { if (onBookingHistory != null) onBookingHistory.run(); });
        assistantButton.setOnAction(e -> { if (onAssistant != null) onAssistant.run(); });

        VBox actionsList = new VBox(10, browseFacilitiesButton, bookingHistoryButton, assistantButton);

        Label quickActionsTitle = new Label("Quick Actions");
        quickActionsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        quickActionsTitle.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        VBox quickActionsCard = new VBox(12, quickActionsTitle, actionsList);
        quickActionsCard.setPadding(new Insets(20));
        quickActionsCard.setStyle(Theme.card());
        quickActionsCard.setMaxWidth(480);

        getChildren().addAll(headerBlock, quickActionsCard);
    }

    /** One clickable row in the Quick Actions card: icon + title + short description. */
    private Button actionRow(String icon, String title, String description) {
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(18));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        titleLabel.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + "; -fx-font-size: 11px;");

        VBox textBlock = new VBox(2, titleLabel, descLabel);

        HBox content = new HBox(12, iconLabel, textBlock);
        content.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(12));
        button.setStyle("-fx-background-color: white; -fx-border-color:" + Theme.GREY_BORDER
                + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color:" + Theme.GREY_BG
                + "; -fx-border-color:" + Theme.RED + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: white; -fx-border-color:" + Theme.GREY_BORDER
                + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));

        HBox.setHgrow(button, Priority.ALWAYS);
        return button;
    }

    public void setOnBrowseFacilities(Runnable r) { this.onBrowseFacilities = r; }
    public void setOnBookingHistory(Runnable r) { this.onBookingHistory = r; }
    public void setOnAssistant(Runnable r) { this.onAssistant = r; }
}