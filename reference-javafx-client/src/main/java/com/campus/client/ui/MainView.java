package com.campus.client.ui;

import com.campus.client.services.mcp.CampusMcpClient;
import com.campus.client.services.rag.RagService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The reference UI. It deliberately covers the three things students must understand, then stops:
 * <ol>
 *   <li><b>Discovery</b> &mdash; list the Tools, Resources and Prompts the server advertises.</li>
 *   <li><b>RAG</b> &mdash; ask a question; retrieve via MCP, generate via the LLM.</li>
 *   <li><b>Direct tool call</b> &mdash; book a room without involving the LLM.</li>
 * </ol>
 *
 * <p>Students replace/extend this with the full UI for their chosen campus application
 * (resource booking, lecturer appointments, leave application, etc.).</p>
 */
public final class MainView {

    private final VBox root = new VBox(10);
    private final Label status = new Label("Starting…");

    private final TextArea discoveryArea = new TextArea();

    // RAG tab
    private final TextField topicField = new TextField();
    private final TextField questionField = new TextField();
    private final TextArea ragOutput = new TextArea();
    private final Button askButton = new Button("Ask (retrieve + generate)");

    // Booking tab
    private final TextField resourceField = new TextField();
    private final TextField dateField = new TextField();
    private final TextField startField = new TextField();
    private final TextField endField = new TextField();
    private final TextField studentField = new TextField();
    private final TextArea bookingOutput = new TextArea();
    private final Button bookButton = new Button("Call book_resource");

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ui-worker");
        t.setDaemon(true);
        return t;
    });

    private CampusMcpClient mcp;
    private RagService rag;

    public MainView() {
        root.setPadding(new Insets(14));
        Label heading = new Label("Campus MCP Reference Client");
        heading.setFont(Font.font(18));

        TabPane tabs = new TabPane(discoveryTab(), ragTab(), bookingTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        status.setWrapText(true);
        root.getChildren().addAll(heading, tabs, status);

        setEnabled(false);
        wire();
    }

    public VBox getRoot() {
        return root;
    }

    public void bind(CampusMcpClient mcp, RagService rag) {
        this.mcp = mcp;
        this.rag = rag;
        setEnabled(true);
        if (rag == null) {
            askButton.setDisable(true); // no API key configured
        }
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    /** Populates the discovery tab with the server's advertised capabilities. */
    public void refreshDiscovery() {
        if (mcp == null) {
            return;
        }
        worker.submit(() -> {
            try {
                String tools = mcp.listTools().stream()
                        .map(t -> "  • " + t.name() + " — " + t.description())
                        .collect(Collectors.joining("\n"));
                String resources = mcp.listResources().stream()
                        .map(r -> "  • " + r.uri() + " (" + r.name() + ")")
                        .collect(Collectors.joining("\n"));
                String prompts = mcp.listPrompts().stream()
                        .map(p -> "  • " + p.name() + " — " + p.description())
                        .collect(Collectors.joining("\n"));
                String text = "TOOLS\n" + tools + "\n\nRESOURCES\n" + resources + "\n\nPROMPTS\n" + prompts;
                Platform.runLater(() -> discoveryArea.setText(text));
            } catch (Exception e) {
                Platform.runLater(() -> discoveryArea.setText("Discovery failed: " + e.getMessage()));
            }
        });
    }

    // ---- tabs ------------------------------------------------------------

    private Tab discoveryTab() {
        discoveryArea.setEditable(false);
        discoveryArea.setFont(Font.font("Monospaced", 12));
        VBox box = new VBox(8,
                new Label("Capabilities advertised by the connected MCP server:"), discoveryArea);
        VBox.setVgrow(discoveryArea, Priority.ALWAYS);
        box.setPadding(new Insets(10));
        return new Tab("Discovery", box);
    }

    private Tab ragTab() {
        topicField.setPromptText("topic (optional): booking / appointments / leave / library");
        questionField.setPromptText("e.g. How many room bookings can I hold at once?");
        ragOutput.setEditable(false);
        ragOutput.setWrapText(true);
        ragOutput.setFont(Font.font("Monospaced", 12));
        VBox.setVgrow(ragOutput, Priority.ALWAYS);

        VBox box = new VBox(8,
                new Label("Ask the campus assistant. The answer is grounded in retrieved context."),
                topicField, questionField, new HBox(askButton),
                new Label("Result:"), ragOutput);
        box.setPadding(new Insets(10));
        return new Tab("Ask (RAG)", box);
    }

    private Tab bookingTab() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        resourceField.setPromptText("HC-01");
        dateField.setPromptText("2026-07-01");
        startField.setPromptText("10:00");
        endField.setPromptText("11:00");
        studentField.setPromptText("S1234567");
        form.addRow(0, new Label("Resource id"), resourceField);
        form.addRow(1, new Label("Date"), dateField);
        form.addRow(2, new Label("Start"), startField);
        form.addRow(3, new Label("End"), endField);
        form.addRow(4, new Label("Student id"), studentField);
        form.add(new HBox(bookButton), 1, 5);

        bookingOutput.setEditable(false);
        bookingOutput.setWrapText(true);
        bookingOutput.setFont(Font.font("Monospaced", 12));
        VBox.setVgrow(bookingOutput, Priority.ALWAYS);

        VBox box = new VBox(10,
                new Label("Directly invoke an MCP tool (no LLM involved):"),
                form, new Label("Result:"), bookingOutput);
        box.setPadding(new Insets(10));
        return new Tab("Book a room", box);
    }

    // ---- behaviour -------------------------------------------------------

    private void wire() {
        askButton.setOnAction(e -> {
            String q = questionField.getText().trim();
            if (q.isEmpty() || rag == null) {
                return;
            }
            setEnabled(false);
            ragOutput.setText("Retrieving context and generating…");
            worker.submit(() -> {
                try {
                    RagService.RagResult r = rag.ask(q, topicField.getText().trim());
                    String out = "RETRIEVED CONTEXT (via search_campus_info)\n"
                            + r.retrievedContext() + "\n\n=== ANSWER ===\n" + r.answer();
                    Platform.runLater(() -> {
                        ragOutput.setText(out);
                        setEnabled(true);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        ragOutput.setText("Error: " + ex.getMessage());
                        setEnabled(true);
                    });
                }
            });
        });

        bookButton.setOnAction(e -> {
            if (mcp == null) {
                return;
            }
            setEnabled(false);
            bookingOutput.setText("Calling book_resource…");
            worker.submit(() -> {
                try {
                    String result = mcp.callTool("book_resource", Map.of(
                            "resourceId", resourceField.getText().trim(),
                            "date", dateField.getText().trim(),
                            "startTime", startField.getText().trim(),
                            "endTime", endField.getText().trim(),
                            "studentId", studentField.getText().trim()));
                    Platform.runLater(() -> {
                        bookingOutput.setText(result);
                        setEnabled(true);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        bookingOutput.setText("Error: " + ex.getMessage());
                        setEnabled(true);
                    });
                }
            });
        });
    }

    private void setEnabled(boolean enabled) {
        askButton.setDisable(!enabled || rag == null);
        bookButton.setDisable(!enabled);
    }
}
