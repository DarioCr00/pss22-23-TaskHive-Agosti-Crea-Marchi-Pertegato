package it.unibo.taskhive.services;

import it.unibo.taskhive.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class LocalUserManager {

    // Mappa username -> utente
    private static final Map<String, User> users = new HashMap<>();
    private User currentUser = null;

    // Inizializza alcuni utenti predefiniti
    static {
        Long adminId = new Random().nextLong();
        String adminHash = BCrypt.hashpw("admin123", BCrypt.gensalt(12));
        users.put("admin", new User(adminId, "admin", adminHash, User.Role.ADMIN));

        Long userId = new Random().nextLong();
        String userHash = BCrypt.hashpw("user123", BCrypt.gensalt(12));
        users.put("user", new User(userId, "user", userHash, User.Role.USER));
    }

    public User getCurrentUser() {
        return currentUser;
    }
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    // Trova un utente per username
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findById(Long id) {
        for (User user : users.values()) {
            if (user.getId().equals(id)) {
                return Optional.ofNullable(user);
            }
        }
        return null;
    }

    public List<User> resolveUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<User> resolved = new ArrayList<>();
        for (Long id : userIds) {
            findById(id).ifPresent(resolved::add);
        }
        return resolved;
    }

    // Salva un nuovo utente
    public boolean save(User user) {
        if (users.containsKey(user.getUsername())) {
            return false; // Username già esistente
        }
        users.put(user.getUsername(), user);
        return true;
    }

    // Verifica credenziali
    public boolean authenticate(String username, String password) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            setCurrentUser(user);
            return BCrypt.checkpw(password, user.getPasswordHash());
        }
        return false;
    }
}