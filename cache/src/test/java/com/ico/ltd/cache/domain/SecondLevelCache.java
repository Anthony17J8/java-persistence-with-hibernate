package com.ico.ltd.cache.domain;

import com.ico.ltd.cache.config.PersistenceConfig;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class SecondLevelCache {

    @Autowired
    EntityManagerFactory emf;

    @Test
    void testStoreData() throws Exception {
        storeTestData();
    }

    public static class CacheTestData {
        public TestData items;
        public TestData users;
    }

    public CacheTestData storeTestData() throws Exception {
        EntityManager em = emf.createEntityManager();
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

        Item item = new Item("Item One", Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)), johndoe);
        em.persist(item);
        itemIds[0] = item.getId();
        for (int i = 1; i <= 3; i++) {
            Bid bid = new Bid(item, robertdoe, new BigDecimal(9 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        item = new Item("Item Two", Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)), johndoe);
        em.persist(item);
        itemIds[1] = item.getId();
        for (int i = 1; i <= 1; i++) {
            Bid bid = new Bid(item, janeroe, new BigDecimal(2 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        item = new Item("Item_Three", Date.from(LocalDateTime.now().plusDays(2).toInstant(ZoneOffset.UTC)), janeroe);
        em.persist(item);
        itemIds[2] = item.getId();
        for (int i = 1; i <= 1; i++) {
            Bid bid = new Bid(item, johndoe, new BigDecimal(3 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        tx.commit();
        em.close();

        // Warm up the cache properly
        tx.begin();
        em = emf.createEntityManager();

        // We load all the User instances into the second-level cache
        // here because NONSTRICT_READ_WRITE strategy doesn't insert
        // them into the second-level cache when persist() is called,
        // only when they are loaded from the database. And we need
        // to do this in a new transaction, or TwoPhaseLoad doesn't
        // put it in the cache.
        em.createQuery("select u from User u").getResultList();

        // The Item#bids are only put in the cache when the
        // collection is loaded.
        for (Long itemId : itemIds) {
            em.find(Item.class, itemId).getBids().size();
        }
        tx.commit();
        em.close();

        emf.unwrap(SessionFactory.class).getStatistics().clear();

        CacheTestData testData = new CacheTestData();
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}