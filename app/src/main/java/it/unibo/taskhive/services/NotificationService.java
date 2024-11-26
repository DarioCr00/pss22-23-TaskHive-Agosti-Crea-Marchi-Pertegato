package it.unibo.taskhive.services;

import it.unibo.taskhive.models.Notification;
import it.unibo.taskhive.models.NotificationType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    //simulazione db con una lista
    private final List<Notification> notifications = new ArrayList<>();
    
    //Metodo per creare una notifica
    public Notification createNotification(int userId, String message, NotificationType type, LocalDateTime reminderTime) {
        Notification notification = new Notification(userId, message, type, reminderTime);
        notifications.add(notification);
        return notification;
    }

    //Recupero delle notifiche per l'utente
    public List<Notification> getNotificationsByUser(int userId) {
        List<Notification> userNotifications = new ArrayList<>();
        for (Notification notification : notifications) {
            if (notification.getUserId() == userId) {
                userNotifications.add(notification);
            }
        }
        return userNotifications;
    }

    //Marcatura lettura notifica
    public void markAsRead(int notificationId) {
        for (Notification notification : notifications) {
            if (notification.getIdNotification() == notificationId) {
                notification.setRead(true);
                break;
            }
        }
    }
}
