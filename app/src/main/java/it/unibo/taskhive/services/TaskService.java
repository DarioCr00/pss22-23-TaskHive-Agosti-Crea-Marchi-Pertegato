package it.unibo.taskhive.services;

import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.Task;
import it.unibo.taskhive.models.TaskPriority;
import it.unibo.taskhive.models.TaskStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class TaskService {

    private final Comparator<Task> priorityComparator = Comparator.comparingInt(this::priorityOrder);

    private final NotificationService notificationService = NotificationService.getInstance();

    public void ensureTaskId(Task task) {
        if (task.getId() == null) {
            task.setId(new Random().nextLong());
        }
    }

    public void addTask(Project project, Task task, Long creatorId) {
        if (project == null || task == null) {
            return;
        }

        if (project.getTasks() == null) {
            project.setTasks(new ArrayList<>());
        }

        ensureTaskId(task);
        task.setProject(project);
        project.getTasks().add(task);

        notificationService.notifyTaskCreated(task, creatorId);
    }

    public void updateTask(Project project, Task updatedTask, Long updaterId) {
        if (project == null || updatedTask == null) {
            return;
        }

        //rest of the logic to be implemented here.

        notificationService.notifyTaskUpdated(updatedTask, updaterId);
    }

    public void deleteTask(Project project, Task task, Long deleterId) {
        if (project == null || task == null || project.getTasks() == null) {
            return;
        }
        project.getTasks().remove(task);

        notificationService.notifyTaskDeleted(task, deleterId);
    }

    public void moveTask(Task task, TaskStatus status) {
        if (task != null && status != null) {
            task.setStatus(status);
        }
    }

    public boolean toggleFollow(Task task, Long userId) {
        if (task == null || userId == null) {
            return false;
        }

        if (task.isFollowedBy(userId)) {
            task.removeFollower(userId);
            return false;
        }

        task.addFollower(userId);
        return true;
    }

    public Optional<Task> findTaskById(Project project, Long taskId) {
        if (project == null || project.getTasks() == null || taskId == null) {
            return Optional.empty();
        }

        return project.getTasks().stream()
            .filter(task -> taskId.equals(task.getId()))
            .findFirst();
    }

    public List<Task> getTasksByStatus(Project project, TaskStatus status) {
        if (project == null || project.getTasks() == null) {
            return List.of();
        }

        return project.getTasks().stream()
            .filter(task -> task.getStatus() == status)
            .sorted(priorityComparator)
            .collect(Collectors.toList());
    }

    public Comparator<Task> getPriorityComparator() {
        return priorityComparator;
    }

    private int priorityOrder(Task task) {
        TaskPriority priority = task != null ? task.getPriority() : null;
        if (priority == null) {
            return 4;
        }

        return switch (priority) {
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }
}
