package it.unibo.taskhive.controllers;

import it.unibo.taskhive.models.Notification;
import it.unibo.taskhive.services.NotificationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationViewController {

    @FXML
    private ListView<Notification> notificationListView;

    private final NotificationService notificationService = NotificationService.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(NotificationViewController.class);

    public void initialize() {

        int currentUserId = 13;

        /*var sessionUser = SessionManager.getInstance().getCurrentUser();

        if(sessionUser == null) {
            logger.warn("[NotificationViewController] Nessun utente loggato trovato nella sessione.");
            return;
        }

        long currentUserId = sessionUser.getId();*/
        logger.info("[NotificationViewController] Caricamento notifiche per userId={}");

        /*notificationService.createNotification(1, "Complete your profile to unlock features!", 
            it.unibo.taskhive.models.NotificationType.SYSTEM, java.time.LocalDateTime.now().plusDays(1));
        notificationService.createNotification(1, "Your task is due tomorrow!", 
            it.unibo.taskhive.models.NotificationType.REMINDER, java.time.LocalDateTime.now().plusHours(12));
        notificationService.createNotification(1, "Meeting scheduled for today at 3 PM.", 
            it.unibo.taskhive.models.NotificationType.REMINDER, java.time.LocalDateTime.now().plusHours(3));
        */

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        ObservableList<Notification> notifications = FXCollections.observableArrayList(notificationService.getNotificationsByUser(currentUserId));
        notificationListView.setItems(notifications);

        notificationListView.setCellFactory(listView -> new ListCell<Notification>() {
            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);

                if (empty || notification == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Creazione del layout della cella
                    HBox root = new HBox(10);
                    root.setStyle("-fx-background-color: #f9f4ff; -fx-background-radius: 10; "
                                + "-fx-padding: 15; -fx-border-color: transparent; "
                                + "-fx-spacing: 10; -fx-alignment: center-left;");

                    // Aggiunta dell'icona o iniziale
                    Label iconLabel = new Label("A");
                    iconLabel.setStyle("-fx-background-color: #e0d6ff; -fx-background-radius: 50%; "
                                    + "-fx-text-fill: #5e4fa2; -fx-font-size: 18px; "
                                    + "-fx-alignment: center; -fx-min-width: 30px; -fx-min-height: 30px; "
                                    + "-fx-max-width: 30px; -fx-max-height: 30px; -fx-padding: 3;");

                    // Contenitore per i testi
                    VBox textContainer = new VBox(5);
                    textContainer.setStyle("-fx-alignment: center-left;");

                    Label titleLabel = new Label(notification.getMessage());
                    titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");

                    Label detailLabel = new Label("Data: " + notification.getReminderTime().format(formatter));
                    detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

                    // Aggiunta dei componenti al layout
                    textContainer.getChildren().addAll(titleLabel, detailLabel);
                    root.getChildren().addAll(iconLabel, textContainer);

                    // Imposta il contenuto della cella
                    setGraphic(root);
                }
            }
        });
    }
    

    @FXML
    private void markSelectedAsRead() {
        Notification selectedNotification = notificationListView.getSelectionModel().getSelectedItem();
        if (selectedNotification != null) {
            notificationService.markAsRead(selectedNotification.getIdNotification());
            notificationListView.refresh();
        }
    }

}
