package it.unibo.taskhiveTest.services;

import it.unibo.taskhive.database.HibernateUtil;
import it.unibo.taskhive.models.User;
import it.unibo.taskhive.services.UserService;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;

    @BeforeAll
    static void setUpClass() {
        assertNotNull(HibernateUtil.getSessionFactory());
    }

    @BeforeEach
    void setUp() {
        userService = new UserService();
        clearDatabase();
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }


    private void clearDatabase() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("DELETE FROM User").executeUpdate();
            tx.commit();
        }
    }

    @Test
    @DisplayName("Registrazione utente: successo")
    void testRegistrazioneUtente_Successo() {
        boolean risultato = userService.register("testuser", "password123");
        assertTrue(risultato, "La registrazione dovrebbe riuscire");

        Optional<User> userOpt = userService.findByUsername("testuser");
        assertTrue(userOpt.isPresent(), "L'utente dovrebbe essere presente nel DB");
        assertEquals("testuser", userOpt.get().getUsername());
        assertNotNull(userOpt.get().getPasswordHash(), "La password hashata non dovrebbe essere null");
        assertEquals(User.Role.USER, userOpt.get().getRole());
    }

    @Test
    @DisplayName("Login con credenziali corrette")
    void testLoginConCredenzialiCorrette() {
        userService.register("loginuser", "password123");
        boolean esito = userService.login("loginuser", "password123");
        assertTrue(esito, "Il login dovrebbe riuscire con credenziali corrette");
    }

    @Test
    @DisplayName("Login con password errata")
    void testLoginConPasswordErrata() {
        userService.register("wrongpassuser", "correctpass");
        boolean esito = userService.login("wrongpassuser", "wrongpass");
        assertFalse(esito, "Il login non dovrebbe riuscire con password sbagliata");
    }

    @Test
    @DisplayName("Login con username inesistente")
    void testLoginConUsernameInesistente() {
        boolean esito = userService.login("nonexistent", "any_password");
        assertFalse(esito, "Il login non dovrebbe riuscire con username inesistente");
    }

    @Test
    @DisplayName("Registrazione fallita: username già esistente")
    void testRegistrazioneDuplicata_Fallita() {
        userService.register("existinguser", "pass123");
        boolean risultato = userService.register("existinguser", "pass456");
        assertFalse(risultato, "Non si dovrebbe poter registrare un username già esistente");
    }

    @Test
    @DisplayName("Trova utente per ID")
    void testFindById() {
        userService.register("findme", "secret");
        Optional<User> byUsername = userService.findByUsername("findme");
        assertTrue(byUsername.isPresent());

        Optional<User> byId = userService.findById(byUsername.get().getId());
        assertTrue(byId.isPresent());
        assertEquals("findme", byId.get().getUsername());
    }
}