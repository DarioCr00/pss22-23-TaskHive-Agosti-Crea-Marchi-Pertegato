/*
 * This source file was generated by the Gradle 'init' task
 */
package it.unibo.taskhiveTest;

import org.junit.jupiter.api.Test;

import it.unibo.taskhive.App;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @Test
    void appHasAGreeting() {
        App classUnderTest = new App();
        assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
    }
}
