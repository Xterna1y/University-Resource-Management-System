package com.campus.client.ui.pages.booking;

import com.campus.client.data.BookingStorage;
import com.campus.client.model.Booking;
import com.campus.client.services.mcp.CampusMcpClient;
import com.campus.client.ui.components.Theme;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BookingView - same booking logic as before (validate(), book_resource
 * call on a background thread, BookingStorage.save()). This pass only
 * restyles the layout into the mockup's two-column Booking Details /
 * Booking Summary composition.
 */
public class BookingView extends BorderPane {

    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("[A-Z]{2}-\\d+");

    private final CampusMcpClient mcpClient;
    private final ExecutorService worker;
    private final BookingStorage bookingStorage;
    private final String studentId;

    private final String resourceId;
    private final String facilityName;

    private final TextField resourceField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField startTimeField = new TextField();
    private final TextField endTimeField = new TextField();
    private final TextField purposeField = new TextField();
    private final TextField numberOfPeopleField = new TextField();

    private final Label summaryFacility = new Label();
    private final Label summaryDate = new Label();
    private final Label summaryTime = new Label();
    private final Label summaryPurpose = new Label();
    private final Label statusLabel = new Label();
    private final Button confirmBookingButton = new Button("Confirm Booking");
    private final Button cancelButton = new Button("Cancel");

    private Runnable onBackToAvailability;
    private Runnable onBookingConfirmed;

    public BookingView(CampusMcpClient mcpClient, ExecutorService worker, BookingStorage bookingStorage,
                        String studentId, String resourceId, String facilityName) {
        this.mcpClient = mcpClient;
        this.worker = worker;
        this.bookingStorage = bookingStorage;
        this.studentId = studentId;
        this.resourceId = resourceId;
        this.facilityName = facilityName;

        setPadding(new Insets(20));
        setStyle("-fx-background-color:" + Theme.GREY_BG + ";");
        setTop(buildHeader());
        setCenter(buildBody());

        resourceField.setText(facilityName);
        resourceField.setEditable(false);

        datePicker.valueProperty().addListener((o, a, b) -> refreshSummary());
        startTimeField.textProperty().addListener((o, a, b) -> refreshSummary());
        endTimeField.textProperty().addListener((o, a, b) -> refreshSummary());
        purposeField.textProperty().addListener((o, a, b) -> refreshSummary());
        numberOfPeopleField.textProperty().addListener((o, a, b) -> refreshSummary());
        refreshSummary();

        confirmBookingButton.setOnAction(e -> handleConfirmBooking());
        cancelButton.setOnAction(e -> {
            if (onBackToAvailability != null) onBackToAvailability.run();
        });
    }

    public void setOnBackToAvailability(Runnable r) { this.onBackToAvailability = r; }
    public void setOnBookingConfirmed(Runnable r) { this.onBookingConfirmed = r; }

    private VBox buildHeader() {
        Label back = new Label("\u2190 Back to Availability");
        back.setStyle("-fx-text-fill:" + Theme.DARK + "; -fx-cursor: hand;");
        back.setOnMouseClicked(e -> { if (onBackToAvailability != null) onBackToAvailability.run(); });

        Label title = new Label("Book Resource");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        Label subtitle = new Label("Review your booking details");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        VBox box = new VBox(6, back, title, subtitle);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    /** Two-column layout: Booking Details form (left) + read-only Booking Summary card (right). */
    private HBox buildBody() {
        VBox detailsCard = buildDetailsCard();
        VBox summaryCard = buildSummaryCard();

        HBox.setHgrow(detailsCard, Priority.ALWAYS);
        HBox row = new HBox(20, detailsCard, summaryCard);
        return row;
    }

    private VBox buildDetailsCard() {
        Label cardTitle = new Label("Booking Details");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(15, 0, 15, 0));

        int row = 0;
        grid.addRow(row++, new Label("Facility:"), resourceField);
        grid.addRow(row++, new Label("Date:"), datePicker);

        startTimeField.setPromptText("HH:mm e.g. 10:00");
        endTimeField.setPromptText("HH:mm e.g. 11:00");
        grid.addRow(row++, new Label("Start Time:"), startTimeField);
        grid.addRow(row++, new Label("End Time:"), endTimeField);

        purposeField.setPromptText("e.g. Group project discussion (optional)");
        grid.addRow(row++, new Label("Purpose:"), purposeField);

        numberOfPeopleField.setPromptText("e.g. 4");
        grid.addRow(row++, new Label("No. of People:"), numberOfPeopleField);

        for (Node n : grid.getChildren()) {
            if (n instanceof TextField || n instanceof DatePicker) {
                GridPane.setHgrow(n, Priority.ALWAYS);
                ((Region) n).setPrefWidth(260);
            }
        }

        confirmBookingButton.setStyle(Theme.primaryButton());
        cancelButton.setStyle(Theme.secondaryButton());
        statusLabel.setWrapText(true);

        HBox actions = new HBox(10, confirmBookingButton, cancelButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, cardTitle, grid, actions, statusLabel);
        card.setPadding(new Insets(20));
        card.setStyle(Theme.card());
        return card;
    }

