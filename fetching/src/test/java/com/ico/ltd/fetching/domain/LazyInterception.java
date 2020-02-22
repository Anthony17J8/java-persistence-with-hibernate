package com.ico.ltd.fetching.domain;

import com.ico.ltd.fetching.config.PersistenceConfig;
import com.ico.ltd.fetching.util.FetchTestData;
import com.ico.ltd.fetching.util.TestData;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class LazyInterception {


    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    void noUserProxy() throws Exception {
        FetchTestData testData = storeTestData();
        Long USER_ID = testData.users.getFirstId();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            {
                // Proxies are disabled, getReference() will return an initialized instance
                User user = em.getReference(User.class, USER_ID);
                // select * from USERS where ID = ?

                assertTrue(Hibernate.isInitialized(user));
            }
            em.clear();

            {

                /* Instead, the proprietary LazyToOneOption.NO_PROXY setting tells Hibernate that the
                bytecode enhancer must add interception code for the seller property. Without this
                option, or if you don’t run the bytecode enhancer, this association would be eagerly
                loaded and the field would be populated right away when the Item is loaded, because
                proxies for the User entity have been disabled.
                 */
                // https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#BytecodeEnhancement-capabilities
                Item item = em.find(Item.class, ITEM_ID);
                // select * from ITEM where ID = ?

                assertEquals(item.getSeller().getId(), USER_ID);
                // select * from USERS where ID = ?
                // Even item.getSeller() would trigger the SELECT!
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }

    }

    public FetchTestData storeTestData() throws Exception {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

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

        Item item = new Item("Item One", new Date(System.currentTimeMillis()), johndoe, "Some description.");
        em.persist(item);
        itemIds[0] = item.getId();

        item = new Item("Item Two", new Date(System.currentTimeMillis()), johndoe, "Some description.");
        em.persist(item);
        itemIds[1] = item.getId();

        item = new Item("Item Three", new Date(System.currentTimeMillis()), janeroe, "Some description.");
        em.persist(item);
        itemIds[2] = item.getId();

        tx.commit();
        em.close();

        FetchTestData testData = new FetchTestData();
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}