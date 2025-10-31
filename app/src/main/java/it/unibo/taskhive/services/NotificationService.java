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

        if (onNotificationListener != null) {           
            try {
                onNotificationListener.accept(message);
            } catch (Exception e) {
                logger.error("[NotificationService] Error while triggering UI listener: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("[NotificationService] No UI listener set — notification will not be displayed in real time.");
        }

        return notification;
    }

    //Recupero delle notifiche per l'utente
    public List<Notification> getNotificationsByUser(long userId) {

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

    public void notifyProjectCreated(Project project, Long creatorId) {

        String message = "Nuovo progetto creato: " + project.getName();

        for (Long memberId : project.getMembers()) {
            createNotification(
                memberId.intValue(),
                message,
                NotificationType.PROJECT_CREATED,
                java.time.LocalDateTime.now()
            );
        }
    }

    public void notifyProjectUpdated(Project project, Long updaterId){

        String message = "Progetto aggiornato: " + project.getName();

        for (Long memberId : project.getMembers()) {
            logger.debug("[NotificationService] Notifying memberId={}", memberId);
            createNotification(
                memberId.intValue(), 
                message, 
                NotificationType.PROJECT_UPDATED, 
                java.time.LocalDateTime.now()
            );
        }
    }

    public void notifyProjectDeleted(Project project, Long deleterId){

        String message = "Il progetto \"" + project.getName() + "\" è stato eliminato.";

        List<Long> recipients = new ArrayList<>();

        if(deleterId != null) {
            recipients.add(deleterId);
        }

        if (project.getMembers() != null && !project.getMembers().isEmpty()) {
            recipients.addAll(project.getMembers());
        }

        recipients = recipients.stream()
                .distinct()
                .toList();

        for (Long memberId : project.getMembers()) {
            createNotification(
                memberId.intValue(),
                message,
                NotificationType.PROJECT_DELETED,
                java.time.LocalDateTime.now()
            );
        }
    }

    public void notifyTaskCreated(Task task, Long creatorId) {

        String message = "Nuovo task creato: " + task.getTitle();

        //Lista destinatari
        List<Long> recipients = new ArrayList<>();

        //Creatore
        if(creatorId != null) {
            recipients.add(creatorId);
        }

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
            createNotification(
                userId.intValue(),
                message,
                NotificationType.TASK_CREATED,
                java.time.LocalDateTime.now()
            );
        }
    }

    public void notifyTaskUpdated(Task task, Long updaterId) {

        String message = "Task aggiornato: " + task.getTitle();

        List<Long> recipients = new ArrayList<>();

        //Include il modificatore del task
        if(updaterId != null) {
            recipients.add(updaterId);
        }

        if(task.getAssignedUser() != null) {
            recipients.add(task.getAssignedUser());
        }

        if(task.getFollowers() != null) {
            recipients.addAll(task.getFollowers());
        }

        recipients = recipients.stream()
                .distinct()
                .toList();

        for(Long userId : recipients) {
            createNotification(
                userId.intValue(),
                message,
                NotificationType.TASK_UPDATED,
                java.time.LocalDateTime.now()
            );
        }
    }

    public void notifyTaskDeleted(Task task, Long deleterId) {
        logger.info("[NotificationService] Sending 'task deleted' notification for task='{}' (deleterId={})", task.getTitle(), deleterId);

        String message = "Il task \"" + task.getTitle() + "\" è stato eliminato.";

        List<Long> recipients = new ArrayList<>();

        if(deleterId != null) {
            recipients.add(deleterId);
        }

        if(task.getAssignedUser() != null && !task.getAssignedUser().equals(deleterId)) {
            recipients.add(task.getAssignedUser());
        }

        if(task.getFollowers() != null) {
            recipients.addAll(
                task.getFollowers().stream()
                    .filter(id -> !id.equals(deleterId))   
                    .toList()
            );
        }

        recipients = recipients.stream().distinct().toList();

        for(Long userId : recipients) {
            createNotification(
                userId.intValue(),
                message,
                NotificationType.TASK_DELETED,
                java.time.LocalDateTime.now()
            );
        }
    }

    public void setOnNotificationListener(Consumer<String> listener) {
        this.onNotificationListener= listener;
        logger.info("[NotificationService] UI listener registered successfully");
    }
}
