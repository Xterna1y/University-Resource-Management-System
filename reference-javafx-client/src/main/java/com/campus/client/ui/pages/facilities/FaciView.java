package com.campus.client.ui.pages.facilities;

import com.campus.client.services.mcp.CampusMcpClient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * FaciView - FR05: browse bookable campus facilities, read from the
 * campus://facilities MCP resource, with a simple text search filter.
 *
 * NOTE: the exact layout of facilities.txt wasn't available when this was
 * written, so entries are shown as raw text blocks (split on blank lines)
 * rather than parsed into strict fields. Tighten the parsing once you've
 * seen the real file, if you want structured columns instead.
 */
public class FaciView extends BorderPane {

    private final CampusMcpClient mcpClient;
    private final ExecutorService worker;

    private final TextField searchField = new TextField();
    private final ListView<String> resultsList = new ListView<>();
    private final Label statusLabel = new Label();
    private final Button checkAvailabilityButton = new Button("Check Availability");
    private final Button backButton = new Button("Back");

    private List<String> allEntries = List.of();
    private Consumer<String> onCheckAvailability; // passes the selected facility's raw text block
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
        Label title = new Label("Campus Facilities");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        searchField.setPromptText("Search by name, building, or type...");
        searchField.textProperty().addListener((o, a, b) -> applyFilter(b));

        VBox box = new VBox(10, title, searchField);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    private VBox buildBody() {
        resultsList.setPlaceholder(new Label("Loading facilities..."));
        VBox box = new VBox(10, resultsList);
        return box;
    }

    private HBox buildActions() {
        checkAvailabilityButton.setOnAction(e -> {
            String selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null && onCheckAvailability != null) {
                onCheckAvailability.accept(selected);
            } else if (selected == null) {
                statusLabel.setText("Select a facility first.");
            }
        });
        backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });

        HBox box = new HBox(10, checkAvailabilityButton, backButton, statusLabel);
        box.setPadding(new Insets(15, 0, 0, 0));
        return box;
    }

    private void loadFacilities() {
        statusLabel.setText("Loading...");
        worker.submit(() -> {
            try {
                String text = mcpClient.readResource("campus://facilities");
                List<String> entries = splitIntoEntries(text);
                Platform.runLater(() -> {
                    allEntries = entries;
                    resultsList.setItems(javafx.collections.FXCollections.observableArrayList(entries));
                    resultsList.setPlaceholder(new Label("No facilities found."));
                    statusLabel.setText(entries.size() + " facilities loaded.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Could not load facilities: " + ex.getMessage()));
            }
        });
    }

    /** Splits the resource's raw text into readable chunks, one per blank-line-separated block. */
    private List<String> splitIntoEntries(String text) {
        if (text == null || text.isBlank()) return List.of();
        return List.of(text.split("\\n\\s*\\n")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void applyFilter(String query) {
        if (query == null || query.isBlank()) {
            resultsList.setItems(javafx.collections.FXCollections.observableArrayList(allEntries));
            return;
        }
        String q = query.toLowerCase();
        List<String> filtered = allEntries.stream()
                .filter(e -> e.toLowerCase().contains(q))
                .collect(Collectors.toList());
        resultsList.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
    }
}