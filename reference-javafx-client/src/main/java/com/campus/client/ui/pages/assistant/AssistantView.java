package com.campus.client.ui.pages.assistant;

import com.campus.client.services.rag.RagResult;
import com.campus.client.services.rag.RagService;
import com.campus.client.ui.components.Theme;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.concurrent.ExecutorService;

/**
 * AssistantView - same RAG pipeline as before (retrieve -> augment ->
 * generate via RagService on a background thread, same null-RagService
 * guard). This pass only restyles the transcript as chat bubbles instead
 * of a plain TextArea, matching the mockup.
 */
public class AssistantView extends BorderPane {

    private record ChatMessage(boolean fromUser, String text) {}

    private final RagService ragService;
    private final ExecutorService worker;

    private final TextField questionField = new TextField();
    private final Button askButton = new Button("Send");
    private final Button clearChatButton = new Button("Clear Chat");
    private final Button backButton = new Button("\u2190 Back");

    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final ListView<ChatMessage> chatList = new ListView<>(messages);
    private final TitledPane contextPane = new TitledPane();
    private final TextArea contextArea = new TextArea();
    private final Label statusLabel = new Label();

    private Runnable onBack;

    public AssistantView(RagService ragService, ExecutorService worker) {
        this.ragService = ragService;
        this.worker = worker;

        setPadding(new Insets(20));
        setTop(buildHeader());
        setCenter(buildConversation());
        setBottom(buildInputBar());

        messages.add(new ChatMessage(false, "Hello! I'm your campus assistant. How can I help you today?"));

        askButton.setOnAction(e -> handleAsk());
        questionField.setOnAction(e -> handleAsk()); // Enter key submits too
        clearChatButton.setOnAction(e -> handleClear());
        backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });

        if (ragService == null) {
            questionField.setDisable(true);
            askButton.setDisable(true);
            statusLabel.setText("Assistant unavailable: ANTHROPIC_API_KEY is not set.");
        }
    }

    public void setOnBack(Runnable r) { this.onBack = r; }

    private VBox buildHeader() {
        Label title = new Label("Campus Assistant");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        Label subtitle = new Label("Ask me anything about campus facilities, bookings or policies");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        clearChatButton.setStyle(Theme.secondaryButton());

        HBox topRow = new HBox(backButton);
        HBox.setHgrow(topRow, Priority.ALWAYS);

        HBox titleRow = new HBox(title);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(spacer, clearChatButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, backButton, titleRow, subtitle);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    private VBox buildConversation() {
        chatList.setCellFactory(list -> new ChatBubbleCell());
        chatList.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(chatList, Priority.ALWAYS);

        contextArea.setEditable(false);
        contextArea.setWrapText(true);
        contextArea.setPrefRowCount(6);
        contextPane.setText("Retrieved context (shows what the answer is grounded in)");
        contextPane.setContent(contextArea);
        contextPane.setExpanded(false);

        VBox box = new VBox(10, chatList, contextPane);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox buildInputBar() {
        questionField.setPromptText("Type your question...");
        questionField.setPrefHeight(38);
        HBox.setHgrow(questionField, Priority.ALWAYS);

        askButton.setStyle(Theme.primaryButton());

        HBox box = new HBox(10, questionField, askButton);
        box.setAlignment(Pos.CENTER);

        VBox wrapper = new VBox(5, box, statusLabel);
        wrapper.setPadding(new Insets(15, 0, 0, 0));

        HBox outer = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return outer;
    }

    /** Same retrieve -> augment -> generate pipeline as before, run on a background thread. */
    private void handleAsk() {
        if (ragService == null) {
            statusLabel.setText("Assistant unavailable: ANTHROPIC_API_KEY is not set.");
            return;
        }
        String question = questionField.getText().trim();
        if (question.isEmpty()) {
            statusLabel.setText("Please type a question first.");
            return;
        }

        messages.add(new ChatMessage(true, question));
        questionField.clear();
        askButton.setDisable(true);
        statusLabel.setText("Thinking...");

        worker.submit(() -> {
            try {
                com.campus.client.services.rag.RagService.RagResult result = ragService.ask(question, "campus services");
                Platform.runLater(() -> {
                    messages.add(new ChatMessage(false, result.answer()));
                    chatList.scrollTo(messages.size() - 1);
                    contextArea.setText(result.context());
                    statusLabel.setText("");
                    askButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    messages.add(new ChatMessage(false, "Sorry, I couldn't reach the campus service right now ("
                            + ex.getMessage() + "). Please try again shortly."));
                    chatList.scrollTo(messages.size() - 1);
                    statusLabel.setText("Error: " + ex.getMessage());
                    askButton.setDisable(false);
                });
            }
        });
    }

    private void handleClear() {
        messages.clear();
        messages.add(new ChatMessage(false, "Hello! I'm your campus assistant. How can I help you today?"));
        contextArea.clear();
        statusLabel.setText("");
    }

    /** Renders a ChatMessage as a left (assistant) or right (user, pink) aligned bubble. */
    private static class ChatBubbleCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage message, boolean empty) {
            super.updateItem(message, empty);
            setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");

            if (empty || message == null) {
                setGraphic(null);
                return;
            }

            Label bubble = new Label(message.text());
            bubble.setWrapText(true);
            bubble.setMaxWidth(360);
            bubble.setPadding(new Insets(10, 14, 10, 14));

            HBox row = new HBox(bubble);
            if (message.fromUser()) {
                bubble.setStyle("-fx-background-color: #FCE4EC; -fx-background-radius: 12;");
                row.setAlignment(Pos.CENTER_RIGHT);
            } else {
                bubble.setStyle("-fx-background-color: white; -fx-border-color:" + Theme.GREY_BORDER
                        + "; -fx-border-radius: 12; -fx-background-radius: 12;");
                row.setAlignment(Pos.CENTER_LEFT);
            }
            row.setFillHeight(false);
            setGraphic(row);
        }
    }
}