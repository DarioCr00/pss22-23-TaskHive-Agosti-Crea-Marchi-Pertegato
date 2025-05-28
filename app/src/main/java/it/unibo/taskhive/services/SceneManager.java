package it.unibo.taskhive.services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getClassLoader().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());

            scene.getStylesheets().add(SceneManager.class.getClassLoader().getResource("css/styles.css").toExternalForm());
    
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
