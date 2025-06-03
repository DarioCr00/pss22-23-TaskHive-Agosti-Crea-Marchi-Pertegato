package it.unibo.taskhiveTest.services;

import it.unibo.taskhive.models.User;
import it.unibo.taskhive.models.User.Role;
import it.unibo.taskhive.services.UserService;
import it.unibo.taskhive.services.LocalUserManager;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    UserService us = new UserService();
    LocalUserManager um = new LocalUserManager();

    @Test
    void testRegistrazioneUtente_Successo() {

        boolean risultato = us.register("testuser", "password123");
        assertTrue(risultato);

        Optional<User> user = um.findByUsername("testuser");
        assertTrue(user.isPresent());
        assertEquals("testuser", user.get().getUsername());
        assertNotNull(user.get().getPasswordHash());
        assertEquals(Role.USER, user.get().getRole());
    }

    @Test
    void testLoginConCredenzialiCorrette() {
        us.register("loginuser", "password123");
        boolean esito = us.login("loginuser", "password123");
        assertTrue(esito);
    }

    @Test
    void testLoginConPasswordErrata() {
        us.register("wrongpassuser", "correctpass");
        boolean esito = us.login("wrongpassuser", "wrongpass");
        assertFalse(esito);
    }

    @Test
    void testLoginConUsernameInesistente() {
        boolean esito = us.login("nonexistent", "any_password");
        assertFalse(esito);
    }
}