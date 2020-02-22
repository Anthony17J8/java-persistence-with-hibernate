package com.ico.ltd.fetching.domain;

import com.ico.ltd.fetching.config.PersistenceConfig;
import com.ico.ltd.fetching.util.FetchTestData;
import com.ico.ltd.fetching.util.TestData;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxyHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class LazyProxyCollections {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    void lazyEntityProxies() throws Exception {
        FetchTestData testData = storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Long ITEM_ID = testData.items.getFirstId();
            Long USER_ID = testData.users.getFirstId();

            {
                Item item = em.getReference(Item.class, ITEM_ID); // No SELECT

                // Calling identifier getter (no field access!) doesn't trigger initialization
                // If @Id was on a field, then calling getId() , just like calling any other method, would initialize the proxy!
                assertEquals(item.getId(), ITEM_ID);

                // Class is runtime generated,named something like Item_$$_javassist_1
                assertNotEquals(item.getClass(), Item.class);

                assertEquals(HibernateProxyHelper.getClassWithoutInitializingProxy(item), Item.class);

                PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();
                assertFalse(persistenceUtil.isLoaded(item));
                assertFalse(persistenceUtil.isLoaded(item, "seller"));

                assertFalse(Hibernate.isInitialized(item));

                // Would trigger initialization of item !
                // assertFalse(Hibernate.isInitialized(item.getSeller()));

                // quick-and-dirty initialization of proxies
                Hibernate.initialize(item);
                // SELECT * FROM ITEM where ID = ?

                // Let's make sure the default EAGER of @ManyToOne has been overriden with LAZY
                assertFalse(Hibernate.isInitialized(item.getSeller()));

                Hibernate.initialize(item.getSeller());
                // select * from USERS where ID = ?
                em.clear();
            }

            {
                /*
                   An <code>Item</code> entity instance is loaded in the persistence context, its
                   <code>seller</code> is not initialized, it's a <code>User</code> proxy.
                 */
                Item item = em.find(Item.class, ITEM_ID);
                // select * from ITEM where ID = ?

                /*
                   You can manually detach the data from the persistence context, or close the
                   persistence context and detach everything.
                 */
                em.detach(item);
                em.detach(item.getSeller());
                // close

                /*
                   The static <code>PersistenceUtil</code> helper works without a persistence
                   context, you can check at any time if the data you want to access has
                   actually been loaded.
                 */
                PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();
                assertTrue(persistenceUtil.isLoaded(item));
                assertFalse(persistenceUtil.isLoaded(item, "seller"));

                /*
                   In detached state, you can call the identifier getter method of the User proxy. But
                   calling any other method on the proxy, such as getUsername() , will throw a Lazy-
                   InitializationException . Data can only be loaded on demand while the persistence
                   context manages the proxy, not in detached state.
                 */
                assertEquals(item.getSeller().getId(), USER_ID);

                // throws LazyInitializationException
                // assertNotNull(item.getSeller().getUsername());
            }
            em.clear();
            {
                // there is no SQL SELECT in this procedure, only one INSERT!
                Item item = em.getReference(Item.class, ITEM_ID);
                User user = em.getReference(User.class, USER_ID);

                Bid newBid = new Bid(new BigDecimal("99.00"));
                newBid.setBidder(user);
                newBid.setItem(item);
                // insert into BID values (?,?,...)

                em.persist(newBid);
                em.flush();
                em.clear();
                assertEquals(0, em.find(Bid.class, newBid.getId()).getAmount().compareTo(new BigDecimal("99")));
            }

        } finally {
            tx.rollback();
        }


    }

    public FetchTestData storeTestData() throws Exception {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        Long[] categoryIds = new Long[3];
        Long[] itemIds = new Long[3];
        Long[] userIds = new Long[3];

        User johndoe = new User("johndoe");
        em.persist(johndoe);
        userIds[0] = johndoe.getId();

        User janeroe = new User("janeroe");
        em.persist(janeroe);
        userIds[1] = janeroe.getId();

        User robertdoe = new User("robertdoe");
        em.persist(robertdoe);
        userIds[2] = robertdoe.getId();

        Category category = new Category("Category One");
        em.persist(category);
        categoryIds[0] = category.getId();

        Item item = new Item("Item One", new Date(System.currentTimeMillis()), johndoe);
        em.persist(item);
        itemIds[0] = item.getId();
        category.getItems().add(item);
        item.getCategories().add(category);
        for (int i = 1; i <= 3; i++) {
            Bid bid = new Bid(item, robertdoe, new BigDecimal(9 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        category = new Category("Category Two");
        em.persist(category);
        categoryIds[1] = category.getId();

        item = new Item("Item Two", new Date(System.currentTimeMillis()), johndoe);
        em.persist(item);
        itemIds[1] = item.getId();
        category.getItems().add(item);
        item.getCategories().add(category);
        for (int i = 1; i <= 1; i++) {
            Bid bid = new Bid(item, janeroe, new BigDecimal(2 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        item = new Item("Item Three", new Date(System.currentTimeMillis()), janeroe);
        em.persist(item);
        itemIds[2] = item.getId();
        category.getItems().add(item);
        item.getCategories().add(category);

        category = new Category("Category Three");
        em.persist(category);
        categoryIds[2] = category.getId();

        tx.commit();
        em.close();

        FetchTestData testData = new FetchTestData();
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}