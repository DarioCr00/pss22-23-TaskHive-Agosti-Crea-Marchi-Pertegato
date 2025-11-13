package it.unibo.taskhive.controllers;

import it.unibo.taskhive.models.User;
import it.unibo.taskhive.models.UserWrapper;
import it.unibo.taskhive.services.SceneManager;
import it.unibo.taskhive.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AdminViewController {

    @FXML private TableView<UserWrapper> userTable;

    @FXML private TableColumn<UserWrapper, String> usernameColumn;
    @FXML private TableColumn<UserWrapper, String> roleColumn;
    @FXML private TableColumn<UserWrapper, Boolean> adminColumn;

    private final UserService userService = new UserService();
    private final ObservableList<UserWrapper> users = FXCollections.observableArrayList();

    public void initialize() {
        List<User> userList = userService.getAllUsers();

        users.clear();
        for (User u : userList) {
            if (u.getRole() != User.Role.SUPER) {
                users.add(new UserWrapper(u));
            }
        }

        userTable.setItems(users);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        usernameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getUser().getUsername())
        );

        roleColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getUser().getRole().name())
        );

        adminColumn.setCellValueFactory(new PropertyValueFactory<>("adminStatus"));
        adminColumn.setCellFactory(tc -> new CheckBoxTableCell<>());

        userTable.setEditable(true);
        adminColumn.setEditable(true);
    }

    @FXML
    private void saveChanges() {
        for (UserWrapper wrapper : users) {
            userService.updateUser(wrapper.getUser());
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successo");
        alert.setHeaderText(null);
        alert.setContentText("Ruoli aggiornati con successo.");
        alert.showAndWait();
        SceneManager.switchScene("fxml/ProjectView.fxml");
    }
}