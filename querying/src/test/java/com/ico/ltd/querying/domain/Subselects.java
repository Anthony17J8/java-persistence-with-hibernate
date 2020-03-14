package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Subselects extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        try {
            tx.begin();
            {
                // Correlated
                // select u from User u where (select count(i) from Item i where i.seller = u) > 1
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);

                Subquery<Long> sq = criteria.subquery(Long.class);
                Root<Item> i = sq.from(Item.class);
                sq.select(cb.count(i))
                        .where(cb.equal(i.get("seller"), u)
                        );

                criteria.select(u).where(cb.gt(sq, 1L));

                TypedQuery<User> query = em.createQuery(criteria);
                List<User> result = query.getResultList();
                assertThat(result, hasSize(1));
                User user = result.iterator().next();
                assertEquals(testData.users.getFirstId(), user.getId());
            }
            em.clear();
            {
                // Uncorrelated
                // select b from Bid b where b.amount + 1 >= (select max(b2.amount) from Bid b2)
                CriteriaQuery<Bid> criteria = cb.createQuery(Bid.class);
                Root<Bid> b = criteria.from(Bid.class);
                Subquery<BigDecimal> sq = criteria.subquery(BigDecimal.class);
                Root<Bid> b2 = sq.from(Bid.class);
                sq.select(cb.max(b2.get("amount")));
                criteria.select(b).where(
                        cb.greaterThanOrEqualTo(
                                cb.sum(b.get("amount"), new BigDecimal(1)),
                                sq
                        )
                );

                TypedQuery<Bid> query = em.createQuery(criteria);
                List<Bid> result = query.getResultList();
                assertThat(result, hasSize(2));
            }
            em.clear();
            {
                // ALL
                // select i from Item i where 10 >= all (select b.amount from i.bids b)
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                Subquery<BigDecimal> sq = criteria.subquery(BigDecimal.class);
                Root<Bid> b = sq.from(Bid.class);
                sq.select(b.get("amount"));
                sq.where(cb.equal(b.get("item"), i));

                criteria.select(i).where(
                        cb.greaterThanOrEqualTo(
                                cb.literal(new BigDecimal(10)),
                                cb.all(sq)
                        )
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> result = query.getResultList();
                assertThat(result, hasSize(2));
            }
            em.clear();
            {
                // ANY
                // select i from Item i where 101.00 = any (select b.amount from i.bids b)
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                Subquery<BigDecimal> sq = criteria.subquery(BigDecimal.class);
                Root<Bid> b = sq.from(Bid.class);
                sq.select(b.get("amount")).where(cb.equal(b.get("item"), i));

                criteria.select(i).where(cb.equal(
                        cb.literal(new BigDecimal(101)),
                        cb.any(sq)
                        )
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> result = query.getResultList();
                assertThat(result, hasSize(1));
            }
            em.clear();
            {
                // EXISTS
                // select i from Item i where exists (select b from Bid b where b.item = i)
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                Subquery<Bid> sq = criteria.subquery(Bid.class);
                Root<Bid> b = sq.from(Bid.class);
                sq.select(b).where(
                        cb.equal(b.get("item"), i)
                );

                criteria.select(i).where(cb.exists(sq));

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> result = query.getResultList();
                assertThat(result, hasSize(2));
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
