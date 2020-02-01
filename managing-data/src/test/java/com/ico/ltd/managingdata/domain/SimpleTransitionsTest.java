package com.ico.ltd.managingdata.domain;

import com.ico.ltd.managingdata.config.PersistenceConfig;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class SimpleTransitionsTest {

    @Autowired
    EntityManagerFactory emf;

    @Test
    void makePersistent() throws Exception {
        EntityManager em = emf.createEntityManager();
        PersistenceUnitUtil puUtil = emf.getPersistenceUnitUtil();

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
}