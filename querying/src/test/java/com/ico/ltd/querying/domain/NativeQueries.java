package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NativeQueries extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            {
                // Simple SQL projection
                Query query = em.createNativeQuery(
                        "select NAME, AUCTIONEND from ITEM"
                );
                List<Object[]> result = query.getResultList();

                for (Object[] tuple : result) {
                    assertTrue(tuple[0] instanceof String);
                    assertTrue(tuple[1] instanceof Date);
                }
                assertThat(result, hasSize(3));
            }
            em.clear();
            {
                // Automatic marshaling of resultset to mapped entity class
                Query query = em.createNativeQuery(
                        "select * from ITEM",
                        Item.class
                );

                List<Item> result = query.getResultList();
                assertThat(result, hasSize(3));
                assertNotNull(result.get(0));
            }
            em.clear();
            {
                // Positional parameter binding
                Long ITEM_ID = testData.items.getFirstId();
                Query query = em.createNativeQuery(
                        "select * from ITEM where ID = ?",
                        Item.class
                );
                query.setParameter(1, ITEM_ID); // Starts at one!

                List<Item> result = query.getResultList();
                assertThat(result, hasSize(1));
                assertEquals(ITEM_ID, result.get(0).getId());
            }
            em.clear();
            {
                // Named parameter binding
                Long ITEM_ID = testData.items.getFirstId();
                Query query = em.createNativeQuery(
                        "select * from ITEM where ID = :id",
                        Item.class
                );
                query.setParameter("id", ITEM_ID);

                List<Item> result = query.getResultList();
                assertEquals(result.size(), 1);
                assertEquals(result.get(0).getId(), ITEM_ID);
            }
            em.clear();
            {
                // Automatic marshaling of resultset to entity class with aliases
                Query query = em.createNativeQuery(
                        "select " +
                                "i.ID, " +
                                "'Auction: ' || i.NAME as EXTENDED_NAME, " +
                                "i.CREATEDON, " +
                                "i.AUCTIONEND, " +
                                "i.AUCTIONTYPE, " +
                                "i.APPROVED, " +
                                "i.BUYNOWPRICE, " +
                                "i.SELLER_ID " +
                                "from ITEM i",
                        "ItemResult"
                );

                List<Item> result = query.getResultList();
                assertThat(result, hasSize(3));
                assertNotNull(result.get(0));
            }
            em.clear();
            {
                // Automatic marshaling of resultset to entity class with aliases (externalized)
                Query query = em.createNativeQuery(
                        "select " +
                                "i.ID, " +
                                "'Auction: ' || i.NAME as EXTENDED_NAME, " +
                                "i.CREATEDON, " +
                                "i.AUCTIONEND, " +
                                "i.AUCTIONTYPE, " +
                                "i.APPROVED, " +
                                "i.BUYNOWPRICE, " +
                                "i.SELLER_ID " +
                                "from ITEM i",
                        "ExternalizedItemResult"
                );

                List<Item> result = query.getResultList();
                assertThat(result, hasSize(3));
                assertNotNull(result.get(0));
            }
            em.clear();
            {
                // Automatic marshaling of resultset to several mapped entity classes
                Query query = em.createNativeQuery(
                        "select " +
                                "i.ID as ITEM_ID, " +
                                "i.NAME, " +
                                "i.CREATEDON, " +
                                "i.AUCTIONEND, " +
                                "i.AUCTIONTYPE, " +
                                "i.APPROVED, " +
                                "i.BUYNOWPRICE, " +
                                "i.SELLER_ID, " +
                                "u.ID as USER_ID, " +
                                "u.USERNAME, " +
                                "u.FIRSTNAME, " +
                                "u.LASTNAME, " +
                                "u.ACTIVATED, " +
                                "u.STREET, " +
                                "u.ZIPCODE, " +
                                "u.CITY " +
                                "from ITEM i join USERS u on u.ID = i.SELLER_ID",
                        "ItemSellerResult"
                );
                List<Object[]> result = query.getResultList();

                for (Object[] tuple : result) {
                    assertTrue(tuple[0] instanceof Item);
                    assertTrue(tuple[1] instanceof User);
                    Item item = (Item) tuple[0];
                    assertTrue(Persistence.getPersistenceUtil().isLoaded(item, "seller"));
                    assertEquals(tuple[1], item.getSeller());
                }
                assertThat(result, hasSize(3));
            }
            em.clear();
            {
                // Automatic marshaling of resultset to entity class with component aliases
                Query query = em.createNativeQuery(
                        "select " +
                                "u.ID, " +
                                "u.USERNAME, " +
                                "u.FIRSTNAME, " +
                                "u.LASTNAME, " +
                                "u.ACTIVATED, " +
                                "u.STREET as USER_STREET, " +
                                "u.ZIPCODE as USER_ZIPCODE, " +
                                "u.CITY as USER_CITY " +
                                "from USERS u",
                        "UserResult"
                );

                List<User> result = query.getResultList();
                assertThat(result, hasSize(3));
                assertNotNull(result.get(0));
            }
            em.clear();
            {
                // Automatic marshaling of resultset to data-transfer class constructor
                Query query = em.createNativeQuery(
                        "select ID, NAME, AUCTIONEND from ITEM",
                        "ItemSummaryResult"
                );
                List<ItemSummary> result = query.getResultList();
                assertTrue(result.get(0) instanceof ItemSummary);
                assertThat(result, hasSize(3));
            }
            em.clear();
            {
                // Automatic marshaling of resultset to entity class and additional column
                Query query = em.createNativeQuery(
                        "select " +
                                "i.*, " +
                                "count(b.ID) as NUM_OF_BIDS " +
                                "from ITEM i left join BID b on b.ITEM_ID = i.ID " +
                                "group by i.ID, i.NAME, i.CREATEDON, i.AUCTIONEND, " +
                                "i.AUCTIONTYPE, i.APPROVED, i.BUYNOWPRICE, i.SELLER_ID",
                        "ItemBidResult"
                );

                List<Object[]> result = query.getResultList();

                for (Object[] tuple : result) {
                    assertTrue(tuple[0] instanceof Item);
                    assertTrue(tuple[1] instanceof Number);
                }
                assertThat(result, hasSize(3));
                assertNotNull(result.get(0));
            }
            {
                // Automatic marshaling of resultset to entity class, constructor, additional column
                Query query = em.createNativeQuery(
                        "select " +
                                "u.*, " +
                                "i.ID as ITEM_ID, i.NAME as ITEM_NAME, i.AUCTIONEND as ITEM_AUCTIONEND, " +
                                "count(b.ID) as NUM_OF_BIDS " +
                                "from ITEM i " +
                                "join USERS u on u.ID = i.SELLER_ID " +
                                "left join BID b on b.ITEM_ID = i.ID " +
                                "group by u.ID, u.USERNAME, u.FIRSTNAME, u.LASTNAME, " +
                                "u.ACTIVATED, u.STREET, u.ZIPCODE, u.CITY, " +
                                "ITEM_ID, ITEM_NAME, ITEM_AUCTIONEND",
                        "SellerItemSummaryResult"
                );

                List<Object[]> result = query.getResultList();
                for (Object[] tuple : result) {
                    // Wrong order of results, Hibernate issue HHH-8678!
                    assertTrue(tuple[0] instanceof User);
                    assertTrue(tuple[1] instanceof BigInteger);
                    assertTrue(tuple[2] instanceof ItemSummary);
                }
                assertEquals(result.size(), 3);
            }
            em.clear();
            {
                // Externalized recursive SQL query
                Query query = em.createNamedQuery("findAllCategories");
                List<Object[]> result = query.getResultList();

                for (Object[] tuple : result) {
                    Category category = (Category) tuple[0];
                    String path = (String) tuple[1];
                    Integer level = (Integer) tuple[2];
                }
            }
            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
