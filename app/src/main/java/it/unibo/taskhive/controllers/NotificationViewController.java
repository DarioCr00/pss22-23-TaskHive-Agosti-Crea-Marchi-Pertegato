package it.unibo.taskhive.controllers;

import it.unibo.taskhive.models.Notification;
import it.unibo.taskhive.services.NotificationService;
import it.unibo.taskhive.services.SceneManager;
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        ObservableList<Notification> notifications = FXCollections.observableArrayList(
            notificationService.getNotificationsByUser(currentUserId)
                .stream()
                .filter(n -> !n.isRead()) //mostra solo le non lette
                .toList()
        );

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
                    root.getStyleClass().add("notification-cell");

                    // Icona circolare con iniziale
                    Label iconLabel = new Label(
                        notification.getMessage() != null && !notification.getMessage().isEmpty()
                            ? notification.getMessage().substring(0, 1).toUpperCase()
                            : "?"
                    );
                    iconLabel.getStyleClass().add("notification-icon");

                    // Contenitore per testo
                    VBox textContainer = new VBox(5);
                    textContainer.getStyleClass().add("notification-text-container");

                    Label titleLabel = new Label(notification.getMessage());
                    titleLabel.getStyleClass().add("notification-title");

                    Label detailLabel = new Label("Data: " + notification.getReminderTime().format(formatter));
                    detailLabel.getStyleClass().add("notification-detail");

                    textContainer.getChildren().addAll(titleLabel, detailLabel);
                    root.getChildren().addAll(iconLabel, textContainer);

                    setGraphic(root);
                }
            }
        });

        // Mostra le notifiche più recenti in cima
        FXCollections.sort(notificationListView.getItems(),
                (n1, n2) -> n2.getReminderTime().compareTo(n1.getReminderTime()));
    }
    

    @FXML
    private void markSelectedAsRead() {
        Notification selectedNotification = notificationListView.getSelectionModel().getSelectedItem();


        if (selectedNotification != null) {

            notificationService.markAsRead(selectedNotification.getIdNotification());

            //rimozione della notifica dalla lista UI
            notificationListView.getItems().remove(selectedNotification);

            logger.info("[NotificationViewController] Notification {} segnata come letta e rimossa dalla lista UI.", selectedNotification.getIdNotification());
        } else {
            logger.warn("[NotificationViewController] Nessuna notifica selezionata da segnare come letta.");
        }
    }

    @FXML
    private void goBack() {
        try{
            SceneManager.switchScene("fxml/ProjectView.fxml");
        } catch (Exception e) {
            logger.error("[NotificationViewController] Errore durante il ritorno alla pagina precedente", e);
        }
    }

}
