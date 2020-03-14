package com.ico.ltd.querying.domain;

import org.hibernate.Session;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.ToListResultTransformer;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransformResults extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        try {
            tx.begin();

            Session session = em.unwrap(Session.class);
            org.hibernate.query.Query query = session.createQuery(
                    "SELECT i.id as itemId, i.name as name, i.auctionEnd as auctionEnd FROM Item i"
            );

            {
                // Access List of Object[]
                List<Object[]> result = query.list();

                for (Object[] tuple : result) {
                    Long itemId = (Long) tuple[0];
                    String name = (String) tuple[1];
                    Date auctionEnd = (Date) tuple[2];
                    // ...
                }
                assertThat(result, hasSize(3));
            }
            em.clear();
            {
                // Transform to List of Lists
                query.setResultTransformer(
                        ToListResultTransformer.INSTANCE
                );

                List<List> result = query.list();
                for (List list : result) {
                    Long itemId = (Long) list.get(0);
                    String name = (String) list.get(1);
                    Date auctionEnd = (Date) list.get(2);
                    // ...
                }
                assertThat(result, hasSize(3));
            }
            em.clear();
            {
                // Transform to List of Maps
                query.setResultTransformer(
                        AliasToEntityMapResultTransformer.INSTANCE
                );

                List<Map> result = query.list();

                // You can access the aliases of the query
                assertThat(
                        query.getReturnAliases(),
                        arrayContaining("itemId", "name", "auctionEnd")
                );
                for (Map map : result) {
                    Long itemId = (Long) map.get("itemId");
                    String name = (String) map.get("name");
                    Date auctionEnd = (Date) map.get("auctionEnd");
                    // ...
                }
                assertThat(result, hasSize(3));
            }
            em.clear();
            {
                // Transform to List of Maps with entity aliases
                org.hibernate.query.Query entityQuery = session.createQuery(
                        "select i as item, u as seller from Item i join i.seller u"
                );

                entityQuery.setResultTransformer(
                        AliasToEntityMapResultTransformer.INSTANCE
                );

                List<Map> result = entityQuery.list();

                for (Map map : result) {
                    Item item = (Item) map.get("item");
                    User seller = (User) map.get("seller");

                    assertEquals(seller, item.getSeller());
                }
                assertThat(result, hasSize(3));
            }

            { // Custom ResultTransformer
                query.setResultTransformer(
                        new ResultTransformer() {

                            /**
                             * For each result "row", an <code>Object[]</code> tuple has to be transformed into
                             * the desired result value for that row. Here you access each projection element by
                             * index in the tuple array, and then call the <code>ItemSummaryFactory</code> to produce
                             * the query result value. Hibernate passes the method the aliases found in the query, for each
                             * tuple element. You don't need the aliases in this transformer, though.
                             */
                            @Override
                            public Object transformTuple(Object[] tuple, String[] aliases) {

                                Long itemId = (Long) tuple[0];
                                String name = (String) tuple[1];
                                Date auctionEnd = (Date) tuple[2];

                                // You can access the aliases of the query if needed
                                assertEquals(aliases[0], "itemId");
                                assertEquals(aliases[1], "name");
                                assertEquals(aliases[2], "auctionEnd");

                                return ItemSummaryFactory.newItemSummary(
                                        itemId, name, auctionEnd
                                );
                            }

                            /**
                             * You can wrap or modify the result list after after transforming the tuples.
                             * Here you make the returned <code>List</code> unmodifiable,
                             * ideal for a reporting screen where nothing should change the data.
                             */
                            @Override
                            public List transformList(List collection) {
                                // The "collection" is a List<ItemSummary>
                                return Collections.unmodifiableList(collection);
                            }
                        }
                );

                List<ItemSummary> result = query.list();
                assertThat(result, hasSize(3));
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
