package it.unibo.taskhive.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import it.unibo.taskhive.database.HibernateUtil;
import it.unibo.taskhive.models.Notification;

public class NotificationDAO {

    public void save(Notification n) {
        try(Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(n);
            tx.commit();
        }
    }

    public List<Notification> findByUserId(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "FROM Notification WHERE userId = :uid ORDER BY reminderTime DESC",
                Notification.class
            )
            .setParameter("uid", userId)
            .getResultList();
        }
    }

    public boolean hasUnread(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                "SELECT COUNT(n) FROM Notification n WHERE n.userId = :uid AND n.isRead = false",
                Long.class
            )
            .setParameter("uid", userId)
            .uniqueResult();

            return count != null && count > 0;
        }
    }

    public void markAsRead(int idNotification) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Notification n = session.get(Notification.class, idNotification);
            if (n != null) {
                n.setRead(true);
                session.merge(n);
            }

            tx.commit();
        }
    }
}
