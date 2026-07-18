package com.campus.client.ui.pages.dashboard;

import com.campus.client.data.BookingStorage;
import com.campus.client.model.Booking;
import com.campus.client.model.User;
import com.campus.client.ui.components.Theme;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * DashboardPage - landing screen after login. Same public API as before
 * (constructor takes only User, same three callback setters); this pass
 * only changes the visual layout to a card-style "Quick Actions" panel
 * matching the mockup. No new data sources were wired in.
 */
public class DashboardPage extends VBox {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a");

    private final Button browseFacilitiesButton;
    private final Button bookingHistoryButton;
    private final Button assistantButton;

    private Runnable onBrowseFacilities;
    private Runnable onBookingHistory;
    private Runnable onAssistant;

    public DashboardPage(User user, BookingStorage bookingStorage) {
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

        /*
        * Read upcoming bookings
        */
        List<Booking> upcomingBookings;

        try {
            @SuppressWarnings("unchecked")
            List<Booking> loadedBookings =
                    bookingStorage.upcoming(user.getStudentId());

            upcomingBookings = loadedBookings.stream()
                    .sorted(
                            Comparator.comparing(Booking::getBookingDate)
                                    .thenComparing(Booking::getStartTime)
                    )
                    .toList();

        } catch (RuntimeException exception) {
            upcomingBookings = List.of();
        }

        /*
         * Upcoming booking count
         */
        VBox bookingCountCard = createCountCard(
                String.format("%02d", upcomingBookings.size()),
                "Upcoming\nBookings"
        );

        VBox nextBookingCard =
                createNextBookingCard(upcomingBookings);

        /*
         * Place the booking count and next booking side by side.
         */
        HBox summaryRow = new HBox(
                20,
                bookingCountCard,
                nextBookingCard
        );

        summaryRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(nextBookingCard, Priority.ALWAYS);

        /*
         * Upcoming bookings list
         */
        VBox upcomingBookingsCard =
                createUpcomingBookingsCard(upcomingBookings);




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
        quickActionsCard.setMaxWidth(360); //480

        /*
         * Put upcoming bookings and quick actions side by side.
         */
        HBox contentRow = new HBox(
                20,
                upcomingBookingsCard,
                quickActionsCard
        );

        HBox.setHgrow(upcomingBookingsCard, Priority.ALWAYS);
        contentRow.setAlignment(Pos.TOP_LEFT);

        getChildren().addAll(headerBlock, summaryRow, contentRow);
    }

    /**
     * Creates the small card showing the total number
     * of upcoming bookings.
     */
    private VBox createCountCard(String number, String description) {
        Label numberLabel = new Label(number);
        numberLabel.setFont(
                Font.font("Segoe UI", FontWeight.NORMAL, 28)
        );
        numberLabel.setStyle(
                "-fx-text-fill:" + Theme.DARK + ";"
        );

        Label descriptionLabel = new Label(description);
        descriptionLabel.setFont(
                Font.font("Segoe UI", FontWeight.NORMAL, 14)
        );
        descriptionLabel.setStyle(
                "-fx-text-fill:" + Theme.DARK + ";"
        );

        HBox content = new HBox(
                20,
                numberLabel,
                descriptionLabel
        );

        //content.setAlignment(Pos.CENTER_LEFT);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(content);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));
        card.setStyle(Theme.card());
        card.setPrefWidth(220);
        card.setMaxWidth(220);

        return card;
    }

    /**
     * Creates a summary card showing the nearest upcoming booking.
     */
    private VBox createNextBookingCard(
            List<Booking> upcomingBookings
    ) {
        Label titleLabel = new Label("Next Booking");
        titleLabel.setFont(
                Font.font("Segoe UI", FontWeight.BOLD, 14)
        );
        titleLabel.setStyle(
                "-fx-text-fill:" + Theme.DARK + ";"
        );

        VBox cardContent = new VBox(4);
        cardContent.getChildren().add(titleLabel);

        if (upcomingBookings.isEmpty()) {
            Label emptyLabel = new Label(
                    "You do not have an upcoming booking."
            );

            emptyLabel.setStyle(
                    "-fx-text-fill:" + Theme.TEXT_MUTED + ";"
            );

            cardContent.getChildren().add(emptyLabel);

        } else {
            /*
             * The bookings were already sorted by date and time,
             * so the first booking is the nearest booking.
             */
            Booking nextBooking = upcomingBookings.get(0);

            Label facilityLabel = new Label(
                    nextBooking.getFacilityName()
            );

            facilityLabel.setFont(
                    Font.font("Segoe UI", FontWeight.BOLD, 13)
            );

            facilityLabel.setStyle(
                    "-fx-text-fill:" + Theme.DARK + ";"
            );

            String dateText =
                    nextBooking.getBookingDate() == null
                            ? "-"
                            : nextBooking.getBookingDate()
                            .format(DATE_FORMAT);

            String timeText =
                    nextBooking.getStartTime() == null
                            || nextBooking.getEndTime() == null
                            ? "-"
                            : nextBooking.getStartTime()
                            .format(TIME_FORMAT)
                            + " - "
                            + nextBooking.getEndTime()
                            .format(TIME_FORMAT);

            Label bookingDetailsLabel = new Label(
                    dateText + "    " + timeText
            );

            bookingDetailsLabel.setStyle(
                    "-fx-text-fill:" + Theme.TEXT_MUTED + ";"
            );

            cardContent.getChildren().addAll(
                    facilityLabel,
                    bookingDetailsLabel
            );
        }

        VBox card = new VBox(cardContent);
        card.setPadding(new Insets(18));
        card.setStyle(Theme.card());

        /*
         * The card can expand horizontally while the count card
         * keeps its current size.
         */
        card.setPrefWidth(450);
        card.setMaxWidth(600);

        return card;
    }

