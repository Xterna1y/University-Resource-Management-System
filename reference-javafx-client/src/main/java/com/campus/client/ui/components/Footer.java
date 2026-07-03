package com.campus.client.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class Footer extends HBox {

    public Footer() {
        Label label = new Label("Campus Companion  \u2022  ITS66704/ITS610304 Advanced Programming  \u2022  Group Project");
        label.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");

        setPadding(new Insets(8, 20, 8, 20));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #dddddd; -fx-border-width: 1 0 0 0;");
        getChildren().add(label);
    }
}