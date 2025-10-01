package it.unibo.taskhive.services;

import it.unibo.taskhive.database.HibernateUtil;
import it.unibo.taskhive.models.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

/**
 * Servizio per la gestione degli utenti con persistenza su database.
 * Sostituisce completamente LocalUserManager.
 */
public class UserService {

    private User currentUser = null;

    /**
     * Registra un nuovo utente nel database.
     *
     * @param username lo username scelto
     * @param password la password in chiaro
     * @return true se registrazione riuscita, false se username già esistente
     */
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

    /**
     * Esegue il login verificando username e password.
     *
     * @param username lo username
     * @param password la password in chiaro
     * @return true se le credenziali sono corrette
     */
    public boolean login(String username, String password) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (BCrypt.checkpw(password, user.getPasswordHash())) {
                this.currentUser = user;
                return true;
            }
        }
        return false;
    }

    /**
     * Restituisce tutti gli utenti presenti nel database.
     *
     * @return lista di utenti
     */
    public List<User> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class)
                    .getResultList();
        }
    }

    /**
     * Cerca un utente per ID.
     *
     * @param id l'ID dell'utente
     * @return Optional<User> se trovato
     */
    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.get(User.class, id);
            return user != null ? Optional.of(user) : Optional.empty();
        }
    }

    /**
     * Cerca un utente per username.
     *
     * @param username lo username
     * @return Optional<User> se trovato
     */
    public Optional<User> findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResultOptional();
        }
    }

    /**
     * Restituisce l'utente attualmente loggato.
     *
     * @return User o null se nessuno è loggato
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Risolve una lista di ID in oggetti User.
     *
     * @param userIds lista di ID
     * @return lista di User trovati
     */
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

    // --- Metodi aggiuntivi utili ---

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.Role.ADMIN;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public void logout() {
        this.currentUser = null;
    }
}