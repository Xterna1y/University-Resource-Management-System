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
@SuppressWarnings("unused")
public class FaciView extends BorderPane {

    private final CampusMcpClient mcpClient;
    private final ExecutorService worker;
    private final Label notesLabel = new Label();
    private final TextField searchField = new TextField();
    private final ListView<String> resultsList = new ListView<>();

    private final Label statusLabel = new Label();

    private final Button checkAvailabilityButton =
            new Button("Check Availability");

    private final Button backButton =
            new Button("Back");

    private List<String> allEntries = List.of();
    private String notesText = "";

    private Consumer<String> onCheckAvailability;
    private Runnable onBack;
    private String facilitiesText = "";

    public FaciView(CampusMcpClient mcpClient,
                    ExecutorService worker) {

        this.mcpClient = mcpClient;
        this.worker = worker;

        setPadding(new Insets(20));
        setStyle("-fx-background-color:" + Theme.GREY_BG + ";");

        setTop(buildHeader());
        setCenter(buildBody());
        setBottom(buildActions());

        checkAvailabilityButton.setDisable(true);

        resultsList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, newValue) ->
                        checkAvailabilityButton.setDisable(newValue == null));

        loadFacilities();
    }

    public void setOnCheckAvailability(Consumer<String> callback) {
        this.onCheckAvailability = callback;
    }

    public void setOnBack(Runnable r) {
        this.onBack = r;
    }

    private VBox buildHeader() {

        Label back = new Label("← Back");
        back.setStyle("-fx-text-fill:" + Theme.DARK + "; -fx-cursor: hand;");

        back.setOnMouseClicked(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        Label title = new Label("Browse Campus Facilities");
        title.setStyle(Theme.title());

        Label subtitle = new Label(
                "Search and select a facility to check availability."
        );
        subtitle.setStyle(Theme.subtitle());

        searchField.setPromptText(
                "Search by facility name, building or type..."
        );

        searchField.setPrefHeight(36);

        searchField.textProperty().addListener(
                (o, oldVal, newVal) -> applyFilter(newVal));

        VBox box = new VBox(8,
                back,
                title,
                subtitle,
                searchField);

        box.setPadding(new Insets(0, 0, 15, 0));

        return box;
    }

    private VBox buildBody() {

        Label heading = new Label("Available Facilities");
        heading.setStyle(
                "-fx-font-size:16;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + Theme.DARK + ";"
        );

        resultsList.setPrefHeight(520);

        resultsList.setStyle(
                "-fx-background-color:white;" +
                        "-fx-control-inner-background:white;" +
                        "-fx-selection-bar:#E8E8E8;" +
                        "-fx-selection-bar-non-focused:#F0F0F0;"
        );

        resultsList.setPlaceholder(
                new Label("Loading facilities...")
        );

        resultsList.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                setText(null);

                VBox card = new VBox(6);
                card.setPadding(new Insets(15));

                card.setStyle(
                        "-fx-background-color:white;" +
                                "-fx-border-color:" + Theme.GREY_BORDER + ";" +
                                "-fx-background-radius:8;" +
                                "-fx-border-radius:8;"
                );

                String[] lines = item.split("\\n");

                String firstLine = lines.length > 0 ? lines[0] : "";

                String icon = "🏢";

                String lower = firstLine.toLowerCase();

                if (lower.contains("computer"))
                    icon = "💻";
                else if (lower.contains("discussion"))
                    icon = "👥";
                else if (lower.contains("study"))
                    icon = "📚";
                else if (lower.contains("sport") || lower.contains("court"))
                    icon = "🏀";

                Label title = new Label(icon + " " + firstLine);
                title.setStyle(
                        "-fx-font-size:14;" +
                                "-fx-font-weight:bold;" +
                                "-fx-text-fill:" + Theme.DARK + ";"
                );

                VBox details = new VBox(4);

                for (int i = 1; i < lines.length; i++) {

                    Label l = new Label(lines[i]);

                    l.setWrapText(true);

                    l.setStyle(
                            "-fx-text-fill:" + Theme.TEXT_MUTED + ";" +
                                    "-fx-font-size:12;"
                    );

                    details.getChildren().add(l);
                }

                card.getChildren().addAll(title, details);

                setGraphic(card);

                /* Fixes white text when selected */
                selectedProperty().addListener((obs, wasSelected, isSelected) -> {

                    if (isSelected) {
                        card.setStyle(
                                "-fx-background-color:#F5F5F5;" +
                                        "-fx-border-color:" + Theme.RED + ";" +
                                        "-fx-background-radius:8;" +
                                        "-fx-border-radius:8;"
                        );
                    } else {
                        card.setStyle(
                                "-fx-background-color:white;" +
                                        "-fx-border-color:" + Theme.GREY_BORDER + ";" +
                                        "-fx-background-radius:8;" +
                                        "-fx-border-radius:8;"
                        );
                    }

                    title.setStyle(
                            "-fx-font-size:14;" +
                                    "-fx-font-weight:bold;" +
                                    "-fx-text-fill:" + Theme.DARK + ";"
                    );
                });
            }
        });

        Label notesTitle = new Label("Notes");
        notesTitle.setStyle(
                "-fx-font-size:14;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + Theme.DARK + ";"
        );

        notesLabel.setWrapText(true);
        notesLabel.setStyle(
                "-fx-text-fill:" + Theme.TEXT_MUTED + ";"
        );

        VBox notesCard = new VBox(8,
                notesTitle,
                notesLabel);

        notesCard.setPadding(new Insets(15));
        notesCard.setStyle(Theme.card());

        VBox card = new VBox(15,
                heading,
                resultsList,
                notesCard);

        card.setPadding(new Insets(20));
        card.setStyle(Theme.card());

        return new VBox(card);
    }

    private HBox buildActions() {

        checkAvailabilityButton.setStyle(Theme.primaryButton());
        backButton.setStyle(Theme.secondaryButton());

        checkAvailabilityButton.setOnAction(e -> {

            String selected = resultsList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                statusLabel.setStyle("-fx-text-fill:" + Theme.RED + ";");
                statusLabel.setText("Please select a facility.");
                return;
            }

            if (onCheckAvailability != null) {
                onCheckAvailability.accept(selected);
            }
        });

        backButton.setOnAction(e -> {

            if (onBack != null) {
                onBack.run();
            }

        });

        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        HBox buttons = new HBox(10,
                checkAvailabilityButton,
                backButton);

        VBox wrapper = new VBox(10,
                buttons,
                statusLabel);

        wrapper.setPadding(new Insets(15, 0, 0, 0));

        return new HBox(wrapper);
    }

    private void loadFacilities() {

        statusLabel.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");
        statusLabel.setText("Loading facilities...");

        resultsList.setDisable(true);

        worker.submit(() -> {

            try {

                String text = mcpClient.readResource("campus://facilities");

                facilitiesText = text;

                List<String> entries = splitIntoEntries(text);

                Platform.runLater(() -> {

                    allEntries = entries;
                    notesLabel.setText(notesText);

                    resultsList.setItems(
                            FXCollections.observableArrayList(entries));

                    resultsList.setDisable(false);

                    resultsList.setPlaceholder(
                            new Label("No facilities found.")
                    );

                    statusLabel.setStyle("-fx-text-fill:" + Theme.GREEN_TEXT + ";");
                    statusLabel.setText(entries.size() + " section(s) loaded.");

                });

            } catch (Exception ex) {

                Platform.runLater(() -> {

                    resultsList.setDisable(false);

                    resultsList.setPlaceholder(
                            new Label("Unable to load facilities.")
                    );

                    statusLabel.setStyle("-fx-text-fill:" + Theme.RED + ";");
                    statusLabel.setText(
                            "Could not load facilities:\n" + ex.getMessage());

                });

            }

        });

    }

    private List<String> splitIntoEntries(String text) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> facilities = new java.util.ArrayList<>();

        boolean insideResources = false;
        boolean insideNotes = false;

        StringBuilder notes = new StringBuilder();

        for (String line : text.split("\\r?\\n")) {

            line = line.trim();

            if (line.isBlank()) {
                continue;
            }

            if (line.contains("[Bookable Resources]")) {
                insideResources = true;
                insideNotes = false;
                continue;
            }

            if (line.contains("[Notes]")) {
                insideResources = false;
                insideNotes = true;
                continue;
            }

            if (insideNotes) {
                notes.append(line).append("\n");
                continue;
            }

            if (!insideResources) {
                continue;
            }

            if (line.startsWith("ROOM")
                    || line.startsWith("=")
                    || line.startsWith(":=")) {
                continue;
            }

            facilities.add(line);
        }

        notesText = notes.toString().trim();

        return facilities;
    }

    private void applyFilter(String query) {

        if (query == null || query.isBlank()) {

            resultsList.setItems(
                    FXCollections.observableArrayList(allEntries));

            return;
        }

        String q = query.toLowerCase();

        List<String> filtered = allEntries.stream()
                .filter(e -> e.toLowerCase().contains(q))
                .collect(Collectors.toList());

        resultsList.setItems(
                FXCollections.observableArrayList(filtered));
    }

}