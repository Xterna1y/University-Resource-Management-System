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
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * BookingView - lets a student confirm a booking for a facility selected on
 * FaciView/AvailableView, then submits it through the MCP "book_resource"
 * tool and records it locally via BookingStorage.
 *
 * Start/End time are now dropdowns generated from the facility's own
 * open/close hours (parsed out of the "ROOM | type | cap | building | open |
 * close" line passed in as facilityName), with any hour this student has
 * already booked for that resource+date removed from the choices. Cancelling
 * a booking in BHistory frees the slot again automatically, since these
 * dropdowns are rebuilt from live BookingStorage data every time this view
 * is opened rather than cached.
 *
 * NOTE: this only prevents a student double-booking themselves. The MCP
 * server's book_resource tool does not check for time-slot conflicts at
 * all, so two different students can still book overlapping times for the
 * same room - that would need a change on the server side to fix properly.
 */
public class BookingView extends BorderPane {

    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("[A-Z]{2}-\\d+");
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(21, 0);

    /** Parsed out of the raw "ROOM | type | capacity | building | open | close" line. */
    private record FacilityInfo(String resourceId, String type, String capacity, String building,
                                 LocalTime open, LocalTime close) {
    }

    private final CampusMcpClient mcpClient;
    private final ExecutorService worker;
    private final BookingStorage bookingStorage;
    private final String studentId;

    private final FacilityInfo facility;
    private final String facilityDisplayName;

    private final TextField resourceField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<LocalTime> startTimeCombo = new ComboBox<>();
    private final ComboBox<LocalTime> endTimeCombo = new ComboBox<>();
    private final TextField purposeField = new TextField();
    private final TextField numberOfPeopleField = new TextField();

    private final Label summaryFacility = new Label();
    private final Label summaryDate = new Label();
    private final Label summaryTime = new Label();
    private final Label summaryPurpose = new Label();
    private final Label statusLabel = new Label();
    private final Label noSlotsLabel = new Label();
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

        this.facility = parseFacility(facilityName, resourceId);
        this.facilityDisplayName = buildDisplayName(facility, facilityName);

        setPadding(new Insets(20));
        setStyle("-fx-background-color:" + Theme.GREY_BG + ";");
        setTop(buildHeader());
        setCenter(buildBody());

        resourceField.setText(facilityDisplayName);
        resourceField.setEditable(false);

        setupTimeCombos();

        datePicker.valueProperty().addListener((o, a, b) -> { refreshTimeOptions(); refreshSummary(); });
        startTimeCombo.valueProperty().addListener((o, a, b) -> { refreshEndOptions(); refreshSummary(); });
        endTimeCombo.valueProperty().addListener((o, a, b) -> refreshSummary());
        purposeField.textProperty().addListener((o, a, b) -> refreshSummary());
        numberOfPeopleField.textProperty().addListener((o, a, b) -> refreshSummary());

        refreshTimeOptions();
        refreshSummary();

        confirmBookingButton.setOnAction(e -> handleConfirmBooking());
        cancelButton.setOnAction(e -> {
            if (onBackToAvailability != null) onBackToAvailability.run();
        });
    }

    public void setOnBackToAvailability(Runnable r) { this.onBackToAvailability = r; }
    public void setOnBookingConfirmed(Runnable r) { this.onBookingConfirmed = r; }

    /**
     * Parses "RESOURCEID | type | capacity | building | open | close" - the raw line
     * FaciView/AvailableView pass through. Falls back to the constructor's resourceId
     * (ignoring the placeholder "AUTO") and default 08:00-21:00 hours if the text
     * doesn't match that shape, so this never crashes on unexpected input.
     */
    private FacilityInfo parseFacility(String raw, String fallbackResourceId) {
        if (raw != null) {
            String[] p = raw.split("\\|");
            for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
            if (p.length >= 6) {
                try {
                    return new FacilityInfo(p[0], p[1], p[2], p[3],
                            LocalTime.parse(p[4]), LocalTime.parse(p[5]));
                } catch (Exception ignored) {
                    // fall through to the default below
                }
            }
        }
        String id = (fallbackResourceId == null || fallbackResourceId.isBlank() || fallbackResourceId.equals("AUTO"))
                ? "" : fallbackResourceId;
        return new FacilityInfo(id, "Facility", "-", "-", DEFAULT_OPEN, DEFAULT_CLOSE);
    }

    private String buildDisplayName(FacilityInfo f, String raw) {
        if (f.resourceId().isBlank()) {
            return raw == null ? "Selected facility" : raw;
        }
        String typeLabel = switch (f.type()) {
            case "discussion_room" -> "Discussion Room";
            case "computer_lab" -> "Computer Lab";
            case "study_pod" -> "Study Pod";
            case "group_study_room" -> "Group Study Room";
            default -> f.type();
        };
        return f.resourceId() + " - " + typeLabel
    + " (Building " + f.building()
    + " - capacity " + f.capacity() + ")";
    }

    private VBox buildHeader() {
        Label back = new Label("\u2190 Back to Availability");
        back.setStyle("-fx-text-fill:" + Theme.DARK + "; -fx-cursor: hand;");
        back.setOnMouseClicked(e -> { if (onBackToAvailability != null) onBackToAvailability.run(); });

        Label title = new Label("Book Resource");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        Label subtitle = new Label("Review your booking details");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        VBox box = new VBox(6, back, title, subtitle);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    private HBox buildBody() {
        VBox detailsCard = buildDetailsCard();
        VBox summaryCard = buildSummaryCard();

        HBox.setHgrow(detailsCard, Priority.ALWAYS);
        HBox row = new HBox(20, detailsCard, summaryCard);
        return row;
    }

    private void setupTimeCombos() {
        StringConverter<LocalTime> converter = new StringConverter<>() {
            @Override public String toString(LocalTime t) { return t == null ? "" : t.toString(); }
            @Override public LocalTime fromString(String s) { return LocalTime.parse(s); }
        };
        startTimeCombo.setConverter(converter);
        endTimeCombo.setConverter(converter);
        startTimeCombo.setPromptText("Select start time");
        endTimeCombo.setPromptText("Select end time");
        startTimeCombo.setMaxWidth(Double.MAX_VALUE);
        endTimeCombo.setMaxWidth(Double.MAX_VALUE);

        noSlotsLabel.setStyle("-fx-text-fill:" + Theme.RED + ";");
        noSlotsLabel.setWrapText(true);
        noSlotsLabel.setVisible(false);
        noSlotsLabel.setManaged(false);
    }

    private VBox buildDetailsCard() {
        Label cardTitle = new Label("Booking Details");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        cardTitle.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(15, 0, 15, 0));

        int row = 0;
        grid.addRow(row++, new Label("Facility:"), resourceField);
        grid.addRow(row++, new Label("Date:"), datePicker);
        grid.addRow(row++, new Label("Start Time:"), startTimeCombo);
        grid.addRow(row++, new Label("End Time:"), endTimeCombo);

        purposeField.setPromptText("e.g. Group project discussion (optional)");
        grid.addRow(row++, new Label("Purpose:"), purposeField);

        numberOfPeopleField.setPromptText("e.g. 4");
        grid.addRow(row++, new Label("No. of People:"), numberOfPeopleField);

        for (Node n : grid.getChildren()) {
            if (n instanceof TextField || n instanceof DatePicker || n instanceof ComboBox) {
                GridPane.setHgrow(n, Priority.ALWAYS);
                ((Region) n).setPrefWidth(260);
            }
        }

        confirmBookingButton.setStyle(Theme.primaryButton());
        cancelButton.setStyle(Theme.secondaryButton());
        statusLabel.setWrapText(true);

        HBox actions = new HBox(10, confirmBookingButton, cancelButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, cardTitle, grid, noSlotsLabel, actions, statusLabel);
        card.setPadding(new Insets(20));
        card.setStyle(Theme.card());
        return card;
    }

    private VBox buildSummaryCard() {
        Label cardTitle = new Label("Booking Summary");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        cardTitle.setStyle("-fx-text-fill:" + Theme.DARK + ";");

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
        l.setStyle("-fx-text-fill:" + Theme.DARK + ";");
        return l;
    }

    /**
     * All hourly slots between facility.open() and facility.close(), minus any hour this
     * student already has an UPCOMING booking covering for this resource on this date.
     */
    private void refreshTimeOptions() {
        LocalDate date = datePicker.getValue();
        Set<LocalTime> blocked = blockedStartTimes(date);

        List<LocalTime> allStarts = generateHourlySlots(facility.open(), facility.close());
        List<LocalTime> available = allStarts.stream()
                .filter(t -> !blocked.contains(t))
                .collect(Collectors.toList());

        LocalTime previousStart = startTimeCombo.getValue();
        startTimeCombo.getItems().setAll(available);
        if (available.contains(previousStart)) {
            startTimeCombo.setValue(previousStart);
        } else {
            startTimeCombo.setValue(null);
            endTimeCombo.getItems().clear();
            endTimeCombo.setValue(null);
        }

        boolean noneLeft = available.isEmpty();
        noSlotsLabel.setVisible(noneLeft);
        noSlotsLabel.setManaged(noneLeft);
        noSlotsLabel.setText(noneLeft
                ? "You already have bookings covering every slot for this facility on this date."
                : "");

        refreshEndOptions();
    }

    /**
     * End-time options: every hourly mark after the chosen start, up to closing time,
     * stopping as soon as extending further would swallow an hour this student already
     * has booked (so you can't select an end time that overlaps an existing booking).
     */
    private void refreshEndOptions() {
        LocalTime start = startTimeCombo.getValue();
        LocalTime previousEnd = endTimeCombo.getValue();
        endTimeCombo.getItems().clear();

        if (start == null) {
            endTimeCombo.setValue(null);
            return;
        }

        Set<LocalTime> blocked = blockedStartTimes(datePicker.getValue());
        List<LocalTime> ends = new ArrayList<>();
        LocalTime cursor = start.plusHours(1);
        while (!cursor.isAfter(facility.close())) {
            LocalTime priorHour = cursor.minusHours(1);
            if (blocked.contains(priorHour) && !priorHour.equals(start)) {
                break; // an hour inside this range is already booked - stop extending
            }
            ends.add(cursor);
            cursor = cursor.plusHours(1);
        }

        endTimeCombo.getItems().setAll(ends);
        endTimeCombo.setValue(ends.contains(previousEnd) ? previousEnd : (ends.isEmpty() ? null : ends.get(0)));
    }

    /** This student's own existing (non-cancelled) booking hours for this resource+date. */
    private Set<LocalTime> blockedStartTimes(LocalDate date) {
        if (date == null || facility.resourceId().isBlank()) {
            return Set.of();
        }
        return bookingStorage.loadForStudent(studentId).stream()
                .filter(b -> b.getStatus() != Booking.Status.CANCELLED)
                .filter(b -> facility.resourceId().equalsIgnoreCase(b.getResourceId()))
                .filter(b -> date.equals(b.getBookingDate()))
                .flatMap(b -> occupiedHours(b.getStartTime(), b.getEndTime()).stream())
                .collect(Collectors.toSet());
    }

    private List<LocalTime> occupiedHours(LocalTime start, LocalTime end) {
        List<LocalTime> hours = new ArrayList<>();
        LocalTime t = start;
        while (t.isBefore(end)) {
            hours.add(t);
            t = t.plusHours(1);
        }
        return hours;
    }

    private List<LocalTime> generateHourlySlots(LocalTime open, LocalTime close) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime t = open;
        while (t.isBefore(close)) {
            slots.add(t);
            t = t.plusHours(1);
        }
        return slots;
    }

    private void refreshSummary() {
        summaryFacility.setText(facilityDisplayName);
        summaryDate.setText(datePicker.getValue() == null ? "-" : datePicker.getValue().toString());
        String start = startTimeCombo.getValue() == null ? "-" : startTimeCombo.getValue().toString();
        String end = endTimeCombo.getValue() == null ? "-" : endTimeCombo.getValue().toString();
        summaryTime.setText(start + " - " + end);
        summaryPurpose.setText(purposeField.getText().isBlank() ? "-" : purposeField.getText());
    }

    /** Validate, call book_resource, then persist locally - same flow as before. */
    private void handleConfirmBooking() {
        String error = validate();
        if (error != null) {
            statusLabel.setText(error);
            return;
        }

        LocalDate date = datePicker.getValue();
        LocalTime start = startTimeCombo.getValue();
        LocalTime end = endTimeCombo.getValue();
        String resourceId = facility.resourceId();

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
                        bookingRef, resourceId, facilityDisplayName, studentId,
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
        if (facility.resourceId().isBlank()) {
            return "Could not determine which resource to book. Please go back and select a facility again.";
        }
        if (datePicker.getValue() == null) {
            return "Please select a date.";
        }
        if (datePicker.getValue().isBefore(LocalDate.now())) {
            return "Booking date cannot be in the past.";
        }
        if (startTimeCombo.getValue() == null || endTimeCombo.getValue() == null) {
            return "Please select a start and end time.";
        }
        if (!endTimeCombo.getValue().isAfter(startTimeCombo.getValue())) {
            return "End time must be after start time.";
        }
        // Defensive re-check against this student's own bookings (dropdowns should already
        // prevent this, but re-verify in case something changed between selections).
        Set<LocalTime> blocked = blockedStartTimes(datePicker.getValue());
        for (LocalTime hour : occupiedHours(startTimeCombo.getValue(), endTimeCombo.getValue())) {
            if (blocked.contains(hour)) {
                return "You already have a booking for this facility that overlaps this time.";
            }
        }
        if (purposeField.getText().isBlank()) {
            return "Please enter a purpose for the booking.";
        }
        String people = numberOfPeopleField.getText().trim();

        if (people.isBlank() || !people.matches("\\d+")) {
            return "Number of people must be a positive number.";
        }

        int numPeople = Integer.parseInt(people);

        if (numPeople <= 0) {
            return "Number of people must be greater than 0.";
        }

        int capacity;
        try {
            capacity = Integer.parseInt(facility.capacity());
        } catch (NumberFormatException e) {
            return "Unable to determine the facility capacity.";
        }

        if (numPeople > capacity) {
            return "This facility can accommodate a maximum of "
                    + capacity + " people.";
        }
        return null;
    }
}