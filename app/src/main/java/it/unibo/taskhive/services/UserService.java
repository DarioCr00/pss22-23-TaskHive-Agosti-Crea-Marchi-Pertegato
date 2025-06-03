package it.unibo.taskhive.services;

import it.unibo.taskhive.models.User;
import it.unibo.taskhive.models.User.Role;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {

    private final LocalUserManager userManager = new LocalUserManager();

    public boolean register(String username, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(username, hashedPassword, Role.USER);
        return userManager.save(user);
    }

    public boolean login(String username, String password) {
        return userManager.authenticate(username, password);
    }
}