package it.unibo.taskhive.services;

import it.unibo.taskhive.database.HibernateUtil;
import it.unibo.taskhive.models.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;


public class UserService {

    private SessionManager sessionManager = SessionManager.getInstance();
  
    public boolean register(String username, String password) {
        if (findByUsername(username).isPresent()) {
            return false;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(username, hashedPassword, User.Role.USER);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(user);
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String username, String password) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (BCrypt.checkpw(password, user.getPasswordHash())) {
                sessionManager.setCurrentUser(user);
                return true;
            }
        }
        return false;
    }


    public List<User> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class)
                    .getResultList();
        }
    }


    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.get(User.class, id);
            return user != null ? Optional.of(user) : Optional.empty();
        }
    }


    public Optional<User> findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResultOptional();
        }
    }

    public List<User> resolveUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
    public void logout() {
        sessionManager.clearSession();
    }
}