package it.unibo.taskhive.database;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        // Crea il registry
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure("hibernate.cfg.xml") // legge hibernate.cfg.xml
                .build();

        try {
            return new MetadataSources(registry)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (Exception e) {
            System.err.println("Errore durante la creazione della SessionFactory: " + e);
            StandardServiceRegistryBuilder.destroy(registry);
            throw new ExceptionInInitializerError("Impossibile creare SessionFactory");
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }
}