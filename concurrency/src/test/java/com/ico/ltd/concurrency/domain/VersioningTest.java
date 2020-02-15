package com.ico.ltd.concurrency.domain;

import com.ico.ltd.concurrency.config.PersistenceConfig;
import com.ico.ltd.concurrency.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import java.math.BigDecimal;
import java.util.List;
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

    @Test
    void manualVersionChecking() throws Exception {
        ConcurrencyTestData testData = storeCategoriesAndItems();
        Long[] CATEGORIES = testData.categories.identifiers;
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            BigDecimal totalPrice = new BigDecimal(0);

            for (Long catId : CATEGORIES) {
                /*
                   For each <code>Category</code>, query all <code>Item</code> instances with
                   an <code>OPTIMISTIC</code> lock mode. Hibernate now knows it has to
                   check each <code>Item</code> at flush time.
                 */
                List<Item> items = em
                        .createQuery("select i from Item i where i.category.id =:catId", Item.class)
                        .setLockMode(LockModeType.OPTIMISTIC)
                        .setParameter("catId", catId)
                        .getResultList();

                for (Item item : items) {
                    totalPrice = totalPrice.add(item.getBuyNowPrice());
                }

                // Now a concurrent transaction will move an item to another category
                if (catId.equals(testData.categories.getFirstId())) {
                    Executors.newSingleThreadExecutor().submit(() -> {
                        EntityManager sEm = emf.createEntityManager();
                        EntityTransaction sTx = sEm.getTransaction();

                        try {
                            sTx.begin();
                            // Moving the first item from the first category into the last category
                            List<Item> list = sEm.createQuery("select i from Item i where  i.category.id =:catId", Item.class)
                                    .setParameter("catId", testData.categories.getFirstId())
                                    .getResultList();

                            Category lastCat = sEm.getReference(Category.class, testData.categories.getLastId());
                            list.iterator().next().setCategory(lastCat);

                            sTx.commit();
                            sEm.close();

                        } catch (Exception exc) {
                            System.out.println("Move item to another category failed");
                            exc.printStackTrace();
                        }
                    }).get();
                }
            }

           /*
               For each <code>Item</code> loaded earlier with the locking query, Hibernate will
               now execute a <code>SELECT</code> during flushing. It checks if the database
               version of each <code>ITEM</code> row is still the same as when it was loaded
               earlier. If any <code>ITEM</code> row has a different version, or the row doesn't
               exist anymore, an <code>OptimisticLockException</code> will be thrown.
             */
            assertThrows(RollbackException.class, tx::commit);
            em.close();

//            assertEquals(totalPrice.toString(), "108.00");
        } finally {
            tx.rollback();
        }

    }

    private ConcurrencyTestData storeCategoriesAndItems() throws Exception {
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        ConcurrencyTestData testData = new ConcurrencyTestData();
        testData.categories = new TestData(new Long[3]);
        testData.items = new TestData(new Long[5]);
        for (int i = 1; i <= testData.categories.identifiers.length; i++) {
            Category category = new Category();
            category.setName("Category " + i);

            em.persist(category);
            testData.categories.identifiers[i - 1] = category.getId();

            for (int j = 1; j <= testData.categories.identifiers.length; j++) {
                Item item = new Item("Item " + j);
                item.setCategory(category);
                item.setBuyNowPrice(new BigDecimal(10 + j));
                em.persist(item);
                testData.items.identifiers[(i - 1) + (j - 1)] = item.getId(); // 0 1 2 // 1 2 3 //
            }
        }
        tx.commit();
        em.close();
        return testData;
    }

    private static class ConcurrencyTestData {

        private TestData categories;

        private TestData items;
    }

    @Test
    void forceVersionIncrement() throws Exception {
        final TestData testData = storeItemAndBids();
        Long ITEM_ID = testData.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            /*
               The <code>find()</code> method accepts a <code>LockModeType</code>. The
               <code>OPTIMISTIC_FORCE_INCREMENT</code> mode tells Hibernate that the version
               of the retrieved <code>Item</code> should be incremented after loading,
               even if it's never modified in the unit of work.
             */
            Item item = em.find(
                    Item.class,
                    ITEM_ID,
                    LockModeType.OPTIMISTIC_FORCE_INCREMENT
            );

            Bid highestBid = queryHighestBid(em, item);

            // Now a concurrent transaction will place a bid for this item, and
            // succeed because the first commit wins!
            Executors.newSingleThreadExecutor().submit(() -> {
                EntityManager sEm = emf.createEntityManager();
                EntityTransaction sTx = sEm.getTransaction();
                try {
                    sTx.begin();

                    Item item2 = sEm.find(
                            Item.class,
                            ITEM_ID,
                            LockModeType.OPTIMISTIC_FORCE_INCREMENT
                    );

                    Bid highestBid2 = queryHighestBid(sEm, item);

                    try {
                        Bid newBid = new Bid(
                                new BigDecimal("44.44"),
                                item2,
                                highestBid2
                        );
                        sEm.persist(newBid);
                    } catch (InvalidBidException ex) {
                        // Ignore
                    }
                } catch (Exception exc) {
                    sTx.rollback();
                    throw new RuntimeException("Concurrent operation failure: " + exc);
                }

                sTx.commit();
                sEm.close();

            }).get();

            try {
                /*
                   The code persists a new <code>Bid</code> instance; this does not affect
                   any values of the <code>Item</code> instance. A new row will be inserted
                   into the <code>BID</code> table. Hibernate would not detect concurrently
                   made bids at all without a forced version increment of the
                   <code>Item</code>. We also use a checked exception to validate the
                   new bid amount; it must be greater than the currently highest bid.
                */
                Bid newBid = new Bid(
                        new BigDecimal("44.44"),
                        item,
                        highestBid
                );
                em.persist(newBid);
            } catch (InvalidBidException ex) {
                // Bid too low, show a validation error screen...
            }

            /*
               When flushing the persistence context, Hibernate will execute an
               <code>INSERT</code> for the new <code>Bid</code> and force an
               <code>UPDATE</code> of the <code>Item</code> with a version check.
               If someone modified the <code>Item</code> concurrently, or placed a
               <code>Bid</code> concurrently with this procedure, Hibernate throws
               an exception.
             */
            assertThrows(RollbackException.class, tx::commit);
            em.close();
        } finally {
            tx.rollback();
        }
    }

    private Bid queryHighestBid(EntityManager em, Item item) {
        // Can't scroll with cursors in JPA, have to use setMaxResult()
        try {
            return (Bid) em.createQuery(
                    "select b from Bid b" +
                            " where b.item = :itm" +
                            " order by b.amount desc"
            )
                    .setParameter("itm", item)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }

    }

    private TestData storeItemAndBids() throws Exception {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Long[] ids = new Long[1];
        Item item = new Item("Some Item");
        em.persist(item);
        ids[0] = item.getId();
        for (int i = 1; i <= 3; i++) {
            Bid bid = new Bid(new BigDecimal(10 + i), item);
            em.persist(bid);
        }
        tx.commit();
        em.close();
        return new TestData(ids);
    }
}