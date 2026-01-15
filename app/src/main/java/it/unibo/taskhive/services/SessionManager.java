package it.unibo.taskhive.services;

import it.unibo.taskhive.models.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void clearSession() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.Role.ADMIN;
    }

    public boolean isSuper() {
        return currentUser != null && currentUser.getRole() == User.Role.SUPER;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
}