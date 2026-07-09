package com.campus.client.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class Footer extends HBox {

    public Footer() {

        Label label = new Label(
                "Campus Companion   •   ITS66704 / ITS610304 Advanced Programming   •   Group Project"
        );

        label.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: #777777;"
        );

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(8, 18, 8, 18));

        setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #E5E5E5;" +
                "-fx-border-width: 1 0 0 0;"
        );

        HBox.setHgrow(label, Priority.ALWAYS);

        getChildren().add(label);
    }
}