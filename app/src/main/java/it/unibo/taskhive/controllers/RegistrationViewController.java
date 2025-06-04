package it.unibo.taskhive.controllers;

import it.unibo.taskhive.services.SceneManager;
import it.unibo.taskhive.services.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class RegistrationViewController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    private final UserService userService = new UserService();

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input non valido", "Username e password non possono essere vuoti.");
        }

        try {
            if (!password.equals(confirmPassword)) {
                
                showAlert("Password non corrispondenti", "Le due password inserite non coincidono.");
            
            }else{
                
                if(userService.register(username, password)){
                    
                    showAlert("Registrazione completata", "Registrazione riuscita.\nOra puoi effettuare il login.");
                    switchToLogin();

                }else {

                    showAlert("Registrazione fallita", "Utente gia' esistente");

                }

            }
            
        } catch (Exception e) {
            showAlert("Errore", e.getMessage());
        }
    }

    @FXML
    private void switchToLogin() {
        SceneManager.switchScene("fxml/LoginView.fxml");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