    /**
     * Creates the card containing the upcoming booking list.
     */
    private VBox createUpcomingBookingsCard(
            List<Booking> upcomingBookings
    ) {
        Label title = new Label("Upcoming Bookings");
        title.setFont(
                Font.font("Segoe UI", FontWeight.BOLD, 16)
        );
        title.setStyle(
                "-fx-text-fill:" + Theme.DARK + ";"
        );

        VBox bookingRows = new VBox();

        if (upcomingBookings.isEmpty()) {
            Label emptyLabel = new Label(
                    "You do not have any upcoming bookings."
            );

            emptyLabel.setStyle(
                    "-fx-text-fill:" + Theme.TEXT_MUTED + ";"
            );

            emptyLabel.setPadding(
                    new Insets(25, 0, 25, 0)
            );

            bookingRows.getChildren().add(emptyLabel);

        } else {
            upcomingBookings.stream()
                    .limit(3)
                    .forEach(booking ->
                            bookingRows.getChildren().add(
                                    createBookingRow(booking)
                            )
                    );
        }

        VBox card = new VBox(
                12,
                title,
                bookingRows
        );

        card.setPadding(new Insets(20));
        card.setStyle(Theme.card());
        card.setMinWidth(450);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    /**
     * Creates one row representing one upcoming booking.
     */
    private VBox createBookingRow(Booking booking) {
        Label facilityLabel = new Label(
                booking.getFacilityName()
        );

        facilityLabel.setFont(
                Font.font("Segoe UI", FontWeight.BOLD, 13)
        );

        facilityLabel.setStyle(
                "-fx-text-fill:" + Theme.DARK + ";"
        );

        String dateText;

        if (booking.getBookingDate() == null) {
            dateText = "-";
        } else {
            dateText = booking.getBookingDate()
                    .format(DATE_FORMAT);
        }

        String timeText;

        if (booking.getStartTime() == null
                || booking.getEndTime() == null) {
            timeText = "-";
        } else {
            timeText =
                    booking.getStartTime().format(TIME_FORMAT)
                            + " - "
                            + booking.getEndTime().format(TIME_FORMAT);
        }

        Label dateLabel = new Label(dateText);
        dateLabel.setStyle(
                "-fx-text-fill:" + Theme.DARK + ";"
        );

        Label timeLabel = new Label(timeText);
        timeLabel.setStyle(
                "-fx-text-fill:" + Theme.DARK + ";"
        );

        HBox dateAndTime = new HBox(
                30,
                dateLabel,
                timeLabel
        );

        VBox bookingInformation = new VBox(
                5,
                facilityLabel,
                dateAndTime
        );

        Label statusLabel = new Label("Confirmed");
        statusLabel.setPadding(
                new Insets(4, 15, 4, 15)
        );

        statusLabel.setStyle(
                "-fx-background-color: #EAF5E6;"
                        + "-fx-text-fill: #294B25;"
                        + "-fx-background-radius: 3;"
        );

        /*
         * This invisible Region grows and pushes the status
         * label towards the right-hand side of the row.
         */
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox rowContent = new HBox(
                15,
                bookingInformation,
                spacer,
                statusLabel
        );

        rowContent.setAlignment(Pos.CENTER_LEFT);

        VBox row = new VBox(rowContent);
        row.setPadding(
                new Insets(14, 5, 14, 5)
        );

        row.setStyle(
                "-fx-border-color: transparent transparent "
                        + Theme.GREY_BORDER
                        + " transparent;"
        );

        return row;
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