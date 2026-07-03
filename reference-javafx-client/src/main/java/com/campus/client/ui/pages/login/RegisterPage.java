package com.campus.client.ui.pages.login;

import com.campus.client.data.UserStorage;
import com.campus.client.model.User;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/** RegisterPage - FR02: new student account creation, stored in data/users.txt. */
public class RegisterPage extends VBox {

    private final UserStorage userStorage;

    private final TextField studentIdField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Label errorLabel = new Label();
    private final Button registerButton = new Button("Create Account");
    private final Button backToLoginButton = new Button("Back to Login");

    private Consumer<User> onRegisterSuccess;
    private Runnable onBackToLogin;

    public RegisterPage(UserStorage userStorage) {
        this.userStorage = userStorage;

        setSpacing(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));
        setMaxWidth(360);

        Label title = new Label("Create Your Account");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        studentIdField.setPromptText("Student ID");
        nameField.setPromptText("Full Name");
        emailField.setPromptText("Email");
        passwordField.setPromptText("Password");
        confirmPasswordField.setPromptText("Confirm Password");
        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setWrapText(true);

        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setDefaultButton(true);
        registerButton.setOnAction(e -> handleRegister());

        backToLoginButton.setMaxWidth(Double.MAX_VALUE);
        backToLoginButton.setOnAction(e -> { if (onBackToLogin != null) onBackToLogin.run(); });

        getChildren().addAll(title, studentIdField, nameField, emailField,
                passwordField, confirmPasswordField, errorLabel, registerButton, backToLoginButton);
    }

    public void setOnRegisterSuccess(Consumer<User> callback) { this.onRegisterSuccess = callback; }
    public void setOnBackToLogin(Runnable callback) { this.onBackToLogin = callback; }

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