    private VBox buildSummaryCard() {
        Label cardTitle = new Label("Booking Summary");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));

        summaryFacility.setWrapText(true);
        summaryDate.setWrapText(true);
        summaryTime.setWrapText(true);
        summaryPurpose.setWrapText(true);

        grid.addRow(0, boldLabel("Facility"), summaryFacility);
        grid.addRow(1, boldLabel("Date"), summaryDate);
        grid.addRow(2, boldLabel("Time"), summaryTime);
        grid.addRow(3, boldLabel("Purpose"), summaryPurpose);

        VBox card = new VBox(10, cardTitle, grid);
        card.setPadding(new Insets(20));
        card.setStyle(Theme.card());
        card.setPrefWidth(280);
        card.setMinWidth(240);
        return card;
    }

    private Label boldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        return l;
    }

    private void refreshSummary() {
        summaryFacility.setText(facilityName);
        summaryDate.setText(datePicker.getValue() == null ? "-" : datePicker.getValue().toString());
        String start = startTimeField.getText().isBlank() ? "-" : startTimeField.getText();
        String end = endTimeField.getText().isBlank() ? "-" : endTimeField.getText();
        summaryTime.setText(start + " - " + end);
        summaryPurpose.setText(purposeField.getText().isBlank() ? "-" : purposeField.getText());
    }

    /** Same booking submission logic as before: validate, call book_resource, then persist locally. */
    private void handleConfirmBooking() {
        String error = validate();
        if (error != null) {
            statusLabel.setText(error);
            return;
        }

        LocalDate date = datePicker.getValue();
        LocalTime start = LocalTime.parse(startTimeField.getText().trim());
        LocalTime end = LocalTime.parse(endTimeField.getText().trim());

        confirmBookingButton.setDisable(true);
        statusLabel.setText("Submitting booking...");

        worker.submit(() -> {
            try {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("resourceId", resourceId);
                args.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                args.put("startTime", start.toString());
                args.put("endTime", end.toString());
                args.put("studentId", studentId);

                String resultText = mcpClient.callTool("book_resource", args);
                String bookingRef = extractBookingReference(resultText);

                Booking booking = new Booking(
                        bookingRef, resourceId, facilityName, studentId,
                        date, start, end, Booking.Status.UPCOMING,
                        java.time.LocalDateTime.now().toString()
                );
                bookingStorage.save(booking);

                Platform.runLater(() -> {
                    statusLabel.setText("Booking confirmed. Reference: " + bookingRef);
                    confirmBookingButton.setDisable(false);
                    if (onBookingConfirmed != null) onBookingConfirmed.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Booking failed: " + ex.getMessage());
                    confirmBookingButton.setDisable(false);
                });
            }
        });
    }

    private String extractBookingReference(String toolResultText) {
        if (toolResultText != null) {
            Matcher m = BOOKING_REF_PATTERN.matcher(toolResultText);
            if (m.find()) return m.group();
        }
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String validate() {
        if (startTimeField.getText().isBlank() || endTimeField.getText().isBlank()) {
            return "Please enter start and end time.";
        }
        try {
            LocalTime start = LocalTime.parse(startTimeField.getText().trim());
            LocalTime end = LocalTime.parse(endTimeField.getText().trim());
            if (!end.isAfter(start)) {
                return "End time must be after start time.";
            }
        } catch (Exception e) {
            return "Time must be in HH:mm format, e.g. 10:00.";
        }
        if (datePicker.getValue() == null) {
            return "Please select a date.";
        }
        if (datePicker.getValue().isBefore(LocalDate.now())) {
            return "Booking date cannot be in the past.";
        }
        if (purposeField.getText().isBlank()) {
            return "Please enter a purpose for the booking.";
        }
        String people = numberOfPeopleField.getText().trim();
        if (people.isBlank() || !people.matches("\\d+") || Integer.parseInt(people) <= 0) {
            return "Number of people must be a positive number.";
        }
        return null;
    }
}