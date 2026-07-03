package com.campus.client.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Header extends VBox {

    private final Label titleLabel = new Label("Campus Companion");
    private final Label statusLabel = new Label("Starting...");

    public Header() {
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setStyle("-fx-text-fill: #666666;");

        setSpacing(4);
        setPadding(new Insets(15, 20, 10, 20));
        setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");
        getChildren().addAll(titleLabel, statusLabel);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }
}