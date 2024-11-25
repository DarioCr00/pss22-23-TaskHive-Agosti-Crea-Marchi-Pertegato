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
    private int idNotification;

    @Column(nullable = false)
    private int userId;

    @Column(nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean isRead;

    @Column
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

    //Setter per isRead
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    //Getter opzionale per isRead
    public boolean isRead(){
        return isRead;
    }
}
