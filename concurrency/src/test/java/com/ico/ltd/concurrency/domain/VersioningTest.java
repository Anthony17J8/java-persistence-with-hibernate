package com.ico.ltd.concurrency.domain;

import com.ico.ltd.concurrency.config.PersistenceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class VersioningTest {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
    }

    @Test
    void firstCommitWins() throws Exception {
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            Item someItem = new Item("Some name");
            em.persist(someItem);

            tx.commit();
            em.close();

            final Long itemId = someItem.getId();

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item item = em.find(Item.class, itemId);

            assertEquals(0, item.getVersion());

            item.setName("New Name");

            Executors.newSingleThreadExecutor().submit(() -> {
                EntityManager em = emf.createEntityManager();
                EntityTransaction sTx = em.getTransaction();
                try {
                    sTx.begin();

                    Item another = em.find(Item.class, itemId);

                    assertEquals(0, another.getVersion());
                    another.setName("Other name");

                    sTx.commit();
                    // update ITEM set NAME = ?, VERSION = 1 where ID = ? and VERSION = 0
                    // This succeeds, there is a row with ID = ? and VERSION = 0 in the database!
                    em.close();
                } catch (Exception exc) {
                    sTx.rollback();
                }
            }).get();

            /*
               When the persistence context is flushed Hibernate will detect the dirty
               <code>Item</code> instance and increment its version to 1. The SQL
               <code>UPDATE</code> now performs the version check, storing the new version
               in the database, but only if the database version is still 0.
             */
            assertThrows(OptimisticLockException.class, () -> em.flush());
            // update ITEM set NAME = ?, VERSION = 1 where ID = ? and VERSION = 0

        } finally {
            tx.rollback();
        }
    }
}