package com.ico.ltd.concurrency.domain;

import com.ico.ltd.concurrency.config.PersistenceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
public class NonTransactionalTest {

    @Autowired
    EntityManagerFactory emf;

    @Test
    void autoCommitMode() {
        Long ITEM_ID;
        {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();

                Item someItem = new Item("Original Name");
                em.persist(someItem);
                ITEM_ID = someItem.getId();

                tx.commit();
                em.close();
            } finally {
                tx.rollback();
            }
        }

        {
            /*
               No transaction is active when we create the <code>EntityManager</code>. The
               persistence context is now in a special <em>unsynchronized</em> mode, Hibernate
               will not flush automatically at any time.
             */
            EntityManager em = emf.createEntityManager();

            /*
               You can access the database to read data; this operation will execute a
               <code>SELECT</code> statement, sent to the database in auto-commit mode.
             */
            Item item = em.find(Item.class, ITEM_ID);
            item.setName("New Name");

             /*
               Usually Hibernate would flush the persistence context when you execute a
               <code>Query</code>. However, because the context is <em>unsynchronized</em>,
               flushing will not occur and the query will return the old, original database
               value. Queries with scalar results are not repeatable, you'll see whatever
               values are present in the database and given to Hibernate in the
               <code>ResultSet</code>. Note that this isn't a repeatable read either if
               you are in <em>synchronized</em> mode.
             */
            assertEquals(
                    em.createQuery("select i.name from Item i where i.id = :id")
                            .setParameter("id", ITEM_ID).getSingleResult(),
                    "Original Name"
            );

             /*
               Retrieving a managed entity instance involves a lookup, during JDBC
               result set marshaling, in the current persistence context. The
               already loaded <code>Item</code> instance with the changed name will
               be returned from the persistence context, values from the database
               will be ignored. This is a repeatable read of an entity instance,
               even without a system transaction.
             */
            assertEquals(
                    ((Item) em.createQuery("select i from Item i where i.id = :id")
                            .setParameter("id", ITEM_ID).getSingleResult()).getName(),
                    "New Name"
            );

            /*
               If you try to flush the persistence context manually, to store the new
               <code>Item#name</code>, Hibernate will throw a
               <code>javax.persistence.TransactionRequiredException</code>. You are
               prevented from executing an <code>UPDATE</code> statement in
               <em>unsynchronized</em> mode, as you wouldn't be able to roll back the change.
            */
            // em.flush();

            /*
               You can roll back the change you made with the <code>refresh()</code>
               method, it loads the current <code>Item</code> state from the database
               and overwrites the change you have made in memory.
             */
            em.refresh(item);
            assertEquals(item.getName(), "Original Name");

            em.close();
        }
    }
}
