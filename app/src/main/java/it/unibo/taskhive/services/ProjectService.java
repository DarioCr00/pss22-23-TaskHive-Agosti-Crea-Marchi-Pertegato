package it.unibo.taskhive.services;

import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.Task;
import it.unibo.taskhive.models.TaskPriority;
import it.unibo.taskhive.models.TaskStatus;
import it.unibo.taskhive.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ProjectService {

    private final ObservableList<Project> projects = FXCollections.observableArrayList();

    public ObservableList<Project> getProjects() {
        return projects;
    }

    public boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public void addProject(Project project) {
        if (project == null) {
            return;
        }

        if (project.getId() == null) {
            project.setId(new Random().nextLong());
        }

        if (project.getTasks() == null) {
            project.setTasks(new ArrayList<>());
        }

        projects.add(project);
    }

    public void deleteProject(Project project) {
        if (project == null) {
            return;
        }
        projects.remove(project);
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

        return projects.stream()
            .filter(project -> projectId.equals(project.getId()))
            .findFirst();
    }

    public void loadSampleData(List<User> users, TaskService taskService) {
        if (users == null || users.size() < 5 || !projects.isEmpty()) {
            return;
        }

        List<ProjectTemplate> templates = buildSampleTemplates(users);
        for (ProjectTemplate template : templates) {
            projects.add(instantiateProject(template, taskService));
        }
    }

    private Project instantiateProject(ProjectTemplate template, TaskService taskService) {
        List<Long> memberIds = new ArrayList<>();
        for (User member : template.memberUsers()) {
            if (member != null) {
                Long memberId = member.getId();
                if (!memberIds.contains(memberId)) {
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
        project.setId(new Random().nextLong());
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
                taskTemplate.followers(),
                taskService
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
        List<User> followers,
        TaskService taskService
    ) {
        List<Long> followerIds = followers.stream().map(User::getId).toList();
        Task task = new Task(
            title,
            description,
            status,
            priority,
            assignedUser != null ? assignedUser.getId() : null,
            project,
            dueDate,
            followerIds
        );
        taskService.ensureTaskId(task);
        return task;
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
