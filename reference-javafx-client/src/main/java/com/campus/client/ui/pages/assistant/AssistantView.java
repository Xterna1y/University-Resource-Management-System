package com.campus.client.ui.pages.assistant;

import com.campus.client.services.rag.RagResult;
import com.campus.client.services.rag.RagService;

import javafx.application.Platform;
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
 * AssistantView - FR10 / UC15-16: lets the student ask the Campus Assistant
 * a question and shows a grounded answer generated via RAG (Guide 1 & the
 * design report's Section 8.4). The retrieved context is shown alongside the
 * answer for transparency, as required by "Step 4: Display" in the report.
 */
public class AssistantView extends BorderPane {

    private final RagService ragService;
    private final ExecutorService worker;

    private final TextField questionField = new TextField();
    private final Button askButton = new Button("Send");
    private final Button clearChatButton = new Button("Clear Chat");
    private final Button backButton = new Button("Back");

    private final TextArea conversationArea = new TextArea();
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

        askButton.setOnAction(e -> handleAsk());
        questionField.setOnAction(e -> handleAsk()); // Enter key submits too
        clearChatButton.setOnAction(e -> handleClear());
        backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void setOnBack(Runnable r) { this.onBack = r; }

    private VBox buildHeader() {
        Label title = new Label("Campus Assistant");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox actions = new HBox(10, clearChatButton, backButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, title, actions);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    private VBox buildConversation() {
        conversationArea.setEditable(false);
        conversationArea.setWrapText(true);
        conversationArea.setPrefRowCount(16);
        VBox.setVgrow(conversationArea, Priority.ALWAYS);

        contextArea.setEditable(false);
        contextArea.setWrapText(true);
        contextArea.setPrefRowCount(6);
        contextPane.setText("Retrieved context (shows what the answer is grounded in)");
        contextPane.setContent(contextArea);
        contextPane.setExpanded(false);

        VBox box = new VBox(10, conversationArea, contextPane);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox buildInputBar() {
        questionField.setPromptText("Ask about facilities, bookings, or campus policies...");
        HBox.setHgrow(questionField, Priority.ALWAYS);

        HBox box = new HBox(10, questionField, askButton);
        box.setAlignment(Pos.CENTER);

        VBox wrapper = new VBox(5, box, statusLabel);
        wrapper.setPadding(new Insets(15, 0, 0, 0));

        HBox outer = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return outer;
    }

    /** Runs the retrieve -> augment -> generate pipeline on a background thread. */
    private void handleAsk() {
        String question = questionField.getText().trim();
        if (question.isEmpty()) {
            statusLabel.setText("Please type a question first.");
            return;
        }

        appendToConversation("You: " + question);
        questionField.clear();
        askButton.setDisable(true);
        statusLabel.setText("Thinking...");

        worker.submit(() -> {
            try {
                com.campus.client.services.rag.RagService.RagResult result = ragService.ask(question, "campus services");
                Platform.runLater(() -> {
                    appendToConversation("Assistant: " + result.answer());
                    contextArea.setText(result.context());
                    statusLabel.setText("");
                    askButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    appendToConversation("Assistant: Sorry, I couldn't reach the campus service right now ("
                            + ex.getMessage() + "). Please try again shortly.");
                    statusLabel.setText("Error: " + ex.getMessage());
                    askButton.setDisable(false);
                });
            }
        });
    }

    private void handleClear() {
        conversationArea.clear();
        contextArea.clear();
        statusLabel.setText("");
    }

    private void appendToConversation(String line) {
        conversationArea.appendText((conversationArea.getText().isEmpty() ? "" : "\n\n") + line);
    }
}
