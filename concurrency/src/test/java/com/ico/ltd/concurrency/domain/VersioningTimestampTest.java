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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class VersioningTimestampTest {

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
            Item someItem = new Item();
            someItem.setName("Some Name");

            em.persist(someItem);
            tx.commit();
            em.close();

            Long itemId = someItem.getId();

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item item = em.find(Item.class, someItem.getId());
            item.setName("New Name");
            em.persist(item);

            Executors.newSingleThreadExecutor().submit(() -> {
                EntityManager sEm = emf.createEntityManager();
                EntityTransaction sTx = sEm.getTransaction();
                try {
                    sTx.begin();
                    Item sItem = sEm.find(Item.class, itemId);
                    sItem.setName("Result Name");

                    sEm.persist(sItem);
                    sTx.commit();
                    sEm.close();

                } catch (Exception exc) {
                    System.out.println("Error update Item name");
                    exc.printStackTrace();
                    sTx.rollback();
                }
            }).get();

            assertThrows(OptimisticLockException.class, () -> em.flush());

        } finally {
            tx.rollback();
        }
    }
}