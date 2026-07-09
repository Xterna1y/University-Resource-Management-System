package com.campus.client.ui.pages.facilities;

import com.campus.client.services.mcp.CampusMcpClient;
import com.campus.client.ui.components.Theme;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * FaciView - FR05: browse bookable campus facilities, read from the
 * campus://facilities MCP resource.
 *
 * Parses the "[Bookable Resources]" pipe table (ROOM | TYPE | CAPACITY |
 * BUILDING | OPEN | CLOSE) into a proper table, based on the real file
 * format. Same MCP call as before (readResource("campus://facilities"));
 * only how the result is displayed has changed.
 */
public class FaciView extends BorderPane {

    /** One row of the [Bookable Resources] table. */
    public record FacilityRow(String room, String type, String capacity, String building,
                               String open, String close) {
        String typeLabel() {
            return switch (type) {
                case "discussion_room" -> "Discussion Room";
                case "computer_lab" -> "Computer Lab";
                case "study_pod" -> "Study Pod";
                case "group_study_room" -> "Group Study Room";
                case "sports_court", "basketball_court" -> "Sports Facility";
                default -> type;
            };
        }
    }

    private final CampusMcpClient mcpClient;
    private final ExecutorService worker;

    private final TextField searchField = new TextField();
    private final TableView<FacilityRow> table = new TableView<>();
    private final Label statusLabel = new Label();
    private final Button checkAvailabilityButton = new Button("Check Availability");
    private final Button backButton = new Button("Back");

    private List<FacilityRow> allRows = List.of();
    private Consumer<String> onCheckAvailability; // passes the selected room's building code
    private Runnable onBack;

    public FaciView(CampusMcpClient mcpClient, ExecutorService worker) {
        this.mcpClient = mcpClient;
        this.worker = worker;

        setPadding(new Insets(20));
        setTop(buildHeader());
        setCenter(buildBody());
        setBottom(buildActions());

        loadFacilities();
    }

    public void setOnCheckAvailability(Consumer<String> callback) { this.onCheckAvailability = callback; }
    public void setOnBack(Runnable r) { this.onBack = r; }

    private VBox buildHeader() {
        Label title = new Label("Browse Campus Facilities");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label subtitle = new Label("Search and select a facility to check its availability.");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        searchField.setPromptText("Search by room, type, or building...");
        searchField.setPrefHeight(36);
        searchField.textProperty().addListener((o, oldVal, newVal) -> applyFilter(newVal));

        VBox box = new VBox(8, title, subtitle, searchField);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    @SuppressWarnings("unchecked")
    private VBox buildBody() {
        TableColumn<FacilityRow, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().room()));

        TableColumn<FacilityRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().typeLabel()));

        TableColumn<FacilityRow, String> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().capacity()));

        TableColumn<FacilityRow, String> buildingCol = new TableColumn<>("Building");
        buildingCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().building()));

        TableColumn<FacilityRow, String> hoursCol = new TableColumn<>("Opening Hours");
        hoursCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().open() + " - " + d.getValue().close()));

        table.getColumns().setAll(List.of(roomCol, typeCol, capacityCol, buildingCol, hoursCol));
        table.setPlaceholder(new Label("Loading facilities..."));
        table.setPrefHeight(420);
        table.setStyle(Theme.card());

        VBox box = new VBox(10, table);
        return box;
    }

    private HBox buildActions() {
        checkAvailabilityButton.setStyle(Theme.primaryButton());
        backButton.setStyle(Theme.secondaryButton());

        checkAvailabilityButton.setOnAction(e -> {
            FacilityRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Please select a facility.");
                return;
            }
            if (onCheckAvailability != null) {
                onCheckAvailability.accept(selected.building());
            }
        });

        backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });

        HBox buttons = new HBox(10, checkAvailabilityButton, backButton);
        buttons.setPadding(new Insets(15, 0, 0, 0));

        VBox wrapper = new VBox(8, buttons, statusLabel);
        return new HBox(wrapper);
    }

    private void loadFacilities() {
        statusLabel.setText("Loading...");
        worker.submit(() -> {
            try {
                String text = mcpClient.readResource("campus://facilities");
                List<FacilityRow> rows = parseBookableResources(text);
                Platform.runLater(() -> {
                    allRows = rows;
                    table.setItems(FXCollections.observableArrayList(rows));
                    table.setPlaceholder(new Label("No facilities found."));
                    statusLabel.setText(rows.size() + " facilities loaded.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Could not load facilities: " + ex.getMessage()));
            }
        });
    }

    /**
     * Parses the "[Bookable Resources]" section of facilities.txt:
     * a header line (ROOM | TYPE | CAPACITY | BUILDING | OPEN | CLOSE)
     * followed by one pipe-delimited row per bookable room.
     */
    private List<FacilityRow> parseBookableResources(String text) {
        List<FacilityRow> rows = new ArrayList<>();
        if (text == null || text.isBlank()) return rows;

        String[] lines = text.split("\\n");
        boolean inTable = false;
        boolean headerSkipped = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.startsWith("[Bookable Resources]")) {
                inTable = true;
                headerSkipped = false;
                continue;
            }
            if (line.startsWith("[") && inTable) {
                break; // reached the next section
            }
            if (!inTable || line.isEmpty()) {
                continue;
            }
            if (!headerSkipped) {
                headerSkipped = true; // skip the "ROOM | TYPE | ..." header row
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length >= 6) {
                rows.add(new FacilityRow(
                        parts[0].trim(), parts[1].trim(), parts[2].trim(),
                        parts[3].trim(), parts[4].trim(), parts[5].trim()));
            }
        }
        return rows;
    }

    private void applyFilter(String query) {
        if (query == null || query.isBlank()) {
            table.setItems(FXCollections.observableArrayList(allRows));
            return;
        }
        String q = query.toLowerCase();
        List<FacilityRow> filtered = allRows.stream()
                .filter(r -> r.room().toLowerCase().contains(q)
                        || r.typeLabel().toLowerCase().contains(q)
                        || r.building().toLowerCase().contains(q))
                .toList();
        table.setItems(FXCollections.observableArrayList(filtered));
    }
}