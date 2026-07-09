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

/** RegisterPage - FR02: new student account creation. (Same logic as before, restyled to match the mockup.) */
public class RegisterPage extends HBox {

    private final UserStorage userStorage;

    private final TextField studentIdField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Label errorLabel = new Label();
    private final Button registerButton = new Button("Register");
    private final Button backToLoginButton = new Button("Login");

    private Consumer<User> onRegisterSuccess;
    private Runnable onBackToLogin;

    public RegisterPage(UserStorage userStorage) {
        this.userStorage = userStorage;

        getChildren().addAll(buildBrandPanel(), buildFormPanel());
        setPrefHeight(680);
    }

    public void setOnRegisterSuccess(Consumer<User> callback) { this.onRegisterSuccess = callback; }
    public void setOnBackToLogin(Runnable callback) { this.onBackToLogin = callback; }

    /** Same branded panel style as LoginPage, matching the mockup's sidebar. */
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
        Label title = new Label("Create an Account");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));

        Label subtitle = new Label("Sign up to get started");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        studentIdField.setPromptText("Enter your student ID");
        nameField.setPromptText("Enter your full name");
        emailField.setPromptText("Enter your email");
        passwordField.setPromptText("Enter your password");
        confirmPasswordField.setPromptText("Confirm your password");
        for (Control c : new Control[]{studentIdField, nameField, emailField, passwordField, confirmPasswordField}) {
            c.setPrefHeight(38);
        }

        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setWrapText(true);

        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setPrefHeight(42);
        registerButton.setStyle(Theme.primaryButton());
        registerButton.setDefaultButton(true);
        registerButton.setOnAction(e -> handleRegister());

        backToLoginButton.setStyle("-fx-background-color: transparent; -fx-text-fill:" + Theme.RED
                + "; -fx-font-weight: bold; -fx-underline: true; -fx-cursor: hand;");
        backToLoginButton.setOnAction(e -> { if (onBackToLogin != null) onBackToLogin.run(); });

        HBox loginRow = new HBox(4, new Label("Already have an account?"), backToLoginButton);
        loginRow.setAlignment(Pos.CENTER);

        VBox form = new VBox(10,
                title, subtitle,
                labeled("Full Name", nameField),
                labeled("Student ID", studentIdField),
                labeled("Email", emailField),
                labeled("Password", passwordField),
                labeled("Confirm Password", confirmPasswordField),
                errorLabel,
                registerButton,
                loginRow);
        form.setMaxWidth(340);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox wrapper = new VBox(form);
        wrapper.setAlignment(Pos.CENTER);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.setPadding(new Insets(40));
        return wrapper;
    }

    private VBox labeled(String labelText, Control field) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        return new VBox(4, label, field);
    }

    private void handleRegister() {
        String id = studentIdField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        String error = validate(id, name, email, password, confirm);
        if (error != null) {
            errorLabel.setText(error);
            return;
        }

        try {
            User user = userStorage.register(id, name, email, password);
            errorLabel.setText("");
            if (onRegisterSuccess != null) onRegisterSuccess.accept(user);
        } catch (IllegalStateException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    private String validate(String id, String name, String email, String password, String confirm) {
        if (id.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return "Please fill in all fields.";
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            return "Please enter a valid email address.";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }
        return null;
    }
}