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

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;
import javax.persistence.Subgraph;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @see <a href=https://www.logicbig.com/tutorials/java-ee-tutorial/jpa/entity-graph-basic-use.html></a>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class FetchLoadGraph {

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    FetchTestLoadEventListener loadEventListener;

    EntityManager em;

    @Test
    @DirtiesContext
    void loadItem() throws Exception {
        FetchTestData testData = storeTestData();
        Long ITEM_ID = testData.items.getFirstId();
        PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();
        loadEventListener.reset();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            {
                Map<String, Object> properties = new HashMap<>();
                properties.put(
                        "javax.persistence.loadgraph",
                        em.getEntityGraph(Item.class.getSimpleName()) // "Item"
                );

                Item item = em.find(Item.class, ITEM_ID, properties); // default fetching plan
                // select * from ITEM where ID = ?

                assertTrue(persistenceUtil.isLoaded(item));
                assertTrue(persistenceUtil.isLoaded(item, "name"));
                assertTrue(persistenceUtil.isLoaded(item, "auctionEnd"));
                assertFalse(persistenceUtil.isLoaded(item, "seller"));
                assertFalse(persistenceUtil.isLoaded(item, "bids"));
            }

            em.clear();

            {
                EntityGraph<Item> itemGraph = em.createEntityGraph(Item.class);

                Map<String, Object> properties = new HashMap<>();
                properties.put("javax.persistence.loadgraph", itemGraph);

                Item item = em.find(Item.class, ITEM_ID, properties); // default fetching plan
                // select * from ITEM where ID = ?

                assertTrue(persistenceUtil.isLoaded(item));
                assertTrue(persistenceUtil.isLoaded(item, "name"));
                assertTrue(persistenceUtil.isLoaded(item, "auctionEnd"));
                assertFalse(persistenceUtil.isLoaded(item, "seller"));
                assertFalse(persistenceUtil.isLoaded(item, "bids"));
            }
            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void loadItemSeller() throws Exception {
        FetchTestData testData = storeTestData();
        Long ITEM_ID = testData.items.getFirstId();
        PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            {
                Map<String, Object> properties = new HashMap<>();
                properties.put(
                        "javax.persistence.loadgraph",
                        em.getEntityGraph("ItemSeller"));

                Item item = em.find(Item.class, ITEM_ID, properties);
                // select i.*, u.*
                //  from ITEM i
                //   inner join USERS u on u.ID = i.SELLER_ID
                // where i.ID = ?

                assertTrue(persistenceUtil.isLoaded(item));
                assertTrue(persistenceUtil.isLoaded(item, "name"));
                assertTrue(persistenceUtil.isLoaded(item, "auctionEnd"));
                assertTrue(persistenceUtil.isLoaded(item, "seller"));
                assertFalse(persistenceUtil.isLoaded(item, "bids"));
            }
            em.clear();
            {
                EntityGraph<Item> itemGraph = em.createEntityGraph(Item.class);
                itemGraph.addAttributeNodes(Item_.seller); // or "seller"

                Map<String, Object> properties = new HashMap<>();
                properties.put(
                        "javax.persistence.loadgraph",
                        itemGraph);

                Item item = em.find(Item.class, ITEM_ID, properties);
                // select i.*, u.*
                //  from ITEM i
                //   inner join USERS u on u.ID = i.SELLER_ID
                // where i.ID = ?

                assertTrue(persistenceUtil.isLoaded(item));
                assertTrue(persistenceUtil.isLoaded(item, "name"));
                assertTrue(persistenceUtil.isLoaded(item, "auctionEnd"));
                assertTrue(persistenceUtil.isLoaded(item, "seller"));
                assertFalse(persistenceUtil.isLoaded(item, "bids"));
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void loadBidBidderItem() throws Exception {
        FetchTestData testData = storeTestData();
        Long BID_ID = testData.bids.getFirstId();
        PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            {
                Map<String, Object> properties = new HashMap<>();
                properties.put(
                        "javax.persistence.loadgraph",
                        em.getEntityGraph("BidBidderItem"));

                Bid bid = em.find(Bid.class, BID_ID, properties);
                // SELECT * FROM BID
                // inner join USERS
                // inner join Item

                assertTrue(persistenceUtil.isLoaded(bid));
                assertTrue(persistenceUtil.isLoaded(bid, "amount"));
                assertTrue(persistenceUtil.isLoaded(bid, "bidder"));
                assertTrue(persistenceUtil.isLoaded(bid, "item"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "name"));
                assertFalse(persistenceUtil.isLoaded(bid.getItem(), "seller"));
            }
            em.clear();
            {
                EntityGraph<Bid> bidGraph = em.createEntityGraph(Bid.class);
                bidGraph.addAttributeNodes("bidder", "item");

                Map<String, Object> properties = new HashMap<>();
                properties.put(
                        "javax.persistence.loadgraph",
                        bidGraph);

                Bid bid = em.find(Bid.class, BID_ID, properties);

                assertTrue(persistenceUtil.isLoaded(bid));
                assertTrue(persistenceUtil.isLoaded(bid, "amount"));
                assertTrue(persistenceUtil.isLoaded(bid, "bidder"));
                assertTrue(persistenceUtil.isLoaded(bid, "item"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "name"));
                assertFalse(persistenceUtil.isLoaded(bid.getItem(), "seller"));
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void loadBidBidderItemSellerBids() throws Exception {
        FetchTestData testData = storeTestData();
        Long BID_ID = testData.bids.getFirstId();
        PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            {
                Map<String, Object> properties = new HashMap<>();
                properties.put(
                        "javax.persistence.loadgraph",
                        em.getEntityGraph("BidBidderItemSellerBids"));

                Bid bid = em.find(Bid.class, BID_ID, properties);

                assertTrue(persistenceUtil.isLoaded(bid));
                assertTrue(persistenceUtil.isLoaded(bid, "amount"));
                assertTrue(persistenceUtil.isLoaded(bid, "bidder"));
                assertTrue(persistenceUtil.isLoaded(bid, "item"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "name"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "seller"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem().getSeller(), "username"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "bids"));
            }
            em.clear();
            {
                EntityGraph<Bid> bidGraph = em.createEntityGraph(Bid.class);
                bidGraph.addAttributeNodes(Bid_.bidder, Bid_.item);
                Subgraph<Item> itemSubGraph = bidGraph.addSubgraph(Bid_.item);
                itemSubGraph.addAttributeNodes(Item_.seller, Item_.bids);

                Map<String, Object> properties = new HashMap<>();
                properties.put(
                        "javax.persistence.loadgraph",
                        bidGraph);

                Bid bid = em.find(Bid.class, BID_ID, properties);

                assertTrue(persistenceUtil.isLoaded(bid));
                assertTrue(persistenceUtil.isLoaded(bid, "amount"));
                assertTrue(persistenceUtil.isLoaded(bid, "bidder"));
                assertTrue(persistenceUtil.isLoaded(bid, "item"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "name"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "seller"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem().getSeller(), "username"));
                assertTrue(persistenceUtil.isLoaded(bid.getItem(), "bids"));
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
        Long[] bidIds = new Long[3];

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
            bidIds[i - 1] = bid.getId();
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
        testData.bids = new TestData(bidIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}