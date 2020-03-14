package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Joins extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        try {
            tx.begin();

            {
                // implicit inner join
                // select b from Bid b where b.item.name like 'Fo%'
                CriteriaQuery<Bid> criteria = cb.createQuery(Bid.class);
                Root<Bid> b = criteria.from(Bid.class);
                criteria.select(b).where(
                        cb.like(
                                b.get("item").get("name"),
                                "Fo%"
                        )
                );

                TypedQuery<Bid> query = em.createQuery(criteria);
                List<Bid> bids = query.getResultList();
                assertThat(bids, hasSize(3));
                bids.forEach(bid ->
                        assertEquals(testData.items.getFirstId(), bid.getItem().getId())
                );
            }
            em.clear();
            {
                // Multiple inner
                // select b from Bid b where b.item.seller.username = 'johndoe'
                CriteriaQuery<Bid> criteria = cb.createQuery(Bid.class);
                Root<Bid> b = criteria.from(Bid.class);
                criteria.select(b).where(
                        cb.equal(
                                b.get("item").get("seller").get("username"),
                                "johndoe"
                        )
                );

                TypedQuery<Bid> query = em.createQuery(criteria);
                List<Bid> bids = query.getResultList();
                assertThat(bids, hasSize(4));
            }
            em.clear();
            {
                // explicit inner
                // select i from Item i join i.bids b where b.amount > 100
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                Join<Item, Bid> b = i.join("bids");
                criteria.select(i).where(
                        cb.gt(
                                b.get("amount"),
                                new BigDecimal(100)
                        )
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> items = query.getResultList();
                assertThat(items, hasSize(1));
                assertEquals(testData.items.getFirstId(), items.get(0).getId());
            }
            em.clear();
            {
                // Explicit outer
                // select i, b from Item i left join i.bids b on b.amount > 100
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<Item> i = criteria.from(Item.class);
                Join<Item, Bid> b = i.join("bids", JoinType.LEFT);
                b.on(
                        cb.gt(b.get("amount"), new BigDecimal(100))
                );

                criteria.multiselect(i, b);

                Query query = em.createQuery(criteria);
                List<Object[]> result = query.getResultList();
                assertThat(result, hasSize(3));
                assertTrue(result.get(0)[0] instanceof Item);
                assertTrue(result.get(0)[1] instanceof Bid);
                assertTrue(result.get(1)[0] instanceof Item);
                assertEquals(null, result.get(1)[1]);
                assertTrue(result.get(2)[0] instanceof Item);
                assertEquals(null, result.get(2)[1]);
            }
            em.clear();
            {
                { // Explicit right outer
                /* TODO Right outer joins not supported in criteria, Hibernate bug JPA-2

                CriteriaQuery criteria = cb.createQuery();
                Root<Bid> b = criteria.from(Bid.class);
                Join<Bid, Item> i = b.join("item", JoinType.RIGHT);
                criteria.multiselect(b, i).where(
                   cb.or(
                      cb.isNull(b),
                      cb.gt(b.<BigDecimal>get("amount"), new BigDecimal(100)))
                );

                Query q = em.createQuery(criteria);
                List<Object[]> result = q.getResultList();
                assertEquals(result.size(), 2);
                assertTrue(result.get(0)[0] instanceof Bid);
                assertTrue(result.get(0)[1] instanceof Item);
                assertEquals(result.get(1)[0], null);
                assertTrue(result.get(1)[1] instanceof Item);
                */
                }
            }
            em.clear();
            {
                // select i from Item i left join fetch i.bids
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                i.fetch("bids", JoinType.LEFT);
                criteria.select(i);

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> result = query.getResultList();
                assertThat(result, hasSize(5)); // 3 items, 4 bids, 5 'rows' in result

                Set<Item> distinctResult = new LinkedHashSet<>(result); // in-memory 'distinct'
                assertThat(distinctResult, hasSize(3));

                boolean haveBids = false;
                for (Item item : distinctResult) {
                    em.detach(item); // No more lazy loading!
                    if (item.getBids().size() > 0) {
                        haveBids = true;
                        break;
                    }
                }
                assertTrue(haveBids);
            }
            em.clear();
            {
                // select distinct i from Item i left join fetch i.bids
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                i.fetch("bids", JoinType.LEFT);

                // DISTINCT operation does not execute in the database.
                // Hibernate performs deduplication in memory
                criteria.select(i).distinct(true);

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> result = query.getResultList();
                assertThat(result, hasSize(3));

                boolean haveBids = false;
                for (Item item : result) {
                    em.detach(item); // No more lazy loading!
                    if (item.getBids().size() > 0) {
                        haveBids = true;
                        break;
                    }
                }
                assertTrue(haveBids);
            }
            em.clear();
            {
                // select distinct i from Item i
                // left join fetch i.bids b
                // join fetch b.bidder
                // left join fetch i.seller
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                Fetch<Item, Bid> b = i.fetch("bids", JoinType.LEFT);
                b.fetch("bidder"); // These are non-nullable foreign key columns, inner join or
                i.fetch("seller", JoinType.LEFT); // outer doesn't make a difference!
                criteria.select(i).distinct(true);

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> result = query.getResultList();
                assertThat(result, hasSize(2));

                boolean haveBids = false;
                boolean haveBidder = false;
                boolean haveSeller = false;
                for (Item item : result) {
                    em.detach(item); // No more lazy loading!
                    if (item.getBids().size() > 0) {
                        haveBids = true;
                        Bid bid = item.getBids().iterator().next();
                        if (bid.getBidder() != null && bid.getBidder().getUsername() != null) {
                            haveBidder = true;
                        }
                    }
                    if (item.getSeller() != null && item.getSeller().getUsername() != null)
                        haveSeller = true;
                }
                assertTrue(haveBids);
                assertTrue(haveBidder);
                assertTrue(haveSeller);
            }
            em.clear();
            {   // SQL Cartesian product of multiple collections! Bad!
                // select distinct i from Item i
                // left join fetch i.bids
                // left join fetch i.images
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                i.fetch("bids", JoinType.LEFT);
                i.fetch("images", JoinType.LEFT); // Cartesian product, bad!
                criteria.select(i).distinct(true);

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> result = query.getResultList();
                assertThat(result, hasSize(3));

                boolean haveBids = false;
                boolean haveImages = false;
                for (Item item : result) {
                    em.detach(item); // No more lazy loading!
                    if (item.getBids().size() > 0) {
                        haveBids = true;
                    }
                    if (item.getImages().size() > 0) {
                        haveImages = true;
                    }
                }
                assertTrue(haveBids);
                assertTrue(haveImages);
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }

    }
}
