package com.ico.ltd.fetching.domain;

import com.ico.ltd.fetching.config.PersistenceConfig;
import com.ico.ltd.fetching.util.FetchTestData;
import com.ico.ltd.fetching.util.TestData;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class FetchProfile {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    void fetchWithProfile() throws Exception {
        FetchTestData testData = storeTestData();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            /*
                The <code>Item#seller</code> is mapped lazy, so the default fetch plan
                will only retrieve the <code>Item</code> instance.
             */
            Item item = em.find(Item.class, ITEM_ID);

            assertFalse(Hibernate.isInitialized(item.getSeller()));

            em.clear();

            em.unwrap(Session.class).enableFetchProfile(Item.PROFILE_JOIN_SELLER);
            item = em.find(Item.class, ITEM_ID);
            // SELECT * FROM ITEM
            // JOIN USER

            em.clear(); // detach item

            assertNotNull(item.getSeller().getUsername());

            /*
                You can overlay another profile on the same unit of work, now the <code>Item#seller</code>
                and the <code>Item#bids</code> collection will be fetched with a join in the same SQL
                statement whenever an <code>Item</code> is loaded.
             */
            em.unwrap(Session.class).enableFetchProfile(Item.PROFILE_JOIN_BIDS);
            item = em.find(Item.class, ITEM_ID);
            // SELECT * FROM ITEM
            // JOIN USER
            // LEFT OUTER BIDS

            em.clear();
            assertNotNull(item.getSeller().getUsername());
            assertTrue(item.getBids().size() > 0);

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    private FetchTestData storeTestData() throws Exception {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        tx.begin();

        Long[] itemIds = new Long[3];
        Long[] userIds = new Long[3];

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

        FetchTestData testData = new FetchTestData();
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}