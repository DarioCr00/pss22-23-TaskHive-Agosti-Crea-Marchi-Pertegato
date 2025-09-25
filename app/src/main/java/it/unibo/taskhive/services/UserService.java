package it.unibo.taskhive.services;

import it.unibo.taskhive.models.User;
import it.unibo.taskhive.models.User.Role;

import java.util.Random;
import java.util.List;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

public class UserService {

    private final LocalUserManager userManager = new LocalUserManager();

    public boolean register(String username, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(new Random().nextLong(), username, hashedPassword, Role.USER);
        return userManager.save(user);
    }

    public boolean login(String username, String password) {
        return userManager.authenticate(username, password);
    }

    public List<User> getAllUsers() {
        return userManager.getAllUsers();
    }

    public Optional<User> findById(Long id) {
        return userManager.findById(id);
    }

    public User getCurrentUser() {
        return userManager.getCurrentUser();
    }
    
    public List<User> resolveUsers(List<Long> userIds) {
        return userManager.resolveUsers(userIds);
    }
}