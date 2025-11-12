package it.unibo.taskhive.controllers;

import it.unibo.taskhive.helpers.ProjectDialogHelper;
import it.unibo.taskhive.helpers.TaskDialogHelper;
import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.Task;
import it.unibo.taskhive.models.TaskStatus;
import it.unibo.taskhive.models.User;
import it.unibo.taskhive.services.NotificationService;
import it.unibo.taskhive.services.ProjectService;
import it.unibo.taskhive.services.SceneManager;
import it.unibo.taskhive.services.TaskService;
import it.unibo.taskhive.services.UserService;
import it.unibo.taskhive.services.SessionManager;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProjectViewController {

    @FXML private ListView<Project> projectListView;
    @FXML private Button addProjectButton;
    @FXML private Button editProjectButton;
    @FXML private Button deleteProjectButton;
    @FXML private Button notificationButton;
    @FXML private Button logoutButton;
    @FXML private Label projectTitleLabel;
    @FXML private Label projectDescriptionLabel;
    @FXML private HBox kanbanBoard;
    @FXML private Button addTaskButton;

    @FXML private Circle notificationDot;
    @FXML private StackPane notificationIconContainer;

    @FXML private URL location;
    @FXML private ResourceBundle resources;

    private final UserService userService = new UserService();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final TaskService taskService = new TaskService();
    private final ProjectService projectService = new ProjectService();
    private final NotificationService notificationService = NotificationService.getInstance();
    private FadeTransition blinkAnimation;
    private ScheduledExecutorService notificationChecker;

    private ProjectDialogHelper projectDialogHelper;
    private TaskDialogHelper taskDialogHelper;

    private ObservableList<Project> projects;
    private Project selectedProject;

    private VBox pendingColumn;
    private VBox inProgressColumn;
    private VBox completedColumn;
    private VBox blockedColumn;

    @FXML
    private void initialize() {
        projectDialogHelper = new ProjectDialogHelper(userService.getAllUsers());
        taskDialogHelper = new TaskDialogHelper(userService);

        projectService.loadSampleData(userService.getAllUsers());
        projects = projectService.loadProjectsForUser(getCurrentUserId());
        projectListView.setItems(projects);
        projectListView.setCellFactory(listView -> new ProjectListCell());
        projectListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectProject(newSel);
            }
        });

        setupButtons();
        setupKanbanBoard();

        if (!projects.isEmpty()) {
            projectListView.getSelectionModel().selectFirst();
        } else {
            handleNoProjectsState();
        }

        notificationService.setOnNotificationListener(this::showPopupNotification);
        notificationDot.setVisible(false);

        startNotificationChecker();
    }

    private void setupButtons() {
        addProjectButton.setOnAction(e -> showAddProjectDialog());
        editProjectButton.setOnAction(e -> showEditProjectDialog());
        deleteProjectButton.setOnAction(e -> deleteProject());
        addTaskButton.setOnAction(e -> showAddTaskDialog());

        notificationButton.setOnAction(e -> openNotificationsView());
        logoutButton.setOnAction(e -> showLogoutPlaceholder());

        editProjectButton.setDisable(true);
        deleteProjectButton.setDisable(true);
        addTaskButton.setDisable(true);
    }

    private void setupKanbanBoard() {
        kanbanBoard.setSpacing(20);
        kanbanBoard.setPadding(new Insets(20));

        pendingColumn = createKanbanColumn("PENDING", "#FF2A04");
        inProgressColumn = createKanbanColumn("IN PROGRESS", "#FFAF3D");
        completedColumn = createKanbanColumn("COMPLETED", "#00E200");
        blockedColumn = createKanbanColumn("BLOCKED", "#7B8089");

        kanbanBoard.getChildren().addAll(pendingColumn, inProgressColumn, completedColumn, blockedColumn);

        HBox.setHgrow(pendingColumn, Priority.ALWAYS);
        HBox.setHgrow(inProgressColumn, Priority.ALWAYS);
        HBox.setHgrow(completedColumn, Priority.ALWAYS);
        HBox.setHgrow(blockedColumn, Priority.ALWAYS);
    }

    private VBox createKanbanColumn(String title, String color) {
        VBox column = new VBox(10);
        column.getStyleClass().add("kanban-column");
        column.setMaxWidth(Double.MAX_VALUE);
        column.setPadding(new Insets(15));

        Label headerLabel = new Label(title);
        headerLabel.getStyleClass().add("kanban-header");
        headerLabel.setStyle("-fx-background-color: " + color + ";");
        headerLabel.setMaxWidth(Double.MAX_VALUE);

        VBox tasksContainer = new VBox(10);
        tasksContainer.getStyleClass().add("tasks-container");

        setupColumnDragAndDrop(tasksContainer, getStatusFromColumnTitle(title));

        column.getChildren().addAll(headerLabel, tasksContainer);
        return column;
    }

    private TaskStatus getStatusFromColumnTitle(String title) {
        return switch (title) {
            case "IN PROGRESS" -> TaskStatus.IN_PROGRESS;
            case "COMPLETED" -> TaskStatus.COMPLETED;
            case "BLOCKED" -> TaskStatus.BLOCKED;
            default -> TaskStatus.PENDING;
        };
    }

    private void setupColumnDragAndDrop(VBox tasksContainer, TaskStatus status) {
        tasksContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != tasksContainer && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        tasksContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString() && selectedProject != null) {
                try {
                    Long taskId = Long.parseLong(db.getString());
                    Optional<Task> taskOptional = taskService.findTaskById(selectedProject, taskId);
                    if (taskOptional.isPresent()) {
                        Task task = taskOptional.get();
                        if (task.getStatus() != status) {
                            taskService.moveTask(task, status);
                            projectService.persistProject(selectedProject, false);
                            refreshKanbanBoard();
                        }
                        success = true;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUID format
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void selectProject(Project project) {
        selectedProject = project;
        projectTitleLabel.setText(project.getName());

        StringBuilder descriptionText = new StringBuilder();
        if (project.getDescription() != null && !project.getDescription().trim().isEmpty()) {
            descriptionText.append(project.getDescription()).append("\n\n");
        }

        if (project.getOwnerUser() != null) {
            userService.findById(project.getOwnerUser())
                .ifPresent(owner -> descriptionText.append("👑 Owner: ").append(owner.getUsername()).append("\n"));
        }

        List<User> members = projectService.getMembers(project, userService.getAllUsers());
        if (!members.isEmpty()) {
            descriptionText.append("👥 Members: ");
            String names = members.stream().map(User::getUsername).reduce((a, b) -> a + ", " + b).orElse("");
            descriptionText.append(names);
        }

        String finalDescription = descriptionText.toString().trim();
        if (!finalDescription.isEmpty()) {
            projectDescriptionLabel.setText(finalDescription);
            projectDescriptionLabel.setVisible(true);
            projectDescriptionLabel.setManaged(true);
        } else {
            projectDescriptionLabel.setText("");
            projectDescriptionLabel.setVisible(false);
            projectDescriptionLabel.setManaged(false);
        }

        editProjectButton.setDisable(false);
        deleteProjectButton.setDisable(false);
        addTaskButton.setDisable(false);

        refreshKanbanBoard();
    }

    private void refreshKanbanBoard() {
        if (selectedProject == null) {
            clearKanbanBoard();
            return;
        }

        clearKanbanBoard();

        addTasksToColumn(pendingColumn, taskService.getTasksByStatus(selectedProject, TaskStatus.PENDING));
        addTasksToColumn(inProgressColumn, taskService.getTasksByStatus(selectedProject, TaskStatus.IN_PROGRESS));
        addTasksToColumn(completedColumn, taskService.getTasksByStatus(selectedProject, TaskStatus.COMPLETED));
        addTasksToColumn(blockedColumn, taskService.getTasksByStatus(selectedProject, TaskStatus.BLOCKED));
    }

    private void clearKanbanBoard() {
        ((VBox) pendingColumn.getChildren().get(1)).getChildren().clear();
        ((VBox) inProgressColumn.getChildren().get(1)).getChildren().clear();
        ((VBox) completedColumn.getChildren().get(1)).getChildren().clear();
        ((VBox) blockedColumn.getChildren().get(1)).getChildren().clear();
    }

    private void addTasksToColumn(VBox column, List<Task> tasks) {
        VBox tasksContainer = (VBox) column.getChildren().get(1);
        tasks.forEach(task -> tasksContainer.getChildren().add(createTaskCard(task)));
    }

    private VBox createTaskCard(Task task) {
        VBox card = new VBox(8);
        card.getStyleClass().add("task-card");
        card.setPadding(new Insets(10));

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-title");

        Label priorityLabel = new Label(task.getPriority().toString());
        priorityLabel.getStyleClass().add("task-priority");
        priorityLabel.getStyleClass().add("priority-" + task.getPriority().toString().toLowerCase());

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        header.getChildren().addAll(titleLabel, headerSpacer, priorityLabel);
        card.getChildren().add(header);

        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(task.getDescription());
            descLabel.getStyleClass().add("task-description");
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }

        Label assignedLabel = null;
        if (task.getAssignedUser() != null) {
            assignedLabel = userService.findById(task.getAssignedUser())
                .map(assigned -> {
                    Label label = new Label("👤 " + assigned.getUsername());
                    label.getStyleClass().add("task-assigned");
                    return label;
                })
                .orElse(null);
        }

        Label dueDateLabel = null;
        if (task.getDueDate() != null) {
            String dueDateText = "📅 " + task.getDueDate().toLocalDate();
            dueDateLabel = new Label(dueDateText);
            dueDateLabel.getStyleClass().add("task-due-date");

            LocalDate today = LocalDate.now();
            LocalDate dueDate = task.getDueDate().toLocalDate();
            if (dueDate.isBefore(today)) {
                dueDateLabel.getStyleClass().add("overdue");
            } else if (dueDate.isEqual(today) || dueDate.isBefore(today.plusDays(7))) {
                dueDateLabel.getStyleClass().add("due-soon");
            }
        }

        if (assignedLabel != null || dueDateLabel != null) {
            HBox footer = new HBox(8);
            footer.setAlignment(Pos.CENTER_LEFT);

            if (assignedLabel != null) {
                footer.getChildren().add(assignedLabel);
            }

            if (dueDateLabel != null) {
                Region footerSpacer = new Region();
                HBox.setHgrow(footerSpacer, Priority.ALWAYS);
                footer.getChildren().add(footerSpacer);
                footer.getChildren().add(dueDateLabel);
            }

            card.getChildren().add(footer);
        }

        HBox actionBox = new HBox(5);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button followBtn = createFollowButton(task);

        Button editBtn = new Button("✎");
        editBtn.getStyleClass().add("action-button");
        editBtn.setOnAction(e -> showEditTaskDialog(task));

        Button deleteBtn = new Button("×");
        deleteBtn.getStyleClass().add("action-button");
        deleteBtn.setOnAction(e -> deleteTask(task));

        actionBox.getChildren().addAll(followBtn, editBtn, deleteBtn);

        card.getChildren().add(actionBox);

        taskService.ensureTaskId(task);
        setupTaskDragAndDrop(card, task);

        return card;
    }

    private Button createFollowButton(Task task) {
        Long currentUserId = getCurrentUserId();
        boolean isFollowing = task.isFollowedBy(currentUserId);

        Button followBtn = new Button(isFollowing ? "🔕" : "🔔");
        followBtn.getStyleClass().add("action-button");
        followBtn.getStyleClass().add(isFollowing ? "following-button" : "follow-button");

        Tooltip tooltip = new Tooltip(isFollowing ? "Unfollow this task" : "Follow this task");
        followBtn.setTooltip(tooltip);

        followBtn.setOnAction(e -> {
            boolean nowFollowing = taskService.toggleFollow(task, currentUserId);
            projectService.persistProject(selectedProject, false);
            showInfoAlert(nowFollowing
                ? "You are now following the task: " + task.getTitle()
                : "You unfollowed the task: " + task.getTitle());
            refreshKanbanBoard();
        });

        return followBtn;
    }

    private Long getCurrentUserId() {
        return sessionManager.getCurrentUser().getId();
    }

    private void selectProjectById(Long projectId) {
        if (projectId == null) {
            return;
        }
        projects.stream()
            .filter(project -> projectId.equals(project.getId()))
            .findFirst()
            .ifPresent(project -> projectListView.getSelectionModel().select(project));
    }

    private void setupTaskDragAndDrop(VBox taskCard, Task task) {
        taskCard.setOnDragDetected(event -> {
            if (task.getId() != null) {
                Dragboard db = taskCard.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(task.getId().toString());
                db.setContent(content);
                taskCard.setOpacity(0.5);
                event.consume();
            }
        });

        taskCard.setOnDragDone(event -> {
            taskCard.setOpacity(1.0);
            event.consume();
        });
    }

    private void showAddProjectDialog() {
        Optional<Project> result = projectDialogHelper.showCreateDialog();
        result.ifPresent(project -> {
            if (!projectService.isValidName(project.getName())) {
                showErrorAlert("Project name cannot be empty.");
                return;
            }
            Project savedProject = projectService.addProject(project, getCurrentUserId());
            selectProjectById(savedProject != null ? savedProject.getId() : null);
        });
    }

    private void showEditProjectDialog() {
        if (selectedProject == null) {
            return;
        }

        Optional<Project> result = projectDialogHelper.showEditDialog(selectedProject);
        result.ifPresent(project -> {
            if (!projectService.isValidName(project.getName())) {
                showErrorAlert("Project name cannot be empty.");
                return;
            }
            Project updatedProject = projectService.updateProject(project, getCurrentUserId());
            selectProjectById(updatedProject != null ? updatedProject.getId() : null);
        });
    }

    private void deleteProject() {
        if (selectedProject == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Project");
        alert.setHeaderText("Are you sure you want to delete this project?");
        alert.setContentText("This action cannot be undone. All tasks in this project will be deleted.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            Long deleterId = getCurrentUserId();
            projectService.deleteProject(selectedProject, deleterId);

            if (!projects.isEmpty()) {
                projectListView.getSelectionModel().selectFirst();
                selectProject(projectListView.getSelectionModel().getSelectedItem());
            } else {
                selectedProject = null;
                handleNoProjectsState();
                clearKanbanBoard();
            }
        }  
    }

    private void handleNoProjectsState() {
        projectTitleLabel.setText("Use the sidebar to select a project or create a new one");
        projectDescriptionLabel.setText("");
        projectDescriptionLabel.setVisible(false);
        projectDescriptionLabel.setManaged(false);
        editProjectButton.setDisable(true);
        deleteProjectButton.setDisable(true);
        addTaskButton.setDisable(true);
    }

    private void showAddTaskDialog() {
        if (selectedProject == null) {
            return;
        }

        Optional<Task> result = taskDialogHelper.showCreateDialog(selectedProject);
        result.ifPresent(task -> {
            taskService.addTask(selectedProject, task, getCurrentUserId());
            projectService.persistProject(selectedProject, false);
            refreshKanbanBoard();
        });
    }

    private void showEditTaskDialog(Task task) {
        Optional<Task> result = taskDialogHelper.showEditDialog(selectedProject, task);
        result.ifPresent(ignored -> {
            projectService.persistProject(selectedProject, false);
            refreshKanbanBoard();
        });
    }

    private void deleteTask(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Task");
        alert.setHeaderText("Are you sure you want to delete this task?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            Long deleterId = getCurrentUserId();
            taskService.deleteTask(selectedProject, task, deleterId);
            projectService.persistProject(selectedProject, false);
            refreshKanbanBoard();
        }
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Notifications Functionality");
        alert.setContentText("COMING SOON!");
        alert.showAndWait();
    }

    private void showLogoutPlaceholder() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userService.logout();
            SceneManager.switchScene("fxml/LoginView.fxml");
        }
    }

    private static class ProjectListCell extends ListCell<Project> {
        @Override
        protected void updateItem(Project project, boolean empty) {
            super.updateItem(project, empty);
            if (empty || project == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(project.getName());
                getStyleClass().add("project-list-item");
            }
        }
    }

    @FXML //momentanea, solo per vedere l'update delle notifiche se viene correttamente inserito all'interno della pagina delle notifiche
    private void openNotificationsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NotificationView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1920, 1080);
            
            Stage stage = (Stage) notificationButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPopupNotification(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🔔 Notifica");
            alert.setHeaderText("Nuova Notifica");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void setNotificationAlert(boolean hasUnreadNotifications) {
        javafx.application.Platform.runLater(() -> {
            if (hasUnreadNotifications) {
                notificationDot.setVisible(true);

                // Se non c’è già un’animazione in corso la crea
                if (blinkAnimation == null) {
                    blinkAnimation = new FadeTransition(Duration.seconds(0.8), notificationDot);
                    blinkAnimation.setFromValue(1.0);
                    blinkAnimation.setToValue(0.2);
                    blinkAnimation.setCycleCount(FadeTransition.INDEFINITE);
                    blinkAnimation.setAutoReverse(true);
                    blinkAnimation.play();
                }
            } else {
                notificationDot.setVisible(false);

                if (blinkAnimation != null) {
                    blinkAnimation.stop();
                    blinkAnimation = null;
                    notificationDot.setOpacity(1.0); // reset visibilità
                }
            }
        });
    }

    private void startNotificationChecker() {
        notificationChecker = Executors.newSingleThreadScheduledExecutor();
        notificationChecker.scheduleAtFixedRate(() -> {
            try {
                boolean hasUnread = notificationService.hasUnreadNotifications(); 
                setNotificationAlert(hasUnread);
            } catch (Exception e) {
                System.err.println("[NotificationChecker] Errore durante il controllo notifiche: " + e.getMessage());
            }
        }, 0, 15, TimeUnit.SECONDS);
    }

    public void stopNotificationChecker() {
        if (notificationChecker != null && !notificationChecker.isShutdown()) {
            notificationChecker.shutdownNow();
        }
    }
}
