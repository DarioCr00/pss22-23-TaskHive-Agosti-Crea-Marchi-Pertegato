package it.unibo.taskhive.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_user_id")
    private Long ownerUser;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    @ElementCollection
    @CollectionTable(name = "project_members", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "user_id")
    private List<Long> members;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Project() {
        this.members = new ArrayList<>();
    }

    public Project(String name, String description, List<Long> members) {
        this.name = name;
        this.description = description;
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    public Project(String name, String description, Long ownerUser, List<Long> members) {
        this.name = name;
        this.description = description;
        this.ownerUser = ownerUser;
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (members == null) {
            members = new ArrayList<>();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(Long ownerUser) {
        this.ownerUser = ownerUser;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<Long> getMembers() {
        return members;
    }

    public void setMembers(List<Long> members) {
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
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

    // Utility methods for managing members
    public void addMember(Long memberId) {
        if (members == null) {
            members = new ArrayList<>();
        }
        if (!members.contains(memberId)) {
            members.add(memberId);
        }
    }

    public void removeMember(Long memberId) {
        if (members != null) {
            members.remove(memberId);
        }
    }

    public boolean hasMember(Long userId) {
        return members != null && members.contains(userId);
    }

    public int getMembersCount() {
        return members != null ? members.size() : 0;
    }

    public boolean isOwner(Long userId) {
        return ownerUser != null && ownerUser.equals(userId);
    }

    @Override
    public String toString() {
        return name;
    }
}

