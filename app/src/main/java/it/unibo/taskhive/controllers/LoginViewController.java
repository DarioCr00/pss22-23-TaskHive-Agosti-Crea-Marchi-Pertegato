package it.unibo.taskhive.controllers;

import it.unibo.taskhive.services.SceneManager;
import it.unibo.taskhive.services.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class LoginViewController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        boolean user = userService.login(username, password);
        if (user == true) {
            showAlert("Login riuscito!", "Benvenuto, " + username);
        } else {
            showAlert("Errore", "Username o password errati");
        }
    }

    @FXML
    private void switchToRegister() {
        SceneManager.switchScene("fxml/RegistrationView.fxml");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
