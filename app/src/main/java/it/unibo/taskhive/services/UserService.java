package it.unibo.taskhive.services;

import it.unibo.taskhive.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserService {

    private final List<User> users = new ArrayList<>();
    private User currentUser;

    public UserService() {
        loadSampleUsers();
    }

    private void loadSampleUsers() {
        if (!users.isEmpty()) {
            return;
        }

        users.addAll(List.of(
            new User(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Alice Johnson", "alice@example.com"),
            new User(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Bob Smith", "bob@example.com"),
            new User(UUID.fromString("00000000-0000-0000-0000-000000000003"), "Charlie Brown", "charlie@example.com"),
            new User(UUID.fromString("00000000-0000-0000-0000-000000000004"), "Diana Prince", "diana@example.com"),
            new User(UUID.fromString("00000000-0000-0000-0000-000000000005"), "Edward Norton", "edward@example.com")
        ));

        currentUser = users.get(0);
    }

    public List<User> getAllUsers() {
        return Collections.unmodifiableList(users);
    }

    public Optional<User> findById(UUID id) {
        return users.stream()
            .filter(user -> user.getId().equals(id))
            .findFirst();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UUID userId) {
        findById(userId).ifPresent(user -> currentUser = user);
    }

    public List<User> resolveUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<User> resolved = new ArrayList<>();
        for (UUID id : userIds) {
            findById(id).ifPresent(resolved::add);
        }
        return resolved;
    }
}
