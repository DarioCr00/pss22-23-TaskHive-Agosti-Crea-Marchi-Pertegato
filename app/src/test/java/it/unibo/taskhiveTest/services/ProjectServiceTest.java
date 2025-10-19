package it.unibo.taskhiveTest.services;

import it.unibo.taskhive.database.HibernateUtil;
import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.User;
import it.unibo.taskhive.services.ProjectService;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest {

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        clearDatabase();
        projectService = new ProjectService();
        projectService.loadProjectsForUser(null);
    }

    private void clearDatabase() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("DELETE FROM Task").executeUpdate();
            session.createMutationQuery("DELETE FROM Project").executeUpdate();
            tx.commit();
        }
    }

    private User userWithId(long id, String username) {
        User user = new User(username, "pass", User.Role.USER);
        user.setId(id);
        return user;
    }

    @Test
    void isValidNameRejectsNullOrBlank() {
        assertFalse(projectService.isValidName(null), "Null names should be rejected");
        assertFalse(projectService.isValidName("   "), "Blank names should be rejected");
        assertTrue(projectService.isValidName("TaskHive"), "Non-empty names should be accepted");
    }

    @Test
    void getMembersReturnsOnlyProjectMembers() {
        Project project = new Project("Project", "Description", 1L, List.of());
        User userOne = userWithId(1L, "alice");
        User userTwo = userWithId(2L, "bob");
        User outsider = userWithId(3L, "charlie");
        project.setMembers(List.of(userOne.getId(), userTwo.getId()));
        projectService.addProject(project);

        List<User> members = projectService.getMembers(project, List.of(userOne, outsider, userTwo));

        assertEquals(List.of(userOne, userTwo), members, "Only users belonging to the project should be returned");
    }

    @Test
    void findByIdReturnsMatchingProject() {
        Project project = new Project("Project", "Description", 5L, List.of(5L));
        Project saved = projectService.addProject(project);

        Optional<Project> found = projectService.findById(saved.getId());

        assertTrue(found.isPresent(), "Project should be found by id");
        assertEquals(saved.getId(), found.orElseThrow().getId(), "Returned project should match the stored instance");
    }

    @Test
    void loadSampleDataPopulatesDemoProjectsOnlyOnce() {
        List<User> users = List.of(
            userWithId(1L, "alice"),
            userWithId(2L, "bob"),
            userWithId(3L, "charlie"),
            userWithId(4L, "diana"),
            userWithId(5L, "edward")
        );

        projectService.loadSampleData(users);

        assertEquals(10, projectService.getProjects().size(), "Ten sample projects should be loaded");
        assertTrue(projectService.getProjects().stream().allMatch(project -> !project.getTasks().isEmpty()),
            "Sample projects should contain demo tasks");

        Set<Long> firstIds = projectService.getProjects().stream()
            .map(Project::getId)
            .collect(Collectors.toSet());

        projectService.loadSampleData(users);

        assertEquals(10, projectService.getProjects().size(),
            "Calling loadSampleData again should not duplicate demo projects");
        Set<Long> secondIds = projectService.getProjects().stream()
            .map(Project::getId)
            .collect(Collectors.toSet());

        assertEquals(firstIds, secondIds,
            "Project identifiers should remain unchanged after reloading sample data");
    }

    @Test
    void loadProjectsForUserFiltersByMembership() {
        Project ownerProject = new Project("Owner", "Owned by user", 1L, List.of(1L));
        Project memberProject = new Project("Member", "Member access", 1L, List.of(1L, 2L));
        Project outsiderProject = new Project("Outsider", "No access", 3L, List.of(3L));

        Project savedOwner = projectService.addProject(ownerProject);
        Project savedMember = projectService.addProject(memberProject);
        Project savedOutsider = projectService.addProject(outsiderProject);

        List<Project> ownerVisible = projectService.loadProjectsForUser(1L);
        List<Long> ownerIds = ownerVisible.stream().map(Project::getId).toList();
        assertTrue(ownerIds.contains(savedOwner.getId()),
            "Owner should see their own project even if not explicitly listed as member");
        assertTrue(ownerIds.contains(savedMember.getId()), "Owner should see projects they own");
        assertFalse(ownerIds.contains(savedOutsider.getId()), "Owner should not see unrelated projects");

        List<Project> memberVisible = projectService.loadProjectsForUser(2L);
        List<Long> memberIds = memberVisible.stream().map(Project::getId).toList();
        assertTrue(memberIds.contains(savedMember.getId()), "Member should see shared project");
        assertFalse(memberIds.contains(savedOwner.getId()), "Member should not see private projects");
        assertFalse(memberIds.contains(savedOutsider.getId()), "Member should not see unrelated projects");
    }

    @Test
    void getMembersReturnsEmptyWhenProjectHasNoMembers() {
        Project project = new Project("Empty", "Description", List.of());
        project.setMembers(null);

        List<User> members = projectService.getMembers(project, List.of());

        assertTrue(members.isEmpty(), "Service should return an empty list when the project has no members");
    }
}
