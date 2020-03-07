package com.ico.ltd.querying.domain;

import org.hamcrest.Matchers;
import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
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
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateExecuteQueries extends QueryingTest {

    @Test
    @DirtiesContext
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
    @DirtiesContext
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
    @DirtiesContext
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
    @DirtiesContext
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

    @Test
    @DirtiesContext
    void pagination() throws Exception {
        storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            List<Item> items;
            { // Limiting result rows
                Query query = em.createQuery("select i from Item i");
                query.setFirstResult(40).setMaxResults(10);

                items = query.getResultList();
                assertEquals(items.size(), 0);
            }
            em.clear();
            { // Rewrite SQL
                Query query = em.createNativeQuery("select * from ITEM");
                query.setFirstResult(40).setMaxResults(10);

                items = query.getResultList();
                assertEquals(items.size(), 0);
            }
            em.clear();
            { // Getting total count with a cursor
                Query query = em.createQuery("select i from Item i");

                /*
                   Unwrap the Hibernate API to use scrollable cursors.
                 */
                org.hibernate.query.Query hibernateQuery = query.unwrap(org.hibernate.query.Query.class);

                /*
                   Execute the query with a database cursor; this does not retrieve the
                   result set into memory.
                 */
                ScrollableResults cursor = hibernateQuery.scroll(ScrollMode.SCROLL_INSENSITIVE);

                /*
                   Jump to the last row of the result in the database, then get the row number.
                   Since row numbers are zero-based, add one to get the total count of rows.
                 */
                cursor.last();
                int count = cursor.getRowNumber() + 1;

                /*
                   You must close the database cursor.
                 */
                cursor.close();
                /*
                   Now execute the query again and retrieve an arbitrary page of data.
                 */
                query.setFirstResult(40).setMaxResults(10);

                assertEquals(count, 3);
                items = query.getResultList();
                assertEquals(items.size(), 0);
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void executeQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();
        Long ITEM_ID = testData.items.getFirstId();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            { // Get a list
                Query query = em.createQuery("select i from Item i");
                List<Item> items = query.getResultList();

                assertEquals(items.size(), 3);
            }
            { // Get a list of scalar values
                Query query = em.createQuery("select i.name from Item i");
                List<String> itemNames = query.getResultList();

                assertEquals(itemNames.size(), 3);
            }
            { // Single result
                TypedQuery<Item> query = em.createQuery(
                        "select i from Item i where i.id = :id", Item.class
                ).setParameter("id", ITEM_ID);

                Item item = query.getSingleResult();

                assertEquals(item.getId(), ITEM_ID);
            }
            { // Single scalar result
                TypedQuery<String> query = em.createQuery(
                        "select i.name from Item i where i.id = :id", String.class
                ).setParameter("id", ITEM_ID);

                String itemName = query.getSingleResult();

                assertEquals(em.find(Item.class, ITEM_ID).getName(), itemName);
            }
            { // No (single) result
                boolean gotException = false;
                try {
                    TypedQuery<Item> query = em.createQuery(
                            "select i from Item i where i.id = :id", Item.class
                    ).setParameter("id", 1234L);

                    Item item = query.getSingleResult();

                } catch (NoResultException ex) {
                    gotException = true;
                }
                assertTrue(gotException);
            }
            { // Not a unique result
                boolean gotException = false;
                try {
                    Query query = em.createQuery(
                            "select i from Item i where i.name like '%a%'"
                    );

                    Item item = (Item) query.getSingleResult();

                } catch (NonUniqueResultException ex) {
                    gotException = true;
                }
                assertTrue(gotException);
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void scrollThroughResultSet() throws Exception {
        storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            // Scrolling with a database cursor
            Session session = em.unwrap(Session.class);

            org.hibernate.query.Query query = session.createQuery(
                    "select i from Item i order by i.id asc"
            );

            org.hibernate.ScrollableResults cursor =
                    query.scroll(org.hibernate.ScrollMode.SCROLL_INSENSITIVE);

            // Jump to third result row
            cursor.setRowNumber(2);

            // Get first "column"
            Item item = (Item) cursor.get(0);

            cursor.close(); // Required!

            assertEquals("Baz", item.getName());

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void iteration() throws Exception {
        storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            // Iterating through a result
            Session session = em.unwrap(Session.class);

            org.hibernate.query.Query query = session.createQuery(
                    "select i from Item i"
            );

            int count = 0;
            Iterator<Item> it = query.iterate(); // select ID from ITEM
            while (it.hasNext()) {
                Item next = it.next(); // select * from ITEM where ID = ?
                // ...
                count++;
            }

            // Iterator must be closed, either when the Session
            // is closed or manually:
            Hibernate.close(it);

            assertEquals(count, 3);
            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void namedQueriesWithXml() throws Exception {
        TestDataCategoriesItems testData = storeTestData();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            {
                // JPQL
                Query query = em.createNamedQuery("findItems");
                List<Item> items = query.getResultList();

                assertThat(items, Matchers.hasSize(3));
            }
            em.clear();
            {
                // Native SQL
                Query query = em.createNamedQuery("findItemsSQL");

                List<Item> items = query.getResultList();

                assertEquals(items.size(), 3);
                assertEquals(items.get(0).getId(), ITEM_ID);
            }
            em.clear();
            {
                Query query = em.createNamedQuery("findItemsSQLHibernate");
                List<Item> items = query.getResultList();

                assertThat(items, Matchers.hasSize(3));
                assertEquals("Bar", items.get(0).getName());
                assertEquals("Baz", items.get(1).getName());
                assertEquals("Foo", items.get(2).getName());
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }
}
