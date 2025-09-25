package it.unibo.taskhiveTest.services;

import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.Task;
import it.unibo.taskhive.models.TaskPriority;
import it.unibo.taskhive.models.TaskStatus;
import it.unibo.taskhive.services.TaskService;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private final TaskService taskService = new TaskService();

    private Project createProject() {
        Long ownerId = new Random().nextLong();
        Project project = new Project("Project", "Description", ownerId, List.of(ownerId));
        project.setId(new Random().nextLong());
        project.setTasks(null);
        return project;
    }

    @Test
    void addTaskInitializesProjectTasksAndAssignsProject() {
        Project project = createProject();
        Task task = new Task("Title", "Description", TaskStatus.PENDING, TaskPriority.MEDIUM, null, null, null);

        taskService.addTask(project, task);

        assertNotNull(task.getId(), "Task id should be assigned");
        assertEquals(project, task.getProject(), "Task should be associated with the project");
        assertNotNull(project.getTasks(), "Project task list should be initialized");
        assertEquals(1, project.getTasks().size(), "Project should contain the added task");
        assertSame(task, project.getTasks().get(0), "Added task instance should be stored in the project");
    }

    @Test
    void toggleFollowAddsAndRemovesFollower() {
        Project project = createProject();
        Task task = new Task("Title", "Description", TaskStatus.PENDING, TaskPriority.HIGH, null, project, null);
        Long followerId = new Random().nextLong();

        boolean added = taskService.toggleFollow(task, followerId);
        assertTrue(added, "First toggle should add the follower");
        assertTrue(task.isFollowedBy(followerId), "Follower should be marked as following after addition");

        boolean removed = taskService.toggleFollow(task, followerId);
        assertFalse(removed, "Second toggle should remove the follower");
        assertFalse(task.isFollowedBy(followerId), "Follower should no longer follow after removal");
    }

    @Test
    void getTasksByStatusReturnsTasksSortedByPriority() {
        Project project = createProject();
        project.setTasks(new ArrayList<>());

        Task high = new Task("High", "", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null, project, null);
        Task medium = new Task("Medium", "", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, null, project, null);
        Task low = new Task("Low", "", TaskStatus.IN_PROGRESS, TaskPriority.LOW, null, project, null);
        Task otherStatus = new Task("Other", "", TaskStatus.PENDING, TaskPriority.HIGH, null, project, null);

        taskService.ensureTaskId(high);
        taskService.ensureTaskId(medium);
        taskService.ensureTaskId(low);
        taskService.ensureTaskId(otherStatus);

        project.getTasks().add(high);
        project.getTasks().add(low);
        project.getTasks().add(medium);
        project.getTasks().add(otherStatus);

        List<Task> result = taskService.getTasksByStatus(project, TaskStatus.IN_PROGRESS);

        assertEquals(3, result.size(), "Only tasks with matching status should be returned");
        assertEquals(List.of(high, medium, low), result, "Tasks should be ordered by priority");
    }

    @Test
    void getTasksByStatusReturnsEmptyWhenProjectHasNoTasks() {
        Project project = createProject();
        project.setTasks(null);

        List<Task> result = taskService.getTasksByStatus(project, TaskStatus.PENDING);

        assertTrue(result.isEmpty(), "Service should return an empty list when the project has no tasks");
    }
}
