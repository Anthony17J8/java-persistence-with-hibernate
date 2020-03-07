package com.ico.ltd.querying.domain;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void createTypedQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            {
                Query query = em.createQuery(
                        "select i from Item i where i.id = :id"
                ).setParameter("id", ITEM_ID);

                Item result = (Item) query.getSingleResult();

                assertEquals(result.getId(), ITEM_ID);
            }

            {
                TypedQuery<Item> query = em.createQuery("SELECT i FROM Item i WHERE i.id = :id", Item.class)
                        .setParameter("id", ITEM_ID);

                Item result = query.getSingleResult(); // No cast needed!

                assertEquals(ITEM_ID, result.getId());
            }

            {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> root = criteria.from(Item.class);
                criteria.select(root).where(cb.equal(root.get("id"), ITEM_ID));

                TypedQuery<Item> query = em.createQuery(criteria);
                Item item = query.getSingleResult();
                assertEquals(ITEM_ID, item.getId()); // No cast needed!
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    void namedParameterBinding() throws Exception {
        TestDataCategoriesItems testData = storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<Item> items;
            {
                // SQL injection issue!
                // NEVER DO THIS!
                String searchString = getValueEnteredByUser();

                items = em.createQuery(
                        "select i from Item i where i.name = '" + searchString + "'", Item.class
                ).getResultList();
                assertEquals(items.size(), 0);
            }
            em.clear();
            {
                // Named parameter
                String searchString = "Foo";

                Query query = em.createQuery(
                        "SELECT i FROM Item i WHERE i.name = :itemName", Item.class
                ).setParameter("itemName", searchString);

                for (Parameter<?> parameter : query.getParameters()) {
                    assertTrue(query.isBound(parameter));
                }
                items = query.getResultList();
                assertThat(items, Matchers.hasSize(1));
            }
            em.clear();
            {
                // Temporal parameter
                Date tomorrowDate = Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));
                Query query = em.createQuery(
                        "SELECT i FROM Item i WHERE i.auctionEnd > :endDate", Item.class
                ).setParameter("endDate", tomorrowDate, TemporalType.TIMESTAMP);

                items = query.getResultList();
                assertEquals(items.size(), 1);
            }
            em.clear();
            {
                // Entity parameter
                Item someItem = em.find(Item.class, testData.items.getFirstId());

                // Hibernate binds the identifier value of the given Item
                Query query = em.createQuery(
                        "select b from Bid b where b.item = :item"
                ).setParameter("item", someItem);

                items = query.getResultList();
                assertEquals(items.size(), 3);
            }
            em.clear();
            {
                // Criteria Query API
                String searchString = "Foo";
                CriteriaBuilder cb = em.getCriteriaBuilder();

                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);

                Query query = em.createQuery(
                        criteria.select(i).where(
                                cb.equal(
                                        i.get("name"),
                                        cb.parameter(String.class, "itemName")
                                )
                        )
                ).setParameter("itemName", searchString);

                items = query.getResultList();
                assertThat(items, Matchers.hasSize(1));
            }
            em.clear();
            {
                // Criteria Query API with ParameterExpression
                String searchString = "Foo";
                CriteriaBuilder cb = em.getCriteriaBuilder();

                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);

                ParameterExpression<String> itemNameParameter = cb.parameter(String.class);
                Query query = em.createQuery(
                        criteria.select(i).where(
                                cb.equal(
                                        i.get("name"),
                                        itemNameParameter))
                ).setParameter(itemNameParameter, searchString); // type-safe

                items = query.getResultList();
                assertThat(items, Matchers.hasSize(1));
            }
            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    @Test
    void positionalParameterBinding() throws Exception {
        storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            String searchString = "B%";
            Date tomorrowDate = Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));

            Query query = em.createQuery(
                    "SELECT i FROM Item i WHERE i.name LIKE ?1 AND i.auctionEnd > ?2"
            );
            query.setParameter(1, searchString);
            query.setParameter(2, tomorrowDate, TemporalType.TIMESTAMP);

            List<Item> items = query.getResultList();
            assertThat(items, Matchers.hasSize(1));

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    protected String getValueEnteredByUser() {
        // What if this would be "foo ' and callSomeStoredProcedure() and 'bar' = 'bar"?
        return "ALWAYS FILTER VALUES ENTERED BY USERS!";
    }
}
