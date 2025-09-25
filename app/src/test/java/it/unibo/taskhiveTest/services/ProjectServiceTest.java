package it.unibo.taskhiveTest.services;

import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.User;
import it.unibo.taskhive.services.ProjectService;
import it.unibo.taskhive.services.TaskService;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest {

    @Test
    void isValidNameRejectsNullOrBlank() {
        ProjectService projectService = new ProjectService();

        assertFalse(projectService.isValidName(null), "Null names should be rejected");
        assertFalse(projectService.isValidName("   "), "Blank names should be rejected");
        assertTrue(projectService.isValidName("TaskHive"), "Non-empty names should be accepted");
    }

    @Test
    void getMembersReturnsOnlyProjectMembers() {
        ProjectService projectService = new ProjectService();
        Project project = new Project("Project", "Description", UUID.randomUUID(), List.of());
        UUID memberOne = UUID.randomUUID();
        UUID memberTwo = UUID.randomUUID();
        project.setMembers(List.of(memberOne, memberTwo));
        projectService.addProject(project);

        User userOne = new User(memberOne, "Alice", "alice@example.com");
        User userTwo = new User(memberTwo, "Bob", "bob@example.com");
        User outsider = new User(UUID.randomUUID(), "Charlie", "charlie@example.com");

        List<User> members = projectService.getMembers(project, List.of(userOne, outsider, userTwo));

        assertEquals(List.of(userOne, userTwo), members, "Only users belonging to the project should be returned");
    }

    @Test
    void findByIdReturnsMatchingProject() {
        ProjectService projectService = new ProjectService();
        Project project = new Project("Project", "Description", UUID.randomUUID(), List.of());
        projectService.addProject(project);

        Optional<Project> found = projectService.findById(project.getId());

        assertTrue(found.isPresent(), "Project should be found by id");
        assertEquals(project, found.orElseThrow(), "Returned project should match the stored instance");
    }

    @Test
    void loadSampleDataPopulatesDemoProjectsOnlyOnce() {
        ProjectService projectService = new ProjectService();
        TaskService taskService = new TaskService();
        List<User> users = List.of(
            new User(UUID.randomUUID(), "Alice", "alice@example.com"),
            new User(UUID.randomUUID(), "Bob", "bob@example.com"),
            new User(UUID.randomUUID(), "Charlie", "charlie@example.com"),
            new User(UUID.randomUUID(), "Diana", "diana@example.com"),
            new User(UUID.randomUUID(), "Edward", "edward@example.com")
        );

        projectService.loadSampleData(users, taskService);

        assertEquals(10, projectService.getProjects().size(), "Ten sample projects should be loaded");
        assertTrue(projectService.getProjects().stream().allMatch(project -> !project.getTasks().isEmpty()),
            "Sample projects should contain demo tasks");

        List<Project> firstLoadSnapshot = new ArrayList<>(projectService.getProjects());

        projectService.loadSampleData(users, taskService);

        assertEquals(firstLoadSnapshot, projectService.getProjects(),
            "Calling loadSampleData again should not duplicate demo projects");
    }

    @Test
    void getMembersReturnsEmptyWhenProjectHasNoMembers() {
        ProjectService projectService = new ProjectService();
        Project project = new Project("Empty", "Description", UUID.randomUUID(), List.of());
        project.setMembers(null);

        List<User> members = projectService.getMembers(project, List.of());

        assertTrue(members.isEmpty(), "Service should return an empty list when the project has no members");
    }
}
