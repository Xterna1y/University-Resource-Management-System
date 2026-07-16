package com.campus.client.ui.pages.facilities;

import com.campus.client.services.mcp.CampusMcpClient;

import com.campus.client.ui.components.Theme;
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
    private final ComboBox<String> buildingField = new ComboBox<>();
    private final Button checkButton = new Button("Check Availability");
    private final ListView<String> availabilityList = new ListView<>();

    // Facility Details
    private final Label facilityIdLabel = new Label("-");
    private final Label facilityTypeLabel = new Label("-");
    private final Label capacityLabel = new Label("-");
    private final Label buildingLabel = new Label("-");

    // Selected facility
    private String selectedResourceId = "";
    private String selectedFacilityName = "";

    private final Button proceedButton = new Button("Continue to Booking");
    private final Button backButton = new Button("Back");
    private final Label statusLabel = new Label();

    private BiConsumer<String, String> onProceedToBooking;
    private Runnable onBack;

    public AvailableView(CampusMcpClient mcpClient,
                         ExecutorService worker,
                         String prefillFacilityText) {

        this.mcpClient = mcpClient;
        this.worker = worker;

        setPadding(new Insets(20));
        setStyle("-fx-background-color:" + Theme.GREY_BG + ";");

        setTop(buildHeader());
        setCenter(buildBody());
        setBottom(buildActions());

        // Store the facility selected from the previous page
        if (prefillFacilityText != null && !prefillFacilityText.isBlank()) {

            selectedFacilityName = prefillFacilityText.split("\\n")[0].trim();

            facilityIdLabel.setText("AUTO");
            facilityTypeLabel.setText("Facility");
            capacityLabel.setText("-");
            buildingLabel.setText("-");
        }

        checkButton.setOnAction(e -> handleCheck());

        availabilityList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> {

                    if (newValue == null) {
                        proceedButton.setDisable(true);
                        return;
                    }

                    // Don't allow booking unavailable resources
                    proceedButton.setDisable(newValue.contains("🔴"));

                    selectedResourceId = "AUTO";

                    facilityIdLabel.setText(selectedResourceId);


                    String building = buildingField.getValue();

                    buildingLabel.setText(
                            building == null || building.equals("All")
                                    ? "-"
                                    : building
                    );

                    String lower = selectedFacilityName.toLowerCase();

                    if (lower.contains("discussion")) {
                        facilityTypeLabel.setText("Discussion Room");
                        capacityLabel.setText("6");
                    } else if (lower.contains("computer")) {
                        facilityTypeLabel.setText("Computer Lab");
                        capacityLabel.setText("30");
                    } else if (lower.contains("study")) {
                        facilityTypeLabel.setText("Study Pod");
                        capacityLabel.setText("2");
                    } else if (lower.contains("basket")
                            || lower.contains("court")
                            || lower.contains("sport")) {

                        facilityTypeLabel.setText("Sports Facility");
                        capacityLabel.setText("20");
                    } else {
                        facilityTypeLabel.setText("Facility");
                        capacityLabel.setText("-");
                    }
                });
    }

    public void setOnProceedToBooking(BiConsumer<String, String> callback) {
        this.onProceedToBooking = callback;
    }

    public void setOnBack(Runnable r) {
        this.onBack = r;
    }

    private VBox buildHeader() {

        Label back = new Label("← Back to Facilities");
        back.setStyle("-fx-text-fill:" + Theme.DARK + "; -fx-cursor: hand;");
        back.setOnMouseClicked(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        Label title = new Label("Check Availability");
        title.setStyle(Theme.title());

        Label subtitle = new Label("Find an available room before making a booking");
        subtitle.setStyle(Theme.subtitle());

        VBox box = new VBox(6, back, title, subtitle);
        box.setPadding(new Insets(0, 0, 15, 0));

        return box;
    }

    private VBox buildBody() {

        // ================= Search Card =================
        Label searchTitle = new Label("Availability Search");
        searchTitle.setStyle(
                "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + Theme.DARK + ";"
        );

        GridPane searchPane = new GridPane();
        searchPane.setHgap(10);
        searchPane.setVgap(12);
        searchPane.setPadding(new Insets(15, 0, 15, 0));

        buildingField.setPromptText("Select Building");

        buildingField.getItems().addAll(
                "All",
                "D",
                "E",
                "LAB",
                "LIB",
                "OUT"
        );

        buildingField.getSelectionModel().selectFirst();

        GridPane.setHgrow(datePicker, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(buildingField, javafx.scene.layout.Priority.ALWAYS);

        searchPane.addRow(0, new Label("Date"), datePicker);
        searchPane.addRow(1, new Label("Building"), buildingField);

        checkButton.setStyle(Theme.primaryButton());

        VBox searchCard = new VBox(10,
                searchTitle,
                searchPane,
                checkButton);

        searchCard.setPadding(new Insets(20));
        searchCard.setStyle(Theme.card());

        // Available Slots

        Label slotsTitle = new Label("Available Time Slots");
        slotsTitle.setStyle(
                "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + Theme.DARK + ";"
        );

        availabilityList.setPrefHeight(320);
        availabilityList.setStyle(
                "-fx-control-inner-background:white;" +
                        "-fx-selection-bar:#E8F0FE;" +
                        "-fx-selection-bar-text:black;" +
                        "-fx-text-fill:black;"
        );

        Label empty = new Label("Select a building and date, then click Check Availability.");
        empty.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");
        availabilityList.setPlaceholder(empty);

        availabilityList.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                VBox card = new VBox(6);
                card.setPadding(new Insets(12));
                card.setStyle(
                        "-fx-background-color:white;" +
                                "-fx-border-color:" + Theme.GREY_BORDER + ";" +
                                "-fx-background-radius:8;" +
                                "-fx-border-radius:8;"
                );

                String icon = "🟢";
                String status = "Available";

                if (item.contains("🔴")) {
                    icon = "🔴";
                    status = "Booked";
                }

                String text = item
                        .replace("🟢", "")
                        .replace("🔴", "")
                        .trim();

                Label title = new Label(icon + " " + text);
                title.setStyle(
                        "-fx-font-weight:bold;" +
                                "-fx-text-fill:" + Theme.DARK + ";" +
                                "-fx-font-size:13px;"
                );

                Label statusLabel = new Label(status);
                statusLabel.setStyle(
                        status.equals("Available")
                                ? Theme.statusPill(true)
                                : Theme.statusPill(false)
                );

                card.getChildren().addAll(title, statusLabel);

                setGraphic(card);

                availabilityList.setStyle(
                        "-fx-selection-bar:#E8F0FE;" +
                                "-fx-selection-bar-text:black;" +
                                "-fx-control-inner-background:white;"
                );
            }
        });

        VBox slotsCard = new VBox(10,
                slotsTitle,
                availabilityList);

        slotsCard.setPadding(new Insets(20));
        slotsCard.setStyle(Theme.card());

        HBox.setHgrow(slotsCard, javafx.scene.layout.Priority.ALWAYS);

        // ================= Facility Details =================

        Label detailsTitle = new Label("Facility Details");
        detailsTitle.setStyle(
                "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + Theme.DARK + ";"
        );

        GridPane details = new GridPane();
        details.setHgap(10);
        details.setVgap(12);

        details.addRow(0, boldLabel("Facility ID"), facilityIdLabel);
        details.addRow(1, boldLabel("Type"), facilityTypeLabel);
        details.addRow(2, boldLabel("Capacity"), capacityLabel);
        details.addRow(3, boldLabel("Building"), buildingLabel);

        VBox detailsCard = new VBox(10,
                detailsTitle,
                details);

        detailsCard.setPadding(new Insets(20));
        detailsCard.setStyle(Theme.card());
        detailsCard.setPrefWidth(260);

        // ================= Layout =================

        HBox cards = new HBox(20,
                slotsCard,
                detailsCard);

        VBox page = new VBox(20,
                searchCard,
                cards);

        return page;
    }

    private Label boldLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + Theme.DARK + ";"
        );
        return label;
    }

    private HBox buildActions() {

        proceedButton.setStyle(Theme.primaryButton());
        backButton.setStyle(Theme.secondaryButton());

        proceedButton.setOnAction(e -> handleProceed());

        backButton.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        statusLabel.setWrapText(true);

        HBox buttons = new HBox(10,
                proceedButton,
                backButton);

        VBox wrapper = new VBox(10,
                buttons,
                statusLabel);

        wrapper.setPadding(new Insets(15, 0, 0, 0));

        return new HBox(wrapper);
    }

    private void handleCheck() {

        LocalDate date = datePicker.getValue();

        if (date == null) {
            statusLabel.setText("Please select a date.");
            return;
        }

        checkButton.setDisable(true);
        proceedButton.setDisable(true);
        availabilityList.setDisable(true);

        statusLabel.setText("Checking availability...");

        worker.submit(() -> {

            try {

                Map<String, Object> args = new LinkedHashMap<>();
                args.put("date",
                        date.format(DateTimeFormatter.ISO_LOCAL_DATE));

                String building = buildingField.getValue();

                if (building != null && !building.equals("All")) {
                    args.put("building", building);
                }

                String result =
                        mcpClient.callTool("check_room_availability", args);

                Platform.runLater(() -> {

                    availabilityList.getItems().clear();

                    String[] lines = result.split("\\n");

                    for (String line : lines) {

                        line = line.trim();

                        if (line.isBlank()) {
                            continue;
                        }

                        // Ignore headings or separators from the MCP output
                        if (line.startsWith("[")
                                || line.startsWith("=")
                                || line.toLowerCase().contains("available resources")) {
                            continue;
                        }

                        availabilityList.getItems().add(line);
                    }

                    statusLabel.setText("");

                    checkButton.setDisable(false);
                    availabilityList.setDisable(false);
                    proceedButton.setDisable(true);

                });

            } catch (Exception ex) {

                Platform.runLater(() -> {

                    statusLabel.setText(
                            "Failed to check availability: "
                                    + ex.getMessage());

                    checkButton.setDisable(false);
                    availabilityList.setDisable(false);
                    proceedButton.setDisable(false);

                });

            }

        });
    }

    private void handleProceed() {

        if (availabilityList.getSelectionModel().getSelectedItem() == null) {
            statusLabel.setText("Please select an available time slot.");
            return;
        }

        if (onProceedToBooking != null) {
            onProceedToBooking.accept(
                    selectedResourceId,
                    selectedFacilityName
            );
        }
    }
}