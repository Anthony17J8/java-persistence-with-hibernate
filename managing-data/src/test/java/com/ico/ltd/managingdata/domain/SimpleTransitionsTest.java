package com.ico.ltd.managingdata.domain;

import com.ico.ltd.managingdata.config.PersistenceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnitUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class SimpleTransitionsTest {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    PersistenceUnitUtil puUtil;

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        puUtil = emf.getPersistenceUnitUtil();
    }

    @Test
    void makePersistent() throws Exception {

        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Item item = new Item();
            item.setName("New Name");

            assertNull(puUtil.getIdentifier(item)); // check whether entity is transient

            em.persist(item);

            assertTrue(em.contains(item)); // check whether entity is persistent

            Long itemId = item.getId(); // has been assigned

            tx.commit();
            em.close();

            assertNotNull(puUtil.getIdentifier(item));
            assertEquals(item.getId(), puUtil.getIdentifier(item)); // check whether entity is detached

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();
            Item result = em.find(Item.class, itemId);
            assertEquals("New Name", result.getName());
            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    @Test
    void loadAndUpdateData() throws Exception {
        EntityTransaction tx = em.getTransaction();
        try {
            // persist data
            tx.begin();

            Item saved = new Item();
            saved.setName("Some Name");

            em.persist(saved);
            tx.commit();
            em.close();

            Long itemId = saved.getId();

            // update data
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item result = em.find(Item.class, itemId); // Hit the database if not already in persistence context

            if (result != null) {
                result.setName("New Name"); // update state
            }
            tx.commit(); // Flush: Dirty check and SQL UPDATE
            em.close();

            // check updated state
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item check = em.find(Item.class, itemId);
            assertEquals("New Name", check.getName());

            tx.commit();
            em.close();

            // repeatable read
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item itemA = em.find(Item.class, itemId);
            Item itemB = em.find(Item.class, itemId);
            assertSame(itemA, itemB);
            assertEquals(itemA, itemB);
            assertEquals(itemA.getId(), itemB.getId());

        } finally {
            tx.rollback();
        }

    }
}