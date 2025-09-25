package it.unibo.taskhive.helpers;

import it.unibo.taskhive.models.Project;
import it.unibo.taskhive.models.User;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectDialogHelper {

    private final List<User> availableUsers;

    public ProjectDialogHelper(List<User> availableUsers) {
        this.availableUsers = availableUsers;
    }

    public Optional<Project> showCreateDialog() {
        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("Add New Project");
        dialog.setHeaderText("Create a new project");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = buildProjectForm();

        TextField nameField = new TextField();
        nameField.setPromptText("Project name");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Project description");
        descriptionArea.setPrefRowCount(3);

        ComboBox<User> ownerCombo = new ComboBox<>();
        ownerCombo.setItems(FXCollections.observableArrayList(availableUsers));
        if (!availableUsers.isEmpty()) {
            ownerCombo.setValue(availableUsers.get(0));
        }
        ownerCombo.setPromptText("Select project owner");

        ListView<User> membersListView = new ListView<>();
        membersListView.setItems(FXCollections.observableArrayList(availableUsers));
        membersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        membersListView.setPrefHeight(120);

        addFormRows(grid, nameField, descriptionArea, ownerCombo, membersListView);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                User selectedOwner = ownerCombo.getValue();
                List<User> selectedMembers = new ArrayList<>(membersListView.getSelectionModel().getSelectedItems());
                List<Long> memberIds = selectedMembers.stream().map(User::getId).toList();

                if (selectedOwner != null && !memberIds.contains(selectedOwner.getId())) {
                    memberIds = new ArrayList<>(memberIds);
                    memberIds.add(selectedOwner.getId());
                }

                Project project = new Project(nameField.getText(), descriptionArea.getText(), selectedOwner != null ? selectedOwner.getId() : null, memberIds);
                project.setTasks(new ArrayList<>());
                return project;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public Optional<Project> showEditDialog(Project project) {
        if (project == null) {
            return Optional.empty();
        }

        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("Edit Project");
        dialog.setHeaderText("Modify project details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = buildProjectForm();

        TextField nameField = new TextField(project.getName());
        TextArea descriptionArea = new TextArea(project.getDescription());
        descriptionArea.setPrefRowCount(3);

        ComboBox<User> ownerCombo = new ComboBox<>();
        ownerCombo.setItems(FXCollections.observableArrayList(availableUsers));
        ownerCombo.setPromptText("Select project owner");
        if (project.getOwnerUser() != null) {
            availableUsers.stream()
                .filter(user -> user.getId().equals(project.getOwnerUser()))
                .findFirst()
                .ifPresent(ownerCombo::setValue);
        }

        ListView<User> membersListView = new ListView<>();
        membersListView.setItems(FXCollections.observableArrayList(availableUsers));
        membersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        membersListView.setPrefHeight(120);

        if (project.getMembers() != null) {
            for (Long memberId : project.getMembers()) {
                availableUsers.stream()
                    .filter(user -> user.getId().equals(memberId))
                    .findFirst()
                    .ifPresent(user -> membersListView.getSelectionModel().select(user));
            }
        }

        addFormRows(grid, nameField, descriptionArea, ownerCombo, membersListView);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                User selectedOwner = ownerCombo.getValue();
                List<User> selectedMembers = new ArrayList<>(membersListView.getSelectionModel().getSelectedItems());
                List<Long> memberIds = selectedMembers.stream().map(User::getId).toList();

                if (selectedOwner != null && !memberIds.contains(selectedOwner.getId())) {
                    memberIds = new ArrayList<>(memberIds);
                    memberIds.add(selectedOwner.getId());
                }

                project.setName(nameField.getText());
                project.setDescription(descriptionArea.getText());
                project.setOwnerUser(selectedOwner != null ? selectedOwner.getId() : null);
                project.setMembers(memberIds);
                return project;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private GridPane buildProjectForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        return grid;
    }

    private void addFormRows(
        GridPane grid,
        TextField nameField,
        TextArea descriptionArea,
        ComboBox<User> ownerCombo,
        ListView<User> membersListView
    ) {
        Label membersLabel = new Label("Select members (hold Ctrl/Cmd for multiple):");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        grid.add(new Label("Owner:"), 0, 2);
        grid.add(ownerCombo, 1, 2);

        grid.add(membersLabel, 0, 3);
        grid.add(membersListView, 1, 3);
    }
}
