package com.ico.ltd.fetching.domain;

import com.ico.ltd.fetching.FetchTestLoadEventListener;
import com.ico.ltd.fetching.config.PersistenceConfig;
import com.ico.ltd.fetching.util.FetchTestData;
import com.ico.ltd.fetching.util.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class NPlusOneSelects {

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    FetchTestLoadEventListener loadEventListener;

    EntityManager em;

    @Test
    @DirtiesContext
    void fetchUsers() throws Exception {
        storeTestData();
        loadEventListener.reset();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            List<Item> items = em.createQuery("SELECT i from Item i", Item.class).getResultList();
            // select * from ITEM
            assertEquals(3, loadEventListener.getLoadCount(Item.class));
            assertEquals(0, loadEventListener.getLoadCount(User.class));

            for (Item item : items) {
                // Each seller has to be loaded with an additional SELECT
                assertNotNull(item.getSeller().getUsername());
                // select * from USERS where ID = ?
            }

            assertEquals(2, loadEventListener.getLoadCount(User.class));

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void fetchBids() throws Exception {
        storeTestData();
        loadEventListener.reset();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            List<Item> items = em.createQuery("SELECT i from Item i", Item.class).getResultList();
            // select * from ITEM
            assertEquals(3, loadEventListener.getLoadCount(Item.class));
            assertEquals(0, loadEventListener.getLoadCount(User.class));

            for (Item item : items) {
                // Each bids collection has to be loaded with an additional SELECT
                assertTrue(item.getBids().size() > 0);
                // select * from BID where ITEM_ID = ?
            }
            assertEquals(loadEventListener.getLoadCount(Bid.class), 5);

            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    private FetchTestData storeTestData() throws Exception {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        Long[] itemIds = new Long[3];
        Long[] userIds = new Long[3];
        try {
            tx.begin();
            User johndoe = new User("johndoe");
            em.persist(johndoe);
            userIds[0] = johndoe.getId();

            User janeroe = new User("janeroe");
            em.persist(janeroe);
            userIds[1] = janeroe.getId();

            User robertdoe = new User("robertdoe");
            em.persist(robertdoe);
            userIds[2] = robertdoe.getId();

            Item item = new Item("Item One", new Date(System.currentTimeMillis()), johndoe);
            em.persist(item);
            itemIds[0] = item.getId();
            for (int i = 1; i <= 3; i++) {
                Bid bid = new Bid(item, robertdoe, new BigDecimal(9 + i));
                item.getBids().add(bid);
                em.persist(bid);
            }

            item = new Item("Item Two", new Date(System.currentTimeMillis()), johndoe);
            em.persist(item);
            itemIds[1] = item.getId();
            for (int i = 1; i <= 1; i++) {
                Bid bid = new Bid(item, janeroe, new BigDecimal(2 + i));
                item.getBids().add(bid);
                em.persist(bid);
            }

            item = new Item("Item Three", new Date(System.currentTimeMillis()), janeroe);
            em.persist(item);
            itemIds[2] = item.getId();
            for (int i = 1; i <= 1; i++) {
                Bid bid = new Bid(item, johndoe, new BigDecimal(3 + i));
                item.getBids().add(bid);
                em.persist(bid);
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
        FetchTestData testData = new FetchTestData();
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}