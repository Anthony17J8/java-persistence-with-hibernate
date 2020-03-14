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
                
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }

    }
}
