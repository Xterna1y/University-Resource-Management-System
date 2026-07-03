package com.campus.client.ui.pages.booking;

import com.campus.client.data.BookingStorage;
import com.campus.client.model.Booking;
import com.campus.client.services.mcp.CampusMcpClient;

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
 * BookingView - lets a student confirm a booking for a facility that was
 * selected on AvailabilityView (Member 2's screen), then submits it through
 * the MCP "book_resource" tool and records it locally via BookingStorage.
 *
 * Covers FR07 (view summary before confirming) and FR08 (submit booking),
 * and Table 25 of the design report (Purpose, Number of People, Confirm,
 * Cancel).
 */
public class BookingView extends BorderPane {

    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("[A-Z]{2}-\\d+");

    private final CampusMcpClient mcpClient;
    private final ExecutorService worker;
    private final BookingStorage bookingStorage;
    private final String studentId;

    // Pre-filled context (usually supplied by AvailabilityView)
    private final String resourceId;
    private final String facilityName;

    private final TextField resourceField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField startTimeField = new TextField();
    private final TextField endTimeField = new TextField();
    private final TextField purposeField = new TextField();
    private final TextField numberOfPeopleField = new TextField();

    private final Label summaryLabel = new Label();
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
        setTop(buildHeader());
        setCenter(buildForm());
        setBottom(buildActions());

        resourceField.setText(facilityName);
        resourceField.setEditable(false);

        // Keep the summary in sync as the student edits the form.
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

    private Node buildHeader() {
        Label title = new Label("Book a Facility");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        return title;
    }

    private Node buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15, 0, 15, 0));

        int row = 0;
        grid.addRow(row++, new Label("Facility:"), resourceField);
        grid.addRow(row++, new Label("Date:"), datePicker);

        startTimeField.setPromptText("HH:mm e.g. 10:00");
        endTimeField.setPromptText("HH:mm e.g. 11:00");
        grid.addRow(row++, new Label("Start Time:"), startTimeField);
        grid.addRow(row++, new Label("End Time:"), endTimeField);

        purposeField.setPromptText("e.g. Group project discussion");
        grid.addRow(row++, new Label("Purpose:"), purposeField);

        numberOfPeopleField.setPromptText("e.g. 4");
        grid.addRow(row++, new Label("Number of People:"), numberOfPeopleField);

        for (Node n : grid.getChildren()) {
            if (n instanceof TextField || n instanceof DatePicker) {
                GridPane.setHgrow(n, Priority.ALWAYS);
                ((Region) n).setPrefWidth(260);
            }
        }

        VBox summaryBox = new VBox(5);
        summaryBox.setPadding(new Insets(10));
        summaryBox.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 6;");
        Label summaryTitle = new Label("Booking Summary");
        summaryTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        summaryLabel.setWrapText(true);
        summaryBox.getChildren().addAll(summaryTitle, summaryLabel);

        VBox container = new VBox(15, grid, summaryBox);
        return container;
    }

    private Node buildActions() {
        HBox box = new HBox(10, confirmBookingButton, cancelButton, statusLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(15, 0, 0, 0));
        statusLabel.setWrapText(true);
        return box;
    }

    private void refreshSummary() {
        summaryLabel.setText(String.format(
                "Facility: %s%nDate: %s%nTime: %s - %s%nPurpose: %s%nPeople: %s",
                facilityName,
                datePicker.getValue() == null ? "-" : datePicker.getValue(),
                startTimeField.getText().isBlank() ? "-" : startTimeField.getText(),
                endTimeField.getText().isBlank() ? "-" : endTimeField.getText(),
                purposeField.getText().isBlank() ? "-" : purposeField.getText(),
                numberOfPeopleField.getText().isBlank() ? "-" : numberOfPeopleField.getText()
        ));
    }

    /** FR08: validate input, then call the book_resource MCP tool on a background thread. */
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

    /** Pulls a reference like "BK-1023" out of the tool's free-text reply, or falls back to a local id. */
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
