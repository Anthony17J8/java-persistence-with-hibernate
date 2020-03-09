package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }

    }
}
