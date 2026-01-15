package it.unibo.taskhive.helpers;

import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.Task;
import it.unibo.taskhive.models.TaskPriority;
import it.unibo.taskhive.models.TaskStatus;
import it.unibo.taskhive.models.User;
import it.unibo.taskhive.services.UserService;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskDialogHelper {

    private final UserService userService;

    public TaskDialogHelper(UserService userService) {
        this.userService = userService;
    }

    public Optional<Task> showCreateDialog(Project project) {
        if (project == null) {
            return Optional.empty();
        }

        List<User> projectMembers = userService.resolveUsers(project.getMembers());

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add new task");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = buildTaskForm();

        TextField titleField = new TextField();
        titleField.setPromptText("Task title");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Task description");
        descriptionArea.setPrefRowCount(3);

        ComboBox<TaskStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(TaskStatus.values());
        statusCombo.setValue(TaskStatus.PENDING);

        ComboBox<TaskPriority> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(TaskPriority.values());
        priorityCombo.setValue(TaskPriority.MEDIUM);

        ComboBox<User> assignedUserCombo = new ComboBox<>();
        assignedUserCombo.setItems(FXCollections.observableArrayList(projectMembers));
        assignedUserCombo.setPromptText("Select assigned user (optional)");

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Select due date (optional)");

        ListView<User> followersListView = new ListView<>();
        followersListView.setItems(FXCollections.observableArrayList(projectMembers));
        followersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        followersListView.setPrefHeight(100);

        addTaskFormRows(grid, titleField, descriptionArea, statusCombo, priorityCombo, assignedUserCombo, dueDatePicker, followersListView);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                User assignedUser = assignedUserCombo.getValue();
                List<User> selectedFollowers = new ArrayList<>(followersListView.getSelectionModel().getSelectedItems());
                LocalDate dueDate = dueDatePicker.getValue();

                List<Long> followerIds = selectedFollowers.stream().map(User::getId).toList();
                LocalDateTime dueDateTime = dueDate != null ? dueDate.atTime(23, 59, 59) : null;

                return new Task(
                    titleField.getText(),
                    descriptionArea.getText(),
                    statusCombo.getValue(),
                    priorityCombo.getValue(),
                    assignedUser != null ? assignedUser.getId() : null,
                    project,
                    dueDateTime,
                    followerIds
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public Optional<Task> showEditDialog(Project project, Task task) {
        if (project == null || task == null) {
            return Optional.empty();
        }

        List<User> projectMembers = userService.resolveUsers(project.getMembers());
        List<User> allUsers = userService.getAllUsers();

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Edit task details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = buildTaskForm();

        TextField titleField = new TextField(task.getTitle());
        TextArea descriptionArea = new TextArea(task.getDescription());
        descriptionArea.setPrefRowCount(3);

        ComboBox<TaskStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(TaskStatus.values());
        statusCombo.setValue(task.getStatus());

        ComboBox<TaskPriority> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(TaskPriority.values());
        priorityCombo.setValue(task.getPriority());

        ComboBox<User> assignedUserCombo = new ComboBox<>();
        assignedUserCombo.setItems(FXCollections.observableArrayList(projectMembers));
        assignedUserCombo.setPromptText("Select assigned user (optional)");
        if (task.getAssignedUser() != null) {
            allUsers.stream()
                .filter(user -> user.getId().equals(task.getAssignedUser()))
                .findFirst()
                .ifPresent(assignedUserCombo::setValue);
        }

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Select due date (optional)");
        if (task.getDueDate() != null) {
            dueDatePicker.setValue(task.getDueDate().toLocalDate());
        }

        ListView<User> followersListView = new ListView<>();
        followersListView.setItems(FXCollections.observableArrayList(projectMembers));
        followersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        followersListView.setPrefHeight(100);

        if (task.getFollowers() != null) {
            for (Long followerId : task.getFollowers()) {
                allUsers.stream()
                    .filter(user -> user.getId().equals(followerId))
                    .findFirst()
                    .ifPresent(user -> followersListView.getSelectionModel().select(user));
            }
        }

        addTaskFormRows(grid, titleField, descriptionArea, statusCombo, priorityCombo, assignedUserCombo, dueDatePicker, followersListView);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                User assignedUser = assignedUserCombo.getValue();
                List<User> selectedFollowers = new ArrayList<>(followersListView.getSelectionModel().getSelectedItems());
                LocalDate dueDate = dueDatePicker.getValue();

                List<Long> followerIds = selectedFollowers.stream().map(User::getId).toList();
                LocalDateTime dueDateTime = dueDate != null ? dueDate.atTime(23, 59, 59) : null;

                task.setTitle(titleField.getText());
                task.setDescription(descriptionArea.getText());
                task.setStatus(statusCombo.getValue());
                task.setPriority(priorityCombo.getValue());
                task.setAssignedUser(assignedUser != null ? assignedUser.getId() : null);
                task.setDueDate(dueDateTime);
                task.setFollowers(followerIds);
                return task;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private GridPane buildTaskForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        return grid;
    }

    private void addTaskFormRows(
        GridPane grid,
        TextField titleField,
        TextArea descriptionArea,
        ComboBox<TaskStatus> statusCombo,
        ComboBox<TaskPriority> priorityCombo,
        ComboBox<User> assignedUserCombo,
        DatePicker dueDatePicker,
        ListView<User> followersListView
    ) {
        Label followersLabel = new Label("Select followers (hold Ctrl/Cmd for multiple):");

        int row = 0;
        grid.add(new Label("Title:"), 0, row);
        grid.add(titleField, 1, row++);
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++);

        grid.add(new Label("Status:"), 0, row);
        grid.add(statusCombo, 1, row++);

        grid.add(new Label("Priority:"), 0, row);
        grid.add(priorityCombo, 1, row++);

        grid.add(new Label("Assigned to:"), 0, row);
        grid.add(assignedUserCombo, 1, row++);

        grid.add(new Label("Due Date:"), 0, row);
        grid.add(dueDatePicker, 1, row++);

        grid.add(followersLabel, 0, row);
        grid.add(followersListView, 1, row);
    }
}
