package it.unibo.taskhive.models;

import javafx.beans.property.SimpleBooleanProperty;

public class UserWrapper {
    private final User user;
    private final SimpleBooleanProperty adminStatus;

    public UserWrapper(User user) {
        this.user = user;
        this.adminStatus = new SimpleBooleanProperty(
            user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER
        );

        this.adminStatus.addListener((obs, oldVal, newVal) -> {
            if (newVal && user.getRole() == User.Role.USER) {
                user.setRole(User.Role.ADMIN);
            } else if (!newVal && user.getRole() == User.Role.ADMIN) {
                user.setRole(User.Role.USER);
            }
        });
    }

    public User getUser() {
        return user;
    }

    public SimpleBooleanProperty adminStatusProperty() {
        return adminStatus;
    }

    public boolean isAdminStatus() {
        return adminStatus.get();
    }

    public void setAdminStatus(boolean admin) {
        this.adminStatus.set(admin);
    }
}