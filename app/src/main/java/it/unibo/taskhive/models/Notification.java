package it.unibo.taskhive.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private int idNotification;

    @Column(name = "id_user", nullable = false)
    private int userId;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "reminder_time")
    private LocalDateTime reminderTime;

    public Notification() {}

    public Notification(int  userId, String message, NotificationType notificationType, LocalDateTime reminderTime) {
        this.userId = userId;
        this.message = message;
        this.notificationType = notificationType;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
        this.reminderTime = reminderTime;
    }

    //Getter per idNotification
    public int getIdNotification() {
        return idNotification;
    }

    //Getter per message
    public String getMessage() {
        return message;
    }

    //Getter per userId
    public int getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return notificationType;
    }

    public LocalDateTime getReminderTime() {
        return reminderTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    //Setter per isRead
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    //Getter opzionale per isRead
    public boolean isRead(){
        return isRead;
    }
}
