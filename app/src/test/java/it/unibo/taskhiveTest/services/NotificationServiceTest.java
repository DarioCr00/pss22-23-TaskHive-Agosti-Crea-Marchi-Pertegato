package it.unibo.taskhiveTest.services;

import it.unibo.taskhive.models.Notification;
import it.unibo.taskhive.models.NotificationType;
import it.unibo.taskhive.services.NotificationService;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    @Test
    void testCreatAndRetrieveNotifications() {
        NotificationService notificationService = new NotificationService();

        //Creazione delle notifiche per il test
        @SuppressWarnings("unused")
        Notification notification1 = notificationService.createNotification(1, "Notifica 1", NotificationType.TASK, LocalDateTime.now());
        @SuppressWarnings("unused")
        Notification notification2 = notificationService.createNotification(1, "Notifica 2", NotificationType.REMINDER, LocalDateTime.now());
        @SuppressWarnings("unused")
        Notification notification3 = notificationService.createNotification(2, "Notifica 3", NotificationType.TASK, LocalDateTime.now());

        //Recupero delle notifiche per utente 1
        List<Notification> user1Notifications = notificationService.getNotificationsByUser(1);
        assertEquals(2, user1Notifications.size());
        assertEquals("Notifica 1", user1Notifications.get(0).getMessage());
        assertEquals("Notifica 2", user1Notifications.get(1).getMessage());

        //Recupero notifiche di utente 2
        List<Notification> user2Notifications = notificationService.getNotificationsByUser(2);
        assertEquals(1, user2Notifications.size());
        assertEquals("Notifica 3", user2Notifications.get(0).getMessage());

        System.out.println("Test 'testCreateAndRetrieveNotifications' per User1 completato con successo: " + user1Notifications.size() + " notifiche trovate.");
        System.out.println("Test 'testCreateAndRetrieveNotifications' per User2 completato con successo: " + user2Notifications.size() + " notifiche trovate.");
    }

    @Test
    void testMarkAsRead() {
        NotificationService notificationService = new NotificationService();

        //Creazione delle notifiche
        Notification notification = notificationService.createNotification(1, "Notifica da leggere", NotificationType.TASK, LocalDateTime.now());

        //marcatura come letta
        notificationService.markAsRead(notification.getIdNotification());

        //Recupero delle notifiche
        List<Notification> user1Notifications = notificationService.getNotificationsByUser(1);
        assertTrue(user1Notifications.get(0).isRead());
    }

}
