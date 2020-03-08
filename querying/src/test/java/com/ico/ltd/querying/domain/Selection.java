package com.ico.ltd.querying.domain;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class Selection extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        CriteriaBuilder cb = em.getCriteriaBuilder();

        try {
            tx.begin();

            { // This is not guaranteed to work in all JPA providers, criteria.select() should be used
                CriteriaQuery criteria = cb.createQuery(Item.class);
                criteria.from(Item.class);

                List<Item> items = em.createQuery(criteria).getResultList();
                assertThat(items, Matchers.hasSize(3));
            }
            em.clear();
            { // Simplest
                CriteriaQuery criteria = cb.createQuery(Item.class);
                Root i = criteria.from(Item.class);
                criteria.select(i);

                List<Item> items = em.createQuery(criteria).getResultList();
                assertThat(items, Matchers.hasSize(3));
            }
            em.clear();
            { // Nested calls
                CriteriaQuery criteria = cb.createQuery(Item.class);
                criteria.select(criteria.from(Item.class));

                List<Item> items = em.createQuery(criteria).getResultList();
                assertThat(items, Matchers.hasSize(3));
            }
            em.clear();
            {
                // Polymorphism restricted types
                // ~ select bd from BillingDetails bd
                CriteriaQuery criteria = cb.createQuery();
                criteria.select(criteria.from(BillingDetails.class));
                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), Matchers.hasSize(2));
            }
            em.clear();
            {
                // ~ select cc from CreditCard cc
                CriteriaQuery criteria = cb.createQuery();
                criteria.select(criteria.from(CreditCard.class));
                TypedQuery<CreditCard> query = em.createQuery(criteria);
                assertThat(query.getResultList(), Matchers.hasSize(1));
            }
            em.clear();
            {
                // ~ select bd from BillingDetails bd where type(bd) = CreditCard
                CriteriaQuery criteria = cb.createQuery();
                Root<BillingDetails> bd = criteria.from(BillingDetails.class);
                criteria.select(bd).where(
                        cb.equal(bd.type(), CreditCard.class)
                );
                TypedQuery<CreditCard> query = em.createQuery(criteria);
                assertThat(query.getResultList(), Matchers.hasSize(1));
            }
            em.clear();
            {
                // ~ select bd from BillingDetails bd where type(bd) in :types
                CriteriaQuery criteria = cb.createQuery();
                Root<BillingDetails> bd = criteria.from(BillingDetails.class);
                criteria.select(bd).where(
                        bd.type().in(cb.parameter(List.class, "types"))
                );
                TypedQuery<BillingDetails> query = em.createQuery(criteria);
                query.setParameter("types", List.of(CreditCard.class, BankAccount.class));

                assertThat(query.getResultList(), Matchers.hasSize(2));
            }
            em.clear();
            {
                // select bd from BillingDetails bd where not type(bd) = BankAccount
                CriteriaQuery criteria = cb.createQuery();
                Root<BillingDetails> bd = criteria.from(BillingDetails.class);
                criteria.select(bd).where(
                        cb.not(cb.equal(bd.type(), BankAccount.class))
                );
                TypedQuery<BillingDetails> query = em.createQuery(criteria);
                assertThat(query.getResultList(), Matchers.hasSize(1));
            }
            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
