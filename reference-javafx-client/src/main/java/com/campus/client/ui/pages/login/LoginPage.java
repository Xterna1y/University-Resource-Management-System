package com.campus.client.ui.pages.login;

import com.campus.client.data.UserStorage;
import com.campus.client.model.User;
import com.campus.client.ui.components.Theme;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/** LoginPage - FR01: student sign in with Student ID + password. (Same logic as before, restyled to match the mockup.) */
public class LoginPage extends HBox {

    private final UserStorage userStorage;

    private final TextField studentIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();
    private final Button loginButton = new Button("Login");
    private final Button goToRegisterButton = new Button("Register");

    private Consumer<User> onLoginSuccess;
    private Runnable onGoToRegister;

    public LoginPage(UserStorage userStorage) {
        this.userStorage = userStorage;

        getChildren().addAll(buildBrandPanel(), buildFormPanel());
        setPrefHeight(600);
    }

    public void setOnLoginSuccess(Consumer<User> callback) { this.onLoginSuccess = callback; }
    public void setOnGoToRegister(Runnable callback) { this.onGoToRegister = callback; }

    /** Left grey panel with Taylor's branding, matching the mockup's sidebar. */
    private VBox buildBrandPanel() {
        Label logo = new Label("TAYLOR'S\nUNIVERSITY");
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        logo.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        Label motto = new Label("Wisdom \u00b7 Integrity \u00b7 Excellence");
        motto.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + "; -fx-font-size: 10px;");

        Label campusServices = new Label("Campus Services");
        Label bookingSystem = new Label("Resource Booking System");
        bookingSystem.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        for (Label l : new Label[]{campusServices, bookingSystem}) {
            l.setStyle(l.getStyle() + "-fx-text-fill:" + Theme.DARK + ";");
        }

        javafx.scene.shape.Line divider = new javafx.scene.shape.Line(0, 0, 220, 0);
        divider.setStyle("-fx-stroke:" + Theme.GREY_BORDER + ";");

        Label bookResources = new Label("Book resources");
        Label checkAvailability = new Label("Check availability");
        Label planCampus = new Label("Plan your campus");
        for (Label l : new Label[]{bookResources, checkAvailability, planCampus}) {
            l.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");
        }

        VBox topBlock = new VBox(4, logo, motto);
        VBox linksBlock = new VBox(12, campusServices, bookingSystem, divider);
        VBox bottomLinks = new VBox(10, bookResources, checkAvailability, planCampus);

        VBox panel = new VBox(30, topBlock, linksBlock, bottomLinks);
        panel.setPadding(new Insets(40, 30, 40, 30));
        panel.setPrefWidth(260);
        panel.setStyle("-fx-background-color:" + Theme.GREY_BG + ";");
        panel.setAlignment(Pos.TOP_LEFT);
        return panel;
    }

    private VBox buildFormPanel() {
        Label title = new Label("Welcome Back");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));

        Label subtitle = new Label("Log in to continue");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        Label studentIdLabel = new Label("Student ID");
        studentIdLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        studentIdField.setPromptText("Enter your student ID");
        studentIdField.setPrefHeight(38);

        Label passwordLabel = new Label("Password");
        passwordLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(38);

        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setWrapText(true);

        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(42);
        loginButton.setStyle(Theme.primaryButton());
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> handleLogin());

        goToRegisterButton.setStyle("-fx-background-color: transparent; -fx-text-fill:" + Theme.RED
                + "; -fx-font-weight: bold; -fx-underline: true; -fx-cursor: hand;");
        goToRegisterButton.setOnAction(e -> { if (onGoToRegister != null) onGoToRegister.run(); });

        HBox registerRow = new HBox(4, new Label("Don't have an account?"), goToRegisterButton);
        registerRow.setAlignment(Pos.CENTER);

        VBox form = new VBox(10,
                title, subtitle,
                spacer(10),
                studentIdLabel, studentIdField,
                passwordLabel, passwordField,
                errorLabel,
                spacer(6),
                loginButton,
                registerRow);
        form.setMaxWidth(340);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox wrapper = new VBox(form);
        wrapper.setAlignment(Pos.CENTER);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.setPadding(new Insets(40));
        return wrapper;
    }

    private javafx.scene.layout.Region spacer(double height) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setPrefHeight(height);
        return r;
    }

    private void handleLogin() {
        String id = studentIdField.getText().trim();
        String password = passwordField.getText();

        if (id.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter your Student ID and password.");
            return;
        }

        userStorage.authenticate(id, password).ifPresentOrElse(
                user -> {
                    errorLabel.setText("");
                    if (onLoginSuccess != null) onLoginSuccess.accept(user);
                },
                () -> errorLabel.setText("Incorrect Student ID or password.")
        );
    }
}