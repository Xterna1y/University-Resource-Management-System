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

/** LoginPage - FR01: student sign in with Student ID + password. */
public class LoginPage extends VBox {

    private final UserStorage userStorage;

    private final TextField studentIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();
    private final Button loginButton = new Button("Log In");
    private final Button goToRegisterButton = new Button("Create an account");

    private Consumer<User> onLoginSuccess;
    private Runnable onGoToRegister;

    public LoginPage(UserStorage userStorage) {
        this.userStorage = userStorage;

        setSpacing(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));
        setMaxWidth(360);

        Label title = new Label("Welcome Back");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        studentIdField.setPromptText("Student ID");
        passwordField.setPromptText("Password");
        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setWrapText(true);

        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> handleLogin());

        goToRegisterButton.setMaxWidth(Double.MAX_VALUE);
        goToRegisterButton.getStyleClass().add("secondary");
        goToRegisterButton.setOnAction(e -> { if (onGoToRegister != null) onGoToRegister.run(); });

        getChildren().addAll(title, studentIdField, passwordField, errorLabel, loginButton, goToRegisterButton);
    }

    public void setOnLoginSuccess(Consumer<User> callback) { this.onLoginSuccess = callback; }
    public void setOnGoToRegister(Runnable callback) { this.onGoToRegister = callback; }

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