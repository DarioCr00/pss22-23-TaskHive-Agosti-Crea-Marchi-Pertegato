package it.unibo.taskhive.services;

import it.unibo.taskhive.database.HibernateUtil;
import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.Task;
import it.unibo.taskhive.models.TaskPriority;
import it.unibo.taskhive.models.TaskStatus;
import it.unibo.taskhive.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ProjectService {

    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private Long lastLoadedUserId;

    public ObservableList<Project> getProjects() {
        return projects;
    }

    public ObservableList<Project> loadProjectsForUser(Long userId) {
        lastLoadedUserId = userId;
        projects.setAll(fetchProjects(userId));
        return projects;
    }

    public void refreshCachedProjects() {
        projects.setAll(fetchProjects(lastLoadedUserId));
    }

    public boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public Project addProject(Project project) {
        if (project == null) {
            return null;
        }
        ensureProjectCollections(project);
        executeInTransaction(session -> session.persist(project));
        refreshCachedProjects();
        return findInCache(project.getId()).orElse(project);
    }

    public Project updateProject(Project project) {
        return persistProject(project, true);
    }

    public Project persistProject(Project project, boolean refreshCache) {
        if (project == null || project.getId() == null) {
            return null;
        }
        ensureProjectCollections(project);
        executeInTransaction(session -> session.merge(project));
        if (refreshCache) {
            refreshCachedProjects();
            return findInCache(project.getId()).orElse(project);
        }
        return project;
    }

    public void deleteProject(Project project) {
        if (project == null || project.getId() == null) {
            return;
        }

        executeInTransaction(session -> {
            Project managed = session.get(Project.class, project.getId());
            if (managed != null) {
                session.remove(managed);
            }
        });

        refreshCachedProjects();
    }

    public List<User> getMembers(Project project, List<User> availableUsers) {
        if (project == null || project.getMembers() == null) {
            return List.of();
        }

        return availableUsers.stream()
            .filter(user -> project.getMembers().contains(user.getId()))
            .toList();
    }

    public Optional<Project> findById(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }

        Optional<Project> cached = findInCache(projectId);
        if (cached.isPresent()) {
            return cached;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Project project = session.get(Project.class, projectId);
            if (project != null) {
                initializeAssociations(session, project);
            }
            return Optional.ofNullable(project);
        }
    }

    public void loadSampleData(List<User> users) {
        if (!shouldLoadSampleData(users)) {
            return;
        }

        executeInTransaction(session -> {
            List<ProjectTemplate> templates = buildSampleTemplates(users);
            for (ProjectTemplate template : templates) {
                Project project = instantiateProject(template);
                session.persist(project);
            }
        });

        refreshCachedProjects();
    }

    private List<Project> fetchProjects(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Project> result = session.createQuery(
                    "SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.tasks t",
                    Project.class
                )
                .getResultList();

            List<Project> deduplicated = deduplicateById(result);
            for (Project project : deduplicated) {
                initializeAssociations(session, project);
            }

            if (userId == null) {
                return deduplicated;
            }

            return deduplicated.stream()
                .filter(project -> isOwnerOrMember(project, userId))
                .toList();
        }
    }

    private List<Project> deduplicateById(List<Project> projects) {
        Map<Long, Project> unique = new LinkedHashMap<>();
        for (Project project : projects) {
            unique.put(project.getId(), project);
        }
        return new ArrayList<>(unique.values());
    }

    private void initializeAssociations(Session session, Project project) {
        Hibernate.initialize(project.getMembers());
        if (project.getMembers() == null) {
            project.setMembers(new ArrayList<>());
        }

        Hibernate.initialize(project.getTasks());
        if (project.getTasks() == null) {
            project.setTasks(new ArrayList<>());
        } else {
            for (Task task : project.getTasks()) {
                task.setProject(project);
                Hibernate.initialize(task.getFollowers());
                if (task.getFollowers() == null) {
                    task.setFollowers(new ArrayList<>());
                }
            }
        }
    }

    private boolean isOwnerOrMember(Project project, Long userId) {
        if (userId == null) {
            return true;
        }
        boolean isOwner = project.getOwnerUser() != null && project.getOwnerUser().equals(userId);
        boolean isMember = project.getMembers() != null && project.getMembers().contains(userId);
        return isOwner || isMember;
    }

    private Optional<Project> findInCache(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return projects.stream()
            .filter(project -> projectId.equals(project.getId()))
            .findFirst();
    }

    private boolean shouldLoadSampleData(List<User> users) {
        if (users == null || users.size() < 5) {
            return false;
        }
        return !hasAnyProject();
    }

    private boolean hasAnyProject() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(p) FROM Project p", Long.class)
                .uniqueResult();
            return count != null && count > 0;
        }
    }

    private void ensureProjectCollections(Project project) {
        if (project.getMembers() == null) {
            project.setMembers(new ArrayList<>());
        }
        if (project.getTasks() == null) {
            project.setTasks(new ArrayList<>());
        }
    }

    private void executeInTransaction(Consumer<Session> work) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            work.accept(session);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Failed to execute transaction", e);
        }
    }

    private Project instantiateProject(ProjectTemplate template) {
        List<Long> memberIds = new ArrayList<>();
        for (User member : template.memberUsers()) {
            if (member != null) {
                Long memberId = member.getId();
                if (memberId != null && !memberIds.contains(memberId)) {
                    memberIds.add(memberId);
                }
            }
        }

        Long ownerId = template.owner().getId();
        if (!memberIds.contains(ownerId)) {
            memberIds.add(ownerId);
        }

        Project project = new Project(
            template.name(),
            template.description(),
            ownerId,
            memberIds
        );
        project.setTasks(new ArrayList<>());

        for (TaskTemplate taskTemplate : template.tasks()) {
            Task task = createTask(
                taskTemplate.title(),
                taskTemplate.description(),
                taskTemplate.status(),
                taskTemplate.priority(),
                taskTemplate.assignedUser(),
                project,
                taskTemplate.dueDate(),
                taskTemplate.followers()
            );
            project.getTasks().add(task);
        }

        return project;
    }

    private List<ProjectTemplate> buildSampleTemplates(List<User> users) {
        LocalDateTime now = LocalDateTime.now();

        User alice = users.get(0);
        User bob = users.get(1);
        User charlie = users.get(2);
        User diana = users.get(3);
        User edward = users.get(4);

        return List.of(
            new ProjectTemplate(
                "Website Redesign",
                "Refresh company website with modern UI/UX",
                alice,
                List.of(alice, bob, charlie),
                List.of(
                    new TaskTemplate("Design Homepage", "Create new homepage design", TaskStatus.COMPLETED, TaskPriority.HIGH, alice, now.minusDays(2), List.of(alice, bob)),
                    new TaskTemplate("Implement Navigation", "Code the new navigation system", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, bob, now.plusDays(5), List.of(alice)),
                    new TaskTemplate("Content Migration", "Migrate old content to new site", TaskStatus.PENDING, TaskPriority.LOW, charlie, now.plusDays(10), List.of()),
                    new TaskTemplate("Performance Testing", "Test site performance", TaskStatus.BLOCKED, TaskPriority.HIGH, null, now.plusDays(1), List.of(alice, bob)),
                    new TaskTemplate("SEO Optimization", "Optimize for search engines", TaskStatus.PENDING, TaskPriority.HIGH, alice, now.minusDays(1), List.of()),
                    new TaskTemplate("Browser Testing", "Test on different browsers", TaskStatus.PENDING, TaskPriority.MEDIUM, bob, null, List.of())
                )
            ),
            new ProjectTemplate(
                "Mobile App",
                "Develop mobile application for iOS and Android platforms",
                bob,
                List.of(bob, diana, edward),
                List.of(
                    new TaskTemplate("Setup Development Environment", "Configure dev environment", TaskStatus.COMPLETED, TaskPriority.HIGH, bob, now.minusDays(5), List.of()),
                    new TaskTemplate("Create User Authentication", "Implement login/signup", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, diana, now.plusDays(3), List.of(bob)),
                    new TaskTemplate("Design UI/UX", "Create app interface design", TaskStatus.PENDING, TaskPriority.MEDIUM, diana, now.plusDays(7), List.of()),
                    new TaskTemplate("Write Documentation", "Create user manual", TaskStatus.PENDING, TaskPriority.LOW, null, null, List.of())
                )
            ),
            new ProjectTemplate(
                "Marketing Campaign",
                "Launch new brand awareness campaign across channels",
                charlie,
                List.of(charlie, alice, diana),
                List.of(
                    new TaskTemplate("Market Research", "Gather insights on target audience", TaskStatus.COMPLETED, TaskPriority.HIGH, charlie, now.minusDays(4), List.of(alice)),
                    new TaskTemplate("Create Content Calendar", "Plan weekly campaign content", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, diana, now.plusDays(4), List.of(charlie)),
                    new TaskTemplate("Launch Social Ads", "Start paid advertisement campaigns", TaskStatus.PENDING, TaskPriority.HIGH, charlie, now.plusDays(7), List.of(alice, diana)),
                    new TaskTemplate("Prepare Press Release", "Draft launch press release", TaskStatus.PENDING, TaskPriority.MEDIUM, null, now.plusDays(9), List.of())
                )
            ),
            new ProjectTemplate(
                "Data Migration",
                "Move customer data to new CRM platform",
                diana,
                List.of(diana, charlie, edward),
                List.of(
                    new TaskTemplate("Audit Legacy Systems", "Document existing data sources", TaskStatus.COMPLETED, TaskPriority.MEDIUM, diana, now.minusDays(3), List.of(edward)),
                    new TaskTemplate("Map Data Schemas", "Define mapping rules for import", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, charlie, now.plusDays(2), List.of(diana)),
                    new TaskTemplate("Set Up ETL Pipeline", "Automate data migration jobs", TaskStatus.BLOCKED, TaskPriority.HIGH, edward, now.plusDays(5), List.of(diana)),
                    new TaskTemplate("Validate Migrated Data", "Verify new CRM contains clean data", TaskStatus.PENDING, TaskPriority.HIGH, diana, now.plusDays(8), List.of(charlie))
                )
            ),
            new ProjectTemplate(
                "DevOps Automation",
                "Automate infrastructure provisioning and deployments",
                edward,
                List.of(edward, bob, alice, charlie, diana),
                List.of(
                    new TaskTemplate("Define CI/CD Strategy", "Outline deployment workflows", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, edward, now.plusDays(1), List.of(bob)),
                    new TaskTemplate("Create Terraform Modules", "Templatize infrastructure resources", TaskStatus.PENDING, TaskPriority.MEDIUM, edward, now.plusDays(6), List.of(alice)),
                    new TaskTemplate("Containerize Services", "Add Docker support to services", TaskStatus.PENDING, TaskPriority.HIGH, bob, now.plusDays(4), List.of(edward)),
                    new TaskTemplate("Monitoring Setup", "Configure alerts and dashboards", TaskStatus.PENDING, TaskPriority.MEDIUM, charlie, null, List.of(diana))
                )
            ),
            new ProjectTemplate(
                "Customer Onboarding",
                "Improve onboarding journey for new customers",
                alice,
                List.of(alice, diana, bob),
                List.of(
                    new TaskTemplate("Map Customer Journey", "Document every onboarding step", TaskStatus.COMPLETED, TaskPriority.HIGH, alice, now.minusDays(5), List.of(diana)),
                    new TaskTemplate("Design Tutorial Videos", "Create walkthrough videos", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, diana, now.plusDays(3), List.of(alice)),
                    new TaskTemplate("Implement Drip Campaign", "Automate welcome emails", TaskStatus.PENDING, TaskPriority.MEDIUM, bob, now.plusDays(10), List.of(diana)),
                    new TaskTemplate("Collect Feedback", "Survey recent onboarded customers", TaskStatus.PENDING, TaskPriority.LOW, null, null, List.of())
                )
            ),
            new ProjectTemplate(
                "Analytics Dashboard",
                "Build unified analytics dashboard for leadership",
                bob,
                List.of(bob, alice, charlie, edward, diana),
                List.of(
                    new TaskTemplate("Define KPIs", "Agree on dashboard metrics", TaskStatus.COMPLETED, TaskPriority.HIGH, bob, now.minusDays(1), List.of(charlie)),
                    new TaskTemplate("Integrate Data Sources", "Connect to data warehouse", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, edward, now.plusDays(4), List.of(bob, diana)),
                    new TaskTemplate("Design Dashboard Layout", "Create initial dashboard wireframes", TaskStatus.PENDING, TaskPriority.MEDIUM, alice, now.plusDays(7), List.of(charlie, edward)),
                    new TaskTemplate("User Acceptance Testing", "Pilot dashboard with stakeholders", TaskStatus.PENDING, TaskPriority.LOW, charlie, now.plusDays(10), List.of(diana))
                )
            ),
            new ProjectTemplate(
                "Internal Training",
                "Roll out updated internal training program",
                charlie,
                List.of(charlie, alice, bob, diana, edward),
                List.of(
                    new TaskTemplate("Identify Learning Goals", "Collect skill gaps from teams", TaskStatus.COMPLETED, TaskPriority.MEDIUM, diana, now.minusDays(6), List.of(alice)),
                    new TaskTemplate("Create Training Materials", "Draft slide decks and exercises", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, alice, now.plusDays(2), List.of(bob)),
                    new TaskTemplate("Schedule Workshops", "Plan live training sessions", TaskStatus.PENDING, TaskPriority.LOW, charlie, now.plusDays(5), List.of(edward)),
                    new TaskTemplate("Gather Feedback", "Evaluate training impact", TaskStatus.PENDING, TaskPriority.LOW, null, null, List.of())
                )
            ),
            new ProjectTemplate(
                "Research Initiative",
                "Explore innovative product opportunities",
                diana,
                List.of(diana, charlie, alice, edward, bob),
                List.of(
                    new TaskTemplate("Draft Research Proposal", "Define research scope and goals", TaskStatus.COMPLETED, TaskPriority.HIGH, charlie, now.minusDays(7), List.of(diana)),
                    new TaskTemplate("Conduct Interviews", "Interview key customer segments", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, alice, now.plusDays(8), List.of(charlie, bob)),
                    new TaskTemplate("Prototype Concepts", "Create quick prototypes for testing", TaskStatus.PENDING, TaskPriority.HIGH, edward, now.plusDays(12), List.of(diana)),
                    new TaskTemplate("Publish Findings", "Share research summary with team", TaskStatus.PENDING, TaskPriority.LOW, null, null, List.of())
                )
            ),
            new ProjectTemplate(
                "Customer Support Revamp",
                "Enhance support responsiveness and tooling",
                edward,
                List.of(edward, alice, diana, bob, charlie),
                List.of(
                    new TaskTemplate("Audit Support Tickets", "Review recent support history", TaskStatus.COMPLETED, TaskPriority.HIGH, edward, now.minusDays(2), List.of(alice)),
                    new TaskTemplate("Define Response Playbooks", "Document standard responses", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, bob, now.plusDays(3), List.of(diana, alice)),
                    new TaskTemplate("Implement Chatbot", "Roll out support chatbot MVP", TaskStatus.BLOCKED, TaskPriority.HIGH, edward, now.plusDays(9), List.of(bob)),
                    new TaskTemplate("Train Support Team", "Run new tooling workshops", TaskStatus.PENDING, TaskPriority.MEDIUM, diana, now.plusDays(11), List.of(charlie))
                )
            )
        );
    }

    private Task createTask(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        User assignedUser,
        Project project,
        LocalDateTime dueDate,
        List<User> followers
    ) {
        List<Long> followerIds = followers.stream().map(User::getId).toList();
        return new Task(
            title,
            description,
            status,
            priority,
            assignedUser != null ? assignedUser.getId() : null,
            project,
            dueDate,
            followerIds
        );
    }

    private record ProjectTemplate(
        String name,
        String description,
        User owner,
        List<User> memberUsers,
        List<TaskTemplate> tasks
    ) {
        ProjectTemplate {
            memberUsers = memberUsers != null ? List.copyOf(memberUsers) : List.of();
            tasks = tasks != null ? List.copyOf(tasks) : List.of();
        }
    }

    private record TaskTemplate(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        User assignedUser,
        LocalDateTime dueDate,
        List<User> followers
    ) {
        TaskTemplate {
            followers = followers != null ? List.copyOf(followers) : List.of();
        }
    }
}
