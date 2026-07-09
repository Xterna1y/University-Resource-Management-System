package com.campus.client.ui.pages.facilities;

import com.campus.client.services.mcp.CampusMcpClient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
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
    private final ListView<String> availabilityList = new ListView<>();

    private final Label facilityIdLabel = new Label("-");
    private final Label facilityTypeLabel = new Label("-");
    private final Label capacityLabel = new Label("-");
    private final Label buildingLabel = new Label("-");

    // Selected facility information
    private String selectedResourceId = "";
    private String selectedFacilityName = "";

    private final Button proceedButton = new Button("Book This Slot");
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
            facilityIdLabel.setText("-");
            facilityTypeLabel.setText("-");
            capacityLabel.setText("-");
            buildingLabel.setText("-");
        }

        checkButton.setOnAction(e -> handleCheck());
        availabilityList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> {

            System.out.println("Clicked = " + newValue);

            if (newValue == null)
                return;

            String line = newValue
                    .replace("🟢", "")
                    .replace("🔴", "")
                    .trim();

            System.out.println(line);

            String[] parts = line.split("\\s+");

            System.out.println("Length = " + parts.length);

            for (int i = 0; i < parts.length; i++) {
                System.out.println(i + " = " + parts[i]);
            }

            if (parts.length >= 5) {
                selectedResourceId = parts[0];
                selectedFacilityName = parts[1];

                facilityIdLabel.setText(selectedResourceId);
                facilityTypeLabel.setText(selectedFacilityName);
                capacityLabel.setText(parts[3]);
                buildingLabel.setText(parts[4]);
            }
        });
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

        GridPane searchPane = new GridPane();
        searchPane.setHgap(10);
        searchPane.setVgap(10);

        buildingField.setPromptText("Building (optional)");

        searchPane.addRow(0,
                new Label("Date:"),
                datePicker);

        searchPane.addRow(1,
                new Label("Building:"),
                buildingField);

        searchPane.add(checkButton,1,2);

        availabilityList.setPrefHeight(350);

        VBox left = new VBox(10,
                new Label("Available Time Slots"),
                availabilityList);

        GridPane details = new GridPane();

        details.setHgap(10);
        details.setVgap(10);

        details.addRow(0,new Label("Facility ID:"),facilityIdLabel);
        details.addRow(1,new Label("Type:"),facilityTypeLabel);
        details.addRow(2,new Label("Capacity:"),capacityLabel);
        details.addRow(3,new Label("Building:"),buildingLabel);

        TitledPane infoPane = new TitledPane();
        infoPane.setText("Facility Details");
        infoPane.setContent(details);
        infoPane.setExpanded(true);

        HBox content = new HBox(20,left,infoPane);

        VBox page = new VBox(20);

        page.getChildren().addAll(searchPane,content);

        return page;
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
                    availabilityList.getItems().clear();
                    String[] lines = result.split("\\n");
                    for (String line : lines) {

                        line = line.trim();

                        if (line.isBlank())
                            continue;

                        if (line.startsWith("Availability"))
                            continue;

                        if (line.contains("-> free")) {
                            availabilityList.getItems().add("🟢 " + line);
                        }
                        else {
                            availabilityList.getItems().add("🔴 " + line);
                        }
                    }
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

        String selected = availabilityList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            statusLabel.setText("Please select a facility.");
            return;
        }

        if (selected.contains("HAS BOOKING")) {
            statusLabel.setText("This resource is already booked.");
            return;
        }

        selected = selected.replace("🟢", "")
                        .replace("🔴", "")
                        .trim();

        String[] parts = selected.split("\\s+");

        if (parts.length < 5) {
            statusLabel.setText("Invalid facility information.");
            return;
        }

        String resourceId = parts[0];
        String facilityName = parts[1];

        if (onProceedToBooking != null) {
            onProceedToBooking.accept(resourceId, facilityName);
        }
    }
}