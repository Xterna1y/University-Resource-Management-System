package com.campus.client;

import com.campus.client.services.llm.AnthropicClient;
import com.campus.client.services.mcp.CampusMcpClient;
import com.campus.client.services.rag.RagService;
import com.campus.client.ui.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX host for the reference client. Resolves configuration, connects to the Campus MCP server
 * over HTTP/SSE on a background thread, builds the RAG stack, and hands everything to {@link MainView}.
 *
 * <p>Configuration (all overridable):
 * <ul>
 *   <li>{@code -Dmcp.server.url} or env {@code MCP_SERVER_URL} (default http://localhost:8080)</li>
 *   <li>env {@code ANTHROPIC_API_KEY} (required for the RAG tab)</li>
 *   <li>{@code -Danthropic.model} (default claude-sonnet-4-6)</li>
 * </ul>
 */
public final class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final String DEFAULT_URL = "http://localhost:8080";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";

    private CampusMcpClient mcp;
    private MainView view;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        view = new MainView();
        stage.setTitle("Campus MCP Reference Client");
        stage.setScene(new Scene(view.getRoot(), 900, 720));
        stage.show();

        Thread t = new Thread(this::bootstrap, "mcp-bootstrap");
        t.setDaemon(true);
        t.start();
    }

    private void bootstrap() {
        try {
            String url = firstNonBlank(System.getProperty("mcp.server.url"),
                    System.getenv("MCP_SERVER_URL"), DEFAULT_URL);
            view.setStatus("Connecting to MCP server at " + url + " …");

            mcp = new CampusMcpClient(url);
            var init = mcp.connect();

            // The LLM is optional: discovery and direct tool calls work without an API key.
            RagService rag = null;
            String apiKey = "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";
            String llmNote;
            String model = firstNonBlank(System.getProperty("anthropic.model"), DEFAULT_MODEL);
            rag = new RagService(mcp, new AnthropicClient(apiKey, model, 1024));
            llmNote = "LLM: " + model;

            final RagService ragFinal = rag;
            Platform.runLater(() -> {
                view.bind(mcp, ragFinal);
                view.setStatus("Connected to '" + init.serverInfo().name() + "'.  " + llmNote);
                view.refreshDiscovery();
            });
        } catch (Exception e) {
            log.error("Bootstrap failed", e);
            Platform.runLater(() -> view.setStatus("Connection failed: " + e.getMessage()
                    + "  (Is the server running?)"));
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    @Override
    public void stop() {
        if (mcp != null) {
            mcp.close();
        }
        Platform.exit();
    }
}
