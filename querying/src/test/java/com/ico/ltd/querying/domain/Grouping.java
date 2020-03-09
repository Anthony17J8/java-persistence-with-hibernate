package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Grouping extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        try {
            tx.begin();

            {
                // select u.lastname, count(u) from User u group by u.lastname
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<User> u = criteria.from(User.class);
                criteria.multiselect(
                        u.get("lastname"),
                        cb.count(u)
                );
                criteria.groupBy(u.get("lastname"));

                Query q = em.createQuery(criteria);
                List<Object[]> result = q.getResultList();
                assertThat(result, hasSize(2));
                for (Object[] row : result) {
                    assertTrue(row[0] instanceof String);
                    assertTrue(row[1] instanceof Long);
                }
            }
            em.clear();
            {
                // select i.name, avg(b.amount) from Bid b join b.item i group by i.name
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<Bid> b = criteria.from(Bid.class);
                criteria.multiselect(
                        b.get("item").get("name"),
                        cb.avg(b.get("amount"))
                );
                criteria.groupBy(b.get("item").get("name"));
                Query q = em.createQuery(criteria);
                List<Object[]> result = q.getResultList();
                assertThat(result, hasSize(2));
                for (Object[] row : result) {
                    assertTrue(row[0] instanceof String);
                    assertTrue(row[1] instanceof Double);
                }
            }
            em.clear();
            {
                // select i, avg(b.amount) from Bid b join b.item i group by i

                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<Bid> b = criteria.from(Bid.class);
                Join<Object, Object> item = b.join("item");
                criteria.multiselect(
                        item,
                        cb.avg(b.get("amount"))
                );
                criteria.groupBy(item); // includes all properties to clause

                Query q = em.createQuery(criteria);
                List<Object[]> result = q.getResultList();
                assertThat(result, hasSize(2));
                for (Object[] row : result) {
                    assertTrue(row[0] instanceof Item);
                    assertTrue(row[1] instanceof Double);
                }
            }
            em.clear();
            {
                // select u.lastname, count(u) from User u
                // group by u.lastname
                // having u.lastname like 'D%'
                CriteriaQuery<Object> criteria = cb.createQuery();
                Root<User> u = criteria.from(User.class);
                criteria.multiselect(
                        u.get("lastname"),
                        cb.count(u)
                );

                criteria.groupBy(u.get("lastname"));
                criteria.having(cb.like(u.get("lastname"), "D%"));

                Query q = em.createQuery(criteria);
                List<Object[]> result = q.getResultList();
                assertThat(result, hasSize(1));
                for (Object[] row : result) {
                    assertTrue(row[0] instanceof String);
                    assertTrue(row[1] instanceof Long);
                }
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
