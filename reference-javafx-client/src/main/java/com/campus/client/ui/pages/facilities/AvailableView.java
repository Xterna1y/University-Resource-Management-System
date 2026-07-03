package com.campus.client.ui.pages.facilities;

import com.campus.client.services.mcp.CampusMcpClient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

/**
 * AvailableView - FR06: check which rooms are free on a given date via the
 * check_room_availability MCP tool, then hand off to BookingView.
 *
 * The tool returns free-text results (room name + booked/free), so the
 * student reads them and types the Resource ID + Facility Name they want to
 * book into the small form below, rather than this screen guessing at a
 * parsed room ID from unstructured text.
 */
public class AvailableView extends BorderPane {

    private final CampusMcpClient mcpClient;
    private final ExecutorService worker;

    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField buildingField = new TextField();
    private final Button checkButton = new Button("Check Availability");
    private final TextArea resultsArea = new TextArea();

    private final TextField resourceIdField = new TextField();
    private final TextField facilityNameField = new TextField();
    private final Button proceedButton = new Button("Proceed to Booking");
    private final Button backButton = new Button("Back to Facilities");
    private final Label statusLabel = new Label();

    private BiConsumer<String, String> onProceedToBooking; // (resourceId, facilityName)
    private Runnable onBack;

    public AvailableView(CampusMcpClient mcpClient, ExecutorService worker, String prefillFacilityText) {
        this.mcpClient = mcpClient;
        this.worker = worker;

        setPadding(new Insets(20));
        setTop(buildHeader());
        setCenter(buildBody());
        setBottom(buildActions());

        if (prefillFacilityText != null && !prefillFacilityText.isBlank()) {
            // Use the first line of the selected facility block as a starting guess for the name.
            String firstLine = prefillFacilityText.split("\\n")[0].trim();
            facilityNameField.setText(firstLine);
        }

        checkButton.setOnAction(e -> handleCheck());
    }

    public void setOnProceedToBooking(BiConsumer<String, String> callback) { this.onProceedToBooking = callback; }
    public void setOnBack(Runnable r) { this.onBack = r; }

    private VBox buildHeader() {
        Label title = new Label("Check Room Availability");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        VBox box = new VBox(10, title);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    private VBox buildBody() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        buildingField.setPromptText("Building (optional)");
        form.addRow(0, new Label("Date:"), datePicker);
        form.addRow(1, new Label("Building:"), buildingField);
        form.add(checkButton, 1, 2);

        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);
        resultsArea.setPrefRowCount(10);
        resultsArea.setPromptText("Availability results will appear here.");

        GridPane proceedForm = new GridPane();
        proceedForm.setHgap(10);
        proceedForm.setVgap(10);
        proceedForm.setPadding(new Insets(10, 0, 0, 0));
        resourceIdField.setPromptText("e.g. HC-01 (from the results above)");
        facilityNameField.setPromptText("e.g. Harapan Discussion Room 1");
        proceedForm.addRow(0, new Label("Resource ID:"), resourceIdField);
        proceedForm.addRow(1, new Label("Facility Name:"), facilityNameField);

        Label hint = new Label("Read the Resource ID from the results above, then enter it here to book.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        VBox box = new VBox(15, form, resultsArea, hint, proceedForm);
        return box;
    }

    private javafx.scene.layout.HBox buildActions() {
        proceedButton.setOnAction(e -> handleProceed());
        backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });

        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(10, proceedButton, backButton, statusLabel);
        box.setPadding(new Insets(15, 0, 0, 0));
        return box;
    }

    private void handleCheck() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            statusLabel.setText("Please select a date.");
            return;
        }

        checkButton.setDisable(true);
        statusLabel.setText("Checking availability...");

        worker.submit(() -> {
            try {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                String building = buildingField.getText().trim();
                if (!building.isEmpty()) {
                    args.put("building", building);
                }

                String result = mcpClient.callTool("check_room_availability", args);

                Platform.runLater(() -> {
                    resultsArea.setText(result);
                    statusLabel.setText("");
                    checkButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to check availability: " + ex.getMessage());
                    checkButton.setDisable(false);
                });
            }
        });
    }

    private void handleProceed() {
        String resourceId = resourceIdField.getText().trim();
        String facilityName = facilityNameField.getText().trim();

        if (resourceId.isEmpty() || facilityName.isEmpty()) {
            statusLabel.setText("Please enter both Resource ID and Facility Name.");
            return;
        }
        if (onProceedToBooking != null) {
            onProceedToBooking.accept(resourceId, facilityName);
        }
    }
}