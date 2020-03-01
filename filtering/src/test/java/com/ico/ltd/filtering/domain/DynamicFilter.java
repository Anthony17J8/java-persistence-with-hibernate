package com.ico.ltd.filtering.domain;

import com.ico.ltd.filtering.config.PersistenceConfig;
import com.ico.ltd.filtering.util.TestData;
import org.hamcrest.Matchers;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class DynamicFilter {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    /**
     * Hibernate doesn’t apply filters to retrieval by identifier operations.
     */
    @Test
    @DirtiesContext
    void filterItems() throws Exception {
        storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            org.hibernate.Filter filter = em.unwrap(Session.class).enableFilter("limitByUserRank");
            filter.setParameter("currentUserRank", 0);

            {
                List<Item> items = em.createQuery(
                        "SELECT i FROM Item i", Item.class
                ).getResultList();
                // select * from ITEM where 0 >=
                // (select u.RANK from USERS u where u.ID = SELLER_ID)

                assertThat(items, Matchers.hasSize(1));
            }
            em.clear();
            {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                criteria.select(criteria.from(Item.class));
                List<Item> items = em.createQuery(criteria).getResultList();
                // select * from ITEM where 0 >=
                // (select u.RANK from USERS u where u.ID = SELLER_ID)
                assertThat(items, Matchers.hasSize(1));
            }
            em.clear();

            filter.setParameter("currentUserRank", 100);
            List<Item> items =
                    em.createQuery("select i from Item i", Item.class)
                            .getResultList();
            assertThat(items, Matchers.hasSize(3));

        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    void filterCollection() throws Exception {
        DynamicFilterTestData testData = storeTestData();
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            Long CATEGORY_ID = testData.categories.getFirstId();
            tx.begin();

            org.hibernate.Filter filter = em.unwrap(Session.class)
                    .enableFilter("limitByUserRankDefault");

            filter.setParameter("currentUserRank", 0);
            Category category = em.find(Category.class, CATEGORY_ID);
            assertEquals(category.getItems().size(), 1);

            em.clear();

            filter.setParameter("currentUserRank", 100);
            category = em.find(Category.class, CATEGORY_ID);
            assertEquals(category.getItems().size(), 2);

            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    private DynamicFilterTestData storeTestData() throws Exception {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        DynamicFilterTestData testData = new DynamicFilterTestData();

        testData.users = new TestData(new Long[2]);
        User johndoe = new User("johndoe");
        em.persist(johndoe);
        testData.users.identifiers[0] = johndoe.getId();
        User janeroe = new User("janeroe", 100);
        em.persist(janeroe);
        testData.users.identifiers[1] = janeroe.getId();

        testData.categories = new TestData(new Long[2]);
        Category categoryOne = new Category("One");
        em.persist(categoryOne);
        testData.categories.identifiers[0] = categoryOne.getId();
        Category categoryTwo = new Category("Two");
        em.persist(categoryTwo);
        testData.categories.identifiers[1] = categoryTwo.getId();

        testData.items = new TestData(new Long[3]);
        Item itemFoo = new Item("Foo", categoryOne, johndoe);
        em.persist(itemFoo);
        testData.items.identifiers[0] = itemFoo.getId();
        Item itemBar = new Item("Bar", categoryOne, janeroe);
        em.persist(itemBar);
        testData.items.identifiers[1] = itemBar.getId();
        Item itemBaz = new Item("Baz", categoryTwo, janeroe);
        em.persist(itemBaz);
        testData.items.identifiers[2] = itemBaz.getId();

        tx.commit();
        em.close();
        return testData;
    }

    private class DynamicFilterTestData {
        TestData categories;
        TestData items;
        TestData users;
    }
}