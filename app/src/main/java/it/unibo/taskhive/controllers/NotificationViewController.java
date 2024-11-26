package it.unibo.taskhive.controllers;

import it.unibo.taskhive.models.Notification;
import it.unibo.taskhive.services.NotificationService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.util.List;

public class NotificationViewController {

    @FXML
    private ListView<String> notificationList;

    @FXML
    private Button markReadButton;

    private final NotificationService notificationService = new NotificationService();
    private  int userId = 1; //simulazione di un utente con ID 1

    public void initialize() {
        loadNotifications();

        markReadButton.setOnAction(event -> markAllAsRead());
    }

    private void loadNotifications() {
        List<Notification> notifications = notificationService.getNotificationsByUser(userId);
        notificationList.getItems().clear();
        for (Notification notification : notifications) {
            notificationList.getItems().add(notification.getMessage());
        }
    }

    private void markAllAsRead() {
        List<Notification> notifications = notificationService.getNotificationsByUser(userId);
        for (Notification notification : notifications) {
            notificationService.markAsRead(notification.getIdNotification());
        }
        loadNotifications();
    }

}
