package com.ico.ltd.fetching.domain;

import com.ico.ltd.fetching.FetchTestLoadEventListener;
import com.ico.ltd.fetching.config.PersistenceConfig;
import com.ico.ltd.fetching.util.FetchTestData;
import com.ico.ltd.fetching.util.TestData;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class CartesianProduct {

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    FetchTestLoadEventListener loadEventListener;

    EntityManager em;

    @Test
    void fetchCollections() throws Exception {
        FetchTestData testData = storeTestData();
        loadEventListener.reset();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Long ITEM_ID = testData.items.getFirstId();

            /*
              The Item has three bids and three images. The size of the
              product depends on the size of the collections you’re retrieving: three times three is nine rows total.

              Hibernate immediately removes all duplicates when it marshals the result set into persistent
              instances and collections
             */
            Item item = em.find(Item.class, ITEM_ID);
            // select i.*, b.*, img.*
            //  from ITEM i
            //   left outer join BID b on b.ITEM_ID = i.ID
            //   left outer join IMAGE img on img.ITEM_ID = i.ID
            //  where i.ID = ?

            assertEquals(loadEventListener.getLoadCount(Item.class), 1);
            assertEquals(loadEventListener.getLoadCount(Bid.class), 3);

            em.detach(item);

            assertEquals(item.getImages().size(), 3);
            assertEquals(item.getBids().size(), 3);

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    public FetchTestData storeTestData() throws Exception {
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
        item.getImages().add("foo.jpg");
        item.getImages().add("bar.jpg");
        item.getImages().add("baz.jpg");
        em.persist(item);
        itemIds[0] = item.getId();
        for (int i = 1; i <= 3; i++) {
            Bid bid = new Bid(item, new BigDecimal(9 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        item = new Item("Item Two", new Date(System.currentTimeMillis()), johndoe);
        item.getImages().add("a.jpg");
        item.getImages().add("b.jpg");
        em.persist(item);
        itemIds[1] = item.getId();
        for (int i = 1; i <= 1; i++) {
            Bid bid = new Bid(item, new BigDecimal(2 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        item = new Item("Item Three", new Date(System.currentTimeMillis()), janeroe);
        em.persist(item);
        itemIds[2] = item.getId();

        tx.commit();
        em.close();

        FetchTestData testData = new FetchTestData();
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}