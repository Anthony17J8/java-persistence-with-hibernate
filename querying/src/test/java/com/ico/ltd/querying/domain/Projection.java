package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Projection extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        try {
            tx.begin();

            {
                // select i, b from Item i, Bid b
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<Item> i = criteria.from(Item.class);
                Root<Bid> b = criteria.from(Bid.class);
                criteria.select(cb.tuple(i, b));

                /* Convenient alternative:
                criteria.multiselect(
                    criteria.from(Item.class),
                    criteria.from(Bid.class)
                );
                */
                Query query = em.createQuery(criteria);
                List<Object[]> result = query.getResultList();
                assertThat(result, hasSize(12)); // Cartesian product!

                Set<Item> items = new HashSet<>();
                Set<Bid> bids = new HashSet<>();
                for (Object[] row : result) {
                    assertTrue(row[0] instanceof Item);
                    items.add((Item) row[0]);

                    assertTrue(row[1] instanceof Bid);
                    bids.add((Bid) row[1]);
                }
                assertThat(items, hasSize(3));
                assertThat(bids, hasSize(4));
            }
            em.clear();
            {
                // Tuple API
                CriteriaQuery<Tuple> criteria = cb.createTupleQuery();

                // Or: CriteriaQuery<Tuple> criteria = cb.createQuery(Tuple.class);

                criteria.multiselect(
                        criteria.from(Item.class).alias("i"), // Aliases optional!
                        criteria.from(Bid.class).alias("b")
                );

                TypedQuery<Tuple> query = em.createQuery(criteria);
                List<Tuple> result = query.getResultList();

                Set<Item> items = new HashSet();
                Set<Bid> bids = new HashSet();

                for (Tuple tuple : result) {
                    // Indexed
                    Item item = tuple.get(0, Item.class);
                    Bid bid = tuple.get(1, Bid.class);

                    // Alias
                    item = tuple.get("i", Item.class);
                    bid = tuple.get("b", Bid.class);

                    // Meta
                    for (TupleElement<?> element : tuple.getElements()) {
                        Class clazz = element.getJavaType();
                        String alias = element.getAlias();
                        Object value = tuple.get(element);
                    }
                    items.add(item);
                    bids.add(bid);
                }
                assertThat(result, hasSize(12)); // Cartesian product!
                assertThat(items, hasSize(3));
                assertThat(bids, hasSize(4));

            }
            em.clear();
            {
                // Transient result
                // select u.id, u.username, u.homeAddress from User u
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<User> u = criteria.from(User.class);
                criteria.multiselect( // Returns List of Object[]
                        u.get("id"), u.get("username"), u.get("homeAddress")
                );

                Query query = em.createQuery(criteria);
                List<Object[]> result = query.getResultList();
                assertThat(result, hasSize(3));

                Object[] firstRow = result.get(0);
                assertTrue(firstRow[0] instanceof Long);
                assertTrue(firstRow[1] instanceof String);
                assertTrue(firstRow[2] instanceof Address);
            }
            em.clear();
            {
                // select new com.ico.ltd.domain.querying.ItemSummary(i.id, i.name, i.auctionEnd) from Item i
                CriteriaQuery<ItemSummary> criteria = cb.createQuery(ItemSummary.class);
                Root<Item> i = criteria.from(Item.class);
                criteria.select(cb.construct(
                        ItemSummary.class, // Must have the right constructor!
                        i.get("id"), i.get("name"), i.get("auctionEnd")
                ));

                TypedQuery<ItemSummary> query = em.createQuery(criteria);
                List<ItemSummary> result = query.getResultList();
                assertThat(result, hasSize(3));
            }
            em.clear();
            {
                // select distinct i.name from Item i
                CriteriaQuery<String> criteria = cb.createQuery(String.class);
                Root<Item> i = criteria.from(Item.class);
                criteria.select(i.get("name"));
                criteria.distinct(true);

                TypedQuery<String> query = em.createQuery(criteria);
                List<String> names = query.getResultList();
                assertThat(names, hasSize(3));
            }
            em.clear();
            {
                //select concat(concat(i.name, ': '), i.auctionEnd) from Item i
                Item item = em.find(Item.class, testData.items.getFirstId());

                CriteriaQuery<String> criteria = cb.createQuery(String.class);
                Root<Item> i = criteria.from(Item.class);
                criteria.select(
                        cb.concat(
                                cb.concat(i.get("name"), ": "),
                                i.get("auctionEnd")
                        )
                );

                TypedQuery<String> query = em.createQuery(criteria);
                List<String> result = query.getResultList();
                assertThat(result, hasSize(3));
                assertEquals(String.format("%s: %s", item.getName(), item.getAuctionEnd().toString()), result.get(0));
            }
            em.clear();
            {
                // select i.name, coalesce(i.buyNowPrice, 0) from Item i
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<Item> i = criteria.from(Item.class);
                criteria.multiselect(
                        i.get("name"),
                        cb.coalesce(i.get("buyNowPrice"), 0)
                );

                Query query = em.createQuery(criteria);
                List<Object[]> result = query.getResultList();
                assertThat(result, hasSize(3));
                for (Object[] row : result) {
                    assertTrue(row[0] instanceof String);
                    assertTrue(row[1] instanceof BigDecimal); // Never NULL!
                }
            }
            em.clear();
            {
                // select u.username,
                //      case when length(u.homeAddress.zipcode) = 5 then 'Germany'
                //           when length(u.homeAddress.zipcode) = 4 then 'Switzerland'
                //      else 'Other'
                //      end
                // from User u
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<User> u = criteria.from(User.class);
                criteria.multiselect(
                        u.get("username"),
                        cb.selectCase()
                                .when(cb.equal(
                                        cb.length(u.get("homeAddress").get("zipcode")), 5
                                ), "Germay")
                                .when(cb.equal(
                                        cb.length(u.get("homeAddress").get("zipcode")), 4
                                ), "Switzerland")
                                .otherwise("Other")
                );

                Query query = em.createQuery(criteria);
                List<Object[]> result = query.getResultList();
                assertThat(result, hasSize(3));
                for (Object[] row : result) {
                    assertTrue(row[0] instanceof String);
                    assertTrue(row[1] instanceof String);
                }
            }
            em.clear();
            {
                // select count(i) from Item i
                CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
                criteria.select(
                        cb.count(criteria.from(Item.class))
                );

                TypedQuery<Long> query = em.createQuery(criteria);
                Long count = query.getSingleResult();
                assertEquals(3, count);
            }
            em.clear();
            {
                // select count(distinct i.name) from Item i
                CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
                criteria.select(
                        cb.countDistinct(criteria.from(Item.class).get("name"))
                );

                TypedQuery<Long> query = em.createQuery(criteria);
                Long count = query.getSingleResult();
                assertEquals(3, count);
            }
            em.clear();
            {
                // select sum(b.amount) from Bid b
                CriteriaQuery<Number> criteria = cb.createQuery(Number.class);
                criteria.select(
                        cb.sum(criteria.from(Bid.class).get("amount"))
                );

                TypedQuery<Number> query = em.createQuery(criteria);
                BigDecimal sum = (BigDecimal) query.getSingleResult();
                assertEquals(0, sum.compareTo(new BigDecimal("304.99")));
            }
            em.clear();
            {
                // select min(b.amount), max(b.amount) from Bid b where b.item.id = :itemId
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<Bid> b = criteria.from(Bid.class);
                criteria.multiselect(
                        cb.min(b.get("amount")),
                        cb.max(b.get("amount"))
                );

                criteria.where(cb.equal(
                        b.get("item").get("id"),
                        cb.parameter(Long.class, "itemId")
                ));

                Query query = em.createQuery(criteria);
                query.setParameter("itemId", testData.items.getFirstId());
                Object[] result = (Object[]) query.getSingleResult();
                assertEquals(((BigDecimal) result[0]).compareTo(new BigDecimal("99")), 0);
                assertEquals(((BigDecimal) result[1]).compareTo(new BigDecimal("101")), 0);
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }

    }
}
