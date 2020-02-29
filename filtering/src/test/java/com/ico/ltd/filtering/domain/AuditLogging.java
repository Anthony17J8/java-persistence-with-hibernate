package com.ico.ltd.filtering.domain;

import com.ico.ltd.filtering.config.PersistenceConfig;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class AuditLogging {

    @Autowired
    EntityManagerFactory emf;

    AuditLogInterceptor interceptor = new AuditLogInterceptor();

    @Test
    void writeAuditLog() {

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        Long CURRENT_USER_ID;
        try {
            {
                tx.begin();

                User currentUser = new User("johndoe");
                em.persist(currentUser);
                CURRENT_USER_ID = currentUser.getId();

                tx.commit();
                em.close();
            }

            // enable this interceptor with a Hibernate property
            em = emf.createEntityManager();
            Session session = em.unwrap(Session.class).getSessionFactory().withOptions().interceptor(interceptor).openSession();

            interceptor.setCurrentUserId(CURRENT_USER_ID);
            interceptor.setCurrentSession(session);
            tx = session.getTransaction();

            // save new Item
            tx.begin();
            Item item = new Item("Foo");
            session.persist(item);
            tx.commit();
            session.clear();

            // check Audit log
            tx = em.getTransaction();
            tx.begin();

            List<AuditLogRecord> logs = em.createQuery(
                    "select lr from AuditLogRecord lr", AuditLogRecord.class)
                    .getResultList();

            assertEquals(1, logs.size());
            assertEquals("insert", logs.get(0).getMessage());
            assertEquals(Item.class, logs.get(0).getEntityClass());
            assertEquals(item.getId(), logs.get(0).getEntityId());
            assertEquals(CURRENT_USER_ID, logs.get(0).getUserId());
            em.createQuery("delete from AuditLogRecord").executeUpdate();
            tx.commit();
            em.clear();

            // update Item
            tx = session.getTransaction();
            tx.begin();

            item = session.find(Item.class, item.getId());
            item.setName("Bar");
            tx.commit();
            session.clear();

            // check Audit log
            tx = em.getTransaction();
            tx.begin();
            logs = em.createQuery(
                    "select lr from AuditLogRecord lr", AuditLogRecord.class)
                    .getResultList();

            assertEquals(1, logs.size());
            assertEquals("update", logs.get(0).getMessage());
            assertEquals(Item.class, logs.get(0).getEntityClass());
            assertEquals(item.getId(), logs.get(0).getEntityId());
            assertEquals(CURRENT_USER_ID, logs.get(0).getUserId());
            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}