package com.ico.ltd.filtering.domain;

import com.ico.ltd.filtering.config.PersistenceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class JpaListener {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    void notifyPostPersist() {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            User user = new User("johndoe");
            CurrentUser.INSTANCE.set(user); // Thread-local

            em.persist(user);
            assertEquals(Mail.INSTANCE.size(), 0);

            em.flush();
            assertEquals(Mail.INSTANCE.size(), 1);
            assertTrue(Mail.INSTANCE.get(0).contains("johndoe"));
            Mail.INSTANCE.clear();

            Item item = new Item("Foo", user);
            em.persist(item);
            assertEquals(Mail.INSTANCE.size(), 0);
            em.flush();
            assertEquals(Mail.INSTANCE.size(), 1);
            assertTrue(Mail.INSTANCE.get(0).contains("johndoe"));
            Mail.INSTANCE.clear();

            CurrentUser.INSTANCE.set(null);

        } finally {
            tx.rollback();
        }

    }
}