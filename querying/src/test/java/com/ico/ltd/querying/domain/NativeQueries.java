package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
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

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
