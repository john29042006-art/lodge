package lodge.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lodge.Main;
import lodge.service.Database;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private RadioButton guestRadio;
    @FXML private RadioButton ownerRadio;
    @FXML private Label errorLabel;

    @FXML private void initialize() {
        guestRadio.selectedProperty().addListener((obs, oldValue, selected) -> {
            passwordField.setDisable(selected);
            passwordField.setPromptText(selected ? "Password not required for guest access" : "Enter admin password");
            usernameField.setPromptText(selected ? "Enter your full name" : "Enter admin username");
            if (selected) passwordField.clear();
        });
        passwordField.setDisable(guestRadio.isSelected());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin(null));
    }

    @FXML private void handleLogin(ActionEvent event) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isBlank()) {
            errorLabel.setText(ownerRadio.isSelected() ? "Enter the admin username." : "Enter your full name to continue.");
            return;
        }
        boolean owner = ownerRadio.isSelected();
        if (owner) {
            String expected = System.getenv().getOrDefault("HOTELOOK_ADMIN_PASSWORD", "admin123");
            if (!"admin".equalsIgnoreCase(username) || !expected.equals(passwordField.getText())) {
                errorLabel.setText("Incorrect admin username or password.");
                passwordField.clear();
                return;
            }
        }
        try {
            Database.initialize();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/lodge/views/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1240, 820);
            scene.getStylesheets().add(Main.class.getResource("/lodge/styles/theme.css").toExternalForm());
            DashboardController controller = loader.getController();
            controller.setSession(username, owner);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Hotelook | " + (owner ? "Admin Dashboard" : "Guest Portal"));
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.centerOnScreen();
        } catch (Exception e) {
            errorLabel.setText("Could not open Hotelook: " + e.getMessage());
        }
    }
}
