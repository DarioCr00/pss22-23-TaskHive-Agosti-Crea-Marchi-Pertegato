package it.unibo.taskhive.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Column(name = "assigned_user_id")
    private Long assignedUser;

    @ElementCollection
    @CollectionTable(name = "task_followers", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "follower_user_id")
    private List<Long> followers;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    private LocalDateTime dueDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Task() {
        this.followers = new ArrayList<>();
    }

    public Task(String title, String description, TaskStatus status, TaskPriority priority,
                Long assignedUser, Project project, LocalDateTime dueDate) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedUser = assignedUser;
        this.project = project;
        this.dueDate = dueDate;
        this.followers = new ArrayList<>();
    }

    public Task(String title, String description, TaskStatus status, TaskPriority priority,
                Long assignedUser, Project project, LocalDateTime dueDate, List<Long> followers) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedUser = assignedUser;
        this.project = project;
        this.dueDate = dueDate;
        this.followers = followers != null ? new ArrayList<>(followers) : new ArrayList<>();
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (followers == null) {
            followers = new ArrayList<>();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Long getAssignedUser() {
        return assignedUser;
    }

    public void setAssignedUser(Long assignedUser) {
        this.assignedUser = assignedUser;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Long> getFollowers() {
        return followers;
    }

    public void setFollowers(List<Long> followers) {
        this.followers = followers != null ? new ArrayList<>(followers) : new ArrayList<>();
    }

    public void addFollower(Long followerId) {
        if (followers == null) {
            followers = new ArrayList<>();
        }
        if (!followers.contains(followerId)) {
            followers.add(followerId);
        }
    }

    public void removeFollower(Long followerId) {
        if (followers != null) {
            followers.remove(followerId);
        }
    }

    public boolean isFollowedBy(Long userId) {
        return followers != null && followers.contains(userId);
    }

    public int getFollowersCount() {
        return followers != null ? followers.size() : 0;
    }

    @Override
    public String toString() {
        return title;
    }
}

