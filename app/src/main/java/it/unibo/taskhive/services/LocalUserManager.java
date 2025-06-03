package it.unibo.taskhive.services;

import it.unibo.taskhive.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LocalUserManager {

    // Mappa username -> utente
    private static final Map<String, User> users = new HashMap<>();

    // Inizializza alcuni utenti predefiniti
    static {
        String adminHash = BCrypt.hashpw("admin123", BCrypt.gensalt(12));
        users.put("admin", new User("admin", adminHash, User.Role.ADMIN));

        String userHash = BCrypt.hashpw("user123", BCrypt.gensalt(12));
        users.put("user", new User("user", userHash, User.Role.USER));
    }

    // Trova un utente per username
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
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
            return BCrypt.checkpw(password, user.getPasswordHash());
        }
        return false;
    }
}