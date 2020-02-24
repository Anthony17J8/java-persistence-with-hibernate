package com.ico.ltd.fetching.domain;

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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class EagerQuery {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    @DirtiesContext
    void fetchUsers() throws Exception {
        storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // JPQL
            {
                List<Item> items = em.createQuery("SELECT i FROM Item i JOIN FETCH i.seller", Item.class).getResultList();
                // select i.*, u.*
                //  from ITEM i
                //   inner join USERS u on u.ID = i.SELLER_ID
                //  where i.ID = ?

                em.clear(); // detach all

                for (Item item : items) {
                    assertNotNull(item.getSeller().getUsername());
                }
            }

            // CriteriaQuery API
            {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> root = criteria.from(Item.class);
                root.fetch("seller");
                criteria.select(root);

                List<Item> items = em.createQuery(criteria).getResultList();

                em.clear(); // detach all

                for (Item item : items) {
                    assertNotNull(item.getSeller().getUsername());
                }
            }

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

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // JPQL
            {
                List<Item> items = em.createQuery(
                        "SELECT i FROM Item i LEFT JOIN FETCH i.bids", Item.class)
                        .getResultList();
                // select i.*, b.*
                //  from ITEM i
                //   left outer join BID b on b.ITEM_ID = i.ID
                //  where i.ID = ?

                em.clear(); // detach all

                for (Item item : items) {
                    assertTrue(item.getBids().size() > 0);
                }
            }

            // CriteriaQuery API
            {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> root = criteria.from(Item.class);
                root.fetch("bids", JoinType.LEFT);
                criteria.select(root);

                List<Item> items = em.createQuery(criteria).getResultList();

                em.clear(); // detach all

                for (Item item : items) {
                    assertTrue(item.getBids().size() > 0);
                }
            }

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