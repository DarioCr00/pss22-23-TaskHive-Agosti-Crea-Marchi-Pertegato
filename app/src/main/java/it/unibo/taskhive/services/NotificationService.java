package it.unibo.taskhive.services;

import it.unibo.taskhive.models.Notification;
import it.unibo.taskhive.models.NotificationType;
import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {

    private static NotificationService instance;

    //simulazione db con una lista
    private final List<Notification> notifications = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    //Listener UI per notifiche in tempo reale
    private Consumer<String> onNotificationListener;

    private NotificationService() {}

    public static NotificationService getInstance() {
        if(instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    
    //Metodo per creare una notifica
    public Notification createNotification(int userId, String message, NotificationType type, LocalDateTime reminderTime) {
        Notification notification = new Notification(userId, message, type, reminderTime);
        notifications.add(notification);

        logger.debug("[NotificationService] Total notifications stored: {}", notifications.size());

        if (onNotificationListener != null) {
            logger.info("[NotificationService] Triggering UI listener for userId={}", userId);
            try {
                onNotificationListener.accept(message);
            } catch (Exception e) {
                logger.error("[NotificationService] Error while triggering UI listener: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("[NotificationService] No UI listener set — notification will not be displayed in real time.");
        }

        /*if(onNotificationListener != null) {
            onNotificationListener.accept(message);
        }*/

        return notification;
    }

    //Recupero delle notifiche per l'utente
    public List<Notification> getNotificationsByUser(long userId) {

        logger.info("[NotificationService] Fetching notifications for userId={}", userId);

        List<Notification> userNotifications = new ArrayList<>();
        for (Notification notification : notifications) {
            if (notification.getUserId() == userId) {
                userNotifications.add(notification);
            }
        }

        logger.debug("[NotificationService] Found {} notifications for userId={}", userNotifications.size(), userId);
        return userNotifications;
    }

    //Marcatura lettura notifica
    public void markAsRead(int notificationId) {
        logger.info("[NotificationService] Marking notification {} as read", notificationId);
        for (Notification notification : notifications) {
            if (notification.getIdNotification() == notificationId) {
                notification.setRead(true);
                logger.debug("[NotificationService] Notification {} marked as read", notificationId);
                break;
            }
        }
    }

    public void notifyProjectCreated(Project project, Long creatorId) {

        logger.info("[NotificationService] Sending 'project created' notification for project='{}' (creatorId={})", project.getName(), creatorId);

        String message = "Nuovo progetto creato: " + project.getName();

        for (Long memberId : project.getMembers()) {
            logger.debug("[NotificationService] Notifying memberId={}", memberId);
            createNotification(
                memberId.intValue(),
                message,
                NotificationType.PROJECT_CREATED,
                java.time.LocalDateTime.now()
            );
        }
        
        logger.info("[NotificationService] Project creation notifications sent for project='{}'", project.getName());
    }

    public void notifyTaskCreated(Task task, Long creatorId) {
        logger.info("[NotificationService] Sending 'task created'notification for task='{}'(creatorId={})", task.getTitle(), creatorId);

        String message = "Nuovo task creato: " + task.getTitle();

        //Lista destinatari
        List<Long> recipients = new ArrayList<>();

        //Utente assegnato
        if(task.getAssignedUser() != null) {
            recipients.add(task.getAssignedUser());
        }

        //Followers (se presenti)
        if(task.getFollowers() != null) {
            recipients.addAll(task.getFollowers());
        }

        //Evita duplicati
        recipients = recipients.stream()
            .distinct()
            .toList();

        //Invio delle notifiche
        for (Long userId : recipients) {
            logger.debug("[NotificationService] Notifying userId={}", userId);
            createNotification(
                userId.intValue(),
                message,
                NotificationType.TASK_CREATED,
                java.time.LocalDateTime.now()
            );
        }
        
        logger.info("[NotificationService] Task creation notifications sent for task='{}'", task.getTitle());
    }

    public void setOnNotificationListener(Consumer<String> listener) {
        this.onNotificationListener= listener;
        logger.info("[NotificationService] UI listener registered successfully");
    }
}
