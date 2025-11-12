package it.unibo.taskhiveTest.services;

import it.unibo.taskhive.models.Notification;
import it.unibo.taskhive.models.NotificationType;
import it.unibo.taskhive.services.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        //Recupero dell'istanza singleton e pulizia delle notifiche prima di ogni test
        notificationService = NotificationService.getInstance();
        clearNotifications(notificationService);
    }

    private void clearNotifications(NotificationService service) {
        try{
            //Helper per svuotare la lista interna di notifiche
            var field = NotificationService.class.getDeclaredField("notifications");
            field.setAccessible(true);
            ((List<?>) field.get(service)).clear();
        } catch(Exception e) {
            throw new RuntimeException("Unable to clear notifications before test", e);
        }
    }

    @Test
    void testCreatAndRetrieveNotifications() {

        //Creazione delle notifiche per il test
        @SuppressWarnings("unused")
        Notification notification1 = notificationService.createNotification(1, "Notifica 1", NotificationType.TASK_UPDATED, LocalDateTime.now());
        @SuppressWarnings("unused")
        Notification notification2 = notificationService.createNotification(1, "Notifica 2", NotificationType.REMINDER, LocalDateTime.now());
        @SuppressWarnings("unused")
        Notification notification3 = notificationService.createNotification(2, "Notifica 3", NotificationType.SYSTEM, LocalDateTime.now());

        //Recupero delle notifiche per utente 1
        List<Notification> user1Notifications = notificationService.getNotificationsByUser(1);
        assertEquals(2, user1Notifications.size(), "user 1 should have two notifications");
        assertEquals("Notifica 1", user1Notifications.get(0).getMessage());
        assertEquals("Notifica 2", user1Notifications.get(1).getMessage());

        //Recupero notifiche di utente 2
        List<Notification> user2Notifications = notificationService.getNotificationsByUser(2);
        assertEquals(1, user2Notifications.size(), "User 2 should have one notification");
        assertEquals("Notifica 3", user2Notifications.get(0).getMessage());

        System.out.println("Test 'testCreateAndRetrieveNotifications' per User1 completato con successo: " + user1Notifications.size() + " notifiche trovate.");
        System.out.println("Test 'testCreateAndRetrieveNotifications' per User2 completato con successo: " + user2Notifications.size() + " notifiche trovate.");
    }

    @Test
    void testMarkAsReadMarksCorrectNotification() {

        //Creazione delle notifiche
        Notification notification = notificationService.createNotification(1, "Notifica da leggere", NotificationType.TASK_UPDATED, LocalDateTime.now());

        //marcatura come letta
        notificationService.markAsRead(notification.getIdNotification());

        //Recupero delle notifiche
        List<Notification> user1Notifications = notificationService.getNotificationsByUser(1);
        assertEquals(1, user1Notifications.size(), "There should be exactly one notificaition");
        assertTrue(user1Notifications.get(0).isRead(), "Notification should be marked as read");
    }

    @Test
    void testMarkAsReadDoesNotThrowIfNotificationNotFound() {
        //test per verificare che il programma non crashi in caso di mark a vuoto
        assertDoesNotThrow(() -> notificationService.markAsRead(999), "Marking a non-existing notification shoud not throw an exception");
    }

    @Test
    void testNotificationListenerIsTriggered() {
        AtomicReference<String> listenerMessage = new AtomicReference<>(null);
        notificationService.setOnNotificationListener(listenerMessage::set);

        notificationService.createNotification(7, "Evento in tempo reale", NotificationType.TASK_CREATED, LocalDateTime.now());

        assertEquals("Evento in tempo reale", listenerMessage.get(), "Listener should receive the same message as the notification");
    }

    @Test
    void testCreateNotificationWithoutListenerDoesNotThrow() {
        assertDoesNotThrow(() -> notificationService.createNotification(5, "Nessun listener", NotificationType.TASK_UPDATED, LocalDateTime.now()));
    }

}
