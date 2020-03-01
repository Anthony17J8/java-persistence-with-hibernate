package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateExecuteQueries extends QueryingTest {

    @Test
    public void createQueries() throws Exception {
        storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            {
                Query query = em.createQuery("select i from Item i");

                assertEquals(3, query.getResultList().size());
            }

            {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                // Also available on EntityManagerFactory:
                // CriteriaBuilder cb = entityManagerFactory.getCriteriaBuilder();

                CriteriaQuery criteria = cb.createQuery();
                criteria.select(criteria.from(Item.class));

                Query query = em.createQuery(criteria);

                assertEquals(3, query.getResultList().size());
            }
            {
                // Note: This old JPA 1.0 method does not return a TypedQuery!
                Query query = em.createNativeQuery(
                        "select * from ITEM", Item.class
                );

                assertEquals(query.getResultList().size(), 3);
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
