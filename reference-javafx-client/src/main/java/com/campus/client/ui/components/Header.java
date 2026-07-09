package com.campus.client.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Header extends VBox {

    private final Label titleLabel = new Label("Campus Companion");
    private final Label statusLabel = new Label();

    public Header() {

        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));

        statusLabel.setFont(Font.font("Segoe UI", 13));
        statusLabel.setStyle("-fx-text-fill:#777777;");

        setSpacing(5);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(18,25,18,25));

        setStyle(
                "-fx-background-color:white;" +
                "-fx-border-color:#E5E5E5;" +
                "-fx-border-width:0 0 1 0;"
        );

        VBox.setVgrow(titleLabel, Priority.NEVER);

        getChildren().addAll(titleLabel, statusLabel);
    }

    public void setTitle(String title){
        titleLabel.setText(title);
    }

    public void setStatus(String status){
        statusLabel.setText(status);
    }
}