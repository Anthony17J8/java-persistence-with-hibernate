package com.ico.ltd.managingdata.domain;

import com.ico.ltd.managingdata.config.PersistenceConfig;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceUnitUtil;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class SimpleTransitionsTest {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    PersistenceUnitUtil puUtil;

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        puUtil = emf.getPersistenceUnitUtil();
    }

    @Test
    void makePersistent() throws Exception {

        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Item item = new Item();
            item.setName("New Name");

            assertNull(puUtil.getIdentifier(item)); // check whether entity is transient

            em.persist(item);

            assertTrue(em.contains(item)); // check whether entity is persistent

            Long itemId = item.getId(); // has been assigned

            tx.commit();
            em.close();

            assertNotNull(puUtil.getIdentifier(item));
            assertEquals(item.getId(), puUtil.getIdentifier(item)); // check whether entity is detached

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();
            Item result = em.find(Item.class, itemId);
            assertEquals("New Name", result.getName());
            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    @Test
    void loadAndUpdateData() throws Exception {
        EntityTransaction tx = em.getTransaction();
        try {
            // persist data
            tx.begin();

            Item saved = new Item();
            saved.setName("Some Name");

            em.persist(saved);
            tx.commit();
            em.close();

            Long itemId = saved.getId();

            // update data
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item result = em.find(Item.class, itemId); // Hit the database if not already in persistence context

            if (result != null) {
                result.setName("New Name"); // update state
            }
            tx.commit(); // Flush: Dirty check and SQL UPDATE
            em.close();

            // check updated state
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item check = em.find(Item.class, itemId);
            assertEquals("New Name", check.getName());

            tx.commit();
            em.close();

            // repeatable read
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item itemA = em.find(Item.class, itemId);
            Item itemB = em.find(Item.class, itemId);
            assertSame(itemA, itemB);
            assertEquals(itemA, itemB);
            assertEquals(itemA.getId(), itemB.getId());

        } finally {
            tx.rollback();
        }
    }

    @Test
    void retrievePersistentReference() {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Item someItem = new Item();
            someItem.setName("Some Item");
            em.persist(someItem);
            tx.commit();
            em.close();

            long itemId = someItem.getId();

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            /*
               If the persistence context already contains an <code>Item</code> with the given identifier, that
               <code>Item</code> instance is returned by <code>getReference()</code> without hitting the database.
               Furthermore, if <em>no</em> persistent instance with that identifier is currently managed, a hollow
               placeholder will be produced by Hibernate, a proxy. This means <code>getReference()</code> will not
               access the database, and it doesn't return <code>null</code>, unlike <code>find()</code>.
             */
            Item item = em.getReference(Item.class, itemId);

            /*
               JPA offers <code>PersistenceUnitUtil</code> helper methods such as <code>isLoaded()</code> to
               detect if you are working with an uninitialized proxy.
            */
            PersistenceUnitUtil persistenceUtil = emf.getPersistenceUnitUtil();
            assertFalse(persistenceUtil.isLoaded(item));

            /*
               As soon as you call any method such as <code>Item#getName()</code> on the proxy, a
               <code>SELECT</code> is executed to fully initialize the placeholder. The exception to this rule is
               a method that is a mapped database identifier getter method, such as <code>getId()</code>. A proxy
               might look like the real thing but it is only a placeholder carrying the identifier value of the
               entity instance it represents. If the database record doesn't exist anymore when the proxy is
               initialized, an <code>EntityNotFoundException</code> will be thrown.
             */
//             assertEquals(item.getName(), "Some Item");
            /*
               Hibernate has a convenient static <code>initialize()</code> method, loading the proxy's data.
             */
            // Hibernate.initialize(item);

            tx.commit();
            em.close();

            /*
               After the persistence context is closed, <code>item</code> is in detached state. If you do
               not initialize the proxy while the persistence context is still open, you get a
               <code>LazyInitializationException</code> if you access the proxy. You can't load
               data on-demand once the persistence context is closed. The solution is simple: Load the
               data before you close the persistence context.
             */
            assertThrows(LazyInitializationException.class, item::getName);

        } finally {
            tx.rollback();
        }
    }

    @Test
    void makeTransient() {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Item someItem = new Item();
            someItem.setName("Some Item");

            em.persist(someItem);
            tx.commit();
            em.close();

            Long itemId = someItem.getId();
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            /*
               If you call <code>find()</code>, Hibernate will execute a <code>SELECT</code> to
               load the <code>Item</code>. If you call <code>getReference()</code>, Hibernate
               will attempt to avoid the <code>SELECT</code> and return a proxy.
             */
            Item item = em.find(Item.class, itemId);
            //Item item = em.getReference(Item.class, ITEM_ID);

            /*
               Calling <code>remove()</code> will queue the entity instance for deletion when
               the unit of work completes, it is now in <em>removed</em> state. If <code>remove()</code>
               is called on a proxy, Hibernate will execute a <code>SELECT</code> to load the data.
               An entity instance has to be fully initialized during life cycle transitions. You may
               have life cycle callback methods or an entity listener enabled
               (see <a href="#EventListenersInterceptors"/>), and the instance must pass through these
               interceptors to complete its full life cycle.
             */
            em.remove(item);

            /*
                An entity in removed state is no longer in persistent state, this can be
                checked with the <code>contains()</code> operation.
             */
            assertFalse(em.contains(item));

            /*
               You can make the removed instance persistent again, cancelling the deletion.
             */
            // em.persist(item);

            // hibernate.use_identifier_rollback was enabled, it now looks like a transient instance
            assertNull(item.getId());

            /*
               When the transaction commits, Hibernate synchronizes the state transitions with the
               database and executes the SQL <code>DELETE</code>. The JVM garbage collector detects that the
               <code>item</code> is no longer referenced by anyone and finally deletes the last trace of
               the data.
             */
            tx.commit();
            em.close();


            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();
            item = em.find(Item.class, itemId);
            assertNull(item);
            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    void refresh() throws Exception {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Item someItem = new Item();
            someItem.setName("Some Name");

            em.persist(someItem);
            tx.commit();
            em.close();

            Long itemId = someItem.getId();

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();
            Item item = em.find(Item.class, itemId);
            item.setName("New Name");

            // someone updates this row in the database
            Executors.newSingleThreadExecutor().submit(() -> {
                EntityManager sEm = emf.createEntityManager();
                EntityTransaction sTx = sEm.getTransaction();
                try {
                    sTx.begin();

                    Session session = sEm.unwrap(Session.class);
                    session.doWork((con) -> {
                        PreparedStatement ps = con.prepareStatement("update ITEM set name = ? where ID = ?");
                        ps.setString(1, "Concurrent Update Name");
                        ps.setLong(2, itemId);

                    /* Alternative: you get an EntityNotFoundException on refresh
                                PreparedStatement ps = con.prepareStatement("delete from ITEM where ID = ?");
                                ps.setLong(1, ITEM_ID);
                    */

                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("ITEM row was not updated");
                        }

                    });
                    sTx.commit();
                    sEm.close();
                } catch (Exception exc) {
                    sTx.rollback();
                    sEm.close();
                    throw new RuntimeException("Concurrent operation failure: " + exc, exc);
                }
                return null;
            }).get();

            String oldName = item.getName();
            em.refresh(item);
            assertNotEquals(oldName, item.getName());
            assertEquals("Concurrent Update Name", item.getName());

            tx.commit(); // Flush: Dirty check and SQL UPDATE
            em.close();

        } finally {
            tx.rollback();
        }
    }

    @Test
    void testPersistenceContextCacheAndSelectiveReadOnly() {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Item someItem = new Item();
            someItem.setName("Some Name");

            em.persist(someItem);

            tx.commit();
            em.close();

            Long itemId = someItem.getId();

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            {
                Item item = em.find(Item.class, itemId);
                assertTrue(em.contains(item));
                em.detach(item);        // manually evict instance from persistence context
                assertFalse(em.contains(item));
            }

            {
                em.unwrap(Session.class).setDefaultReadOnly(true);
                Item item = em.find(Item.class, itemId);
                item.setName("New Name"); // NO UPDATE
            }

            {
                em.clear();
                Item item = em.find(Item.class, itemId);
                assertNotEquals("New Name", item.getName());
            }

            {
                org.hibernate.query.Query<Item> query = em.unwrap(Session.class)
                        .createQuery("select i from Item i", Item.class);

                query.setReadOnly(true).list();

                List<Item> result = query.list();

                for (Item item : result)
                    item.setName("New Name");

                em.flush(); // No UPDATE
            }
            {
                List<Item> items = em.createQuery("select i from Item i", Item.class)
                        .setHint(
                                org.hibernate.annotations.QueryHints.READ_ONLY,
                                true
                        ).getResultList();

                for (Item item : items)
                    item.setName("New Name");
                em.flush(); // No UPDATE
            }
            {
                em.clear();
                Item item = em.find(Item.class, itemId);
                assertNotEquals("New Name", item.getName());
            }

            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    void flushModeType() throws Exception {
        EntityTransaction tx = em.getTransaction();
        Long itemId;
        try {
            tx.begin();

            Item someItem = new Item();
            someItem.setName("Original Name");

            em.persist(someItem);
            tx.commit();
            em.close();

            itemId = someItem.getId();
        } finally {
            tx.rollback();
        }

        try {
            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item item = em.find(Item.class, itemId);
            item.setName("New Name");

            // Disable flushing before queries:
            em.setFlushMode(FlushModeType.COMMIT);

            assertEquals(
                    em.createQuery("select i.name from Item i where i.id = :id")
                            .setParameter("id", itemId).getSingleResult(),
                    "Original Name"
            );

            tx.commit(); // Flush!
            em.close();
        } finally {
            tx.rollback();
        }
    }

    /**
     * Whenever you work with instances in detached state and you test them for equality (usually in
     * hash-based collections), you need to supply your own implementation of the equals() and
     * hashCode() methods for your mapped entity class. This is an important issue: if you don’t
     * work with entity instances in detached state, no action is needed, and the default
     * equals() implementation of java.lang.Object is fine.
     */
    @Test
    void identityOfDetachedInstances() {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Item someItem = new Item();
            someItem.setName("Some Name");

            em.persist(someItem);
            tx.commit();
            em.close();

            Long itemId = someItem.getId();

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item a = em.find(Item.class, itemId);
            Item b = em.find(Item.class, itemId);
            assertSame(a, b);
            assertEquals(a, b);
            assertEquals(a.getId(), b.getId());

            tx.commit();
            em.close(); // persistence context is gone, a' and 'b' are now references to instances in detached state!

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Item c = em.find(Item.class, itemId);
            assertNotSame(a, c); // The 'a' reference is still detached!
            assertNotEquals(a, c);
            assertEquals(a.getId(), c.getId());

            tx.commit();
            em.close();

            Set<Item> items = new HashSet<>();
            items.add(a);
            items.add(b);
            items.add(c);
            assertEquals(2, items.size());  // That seems wrong and arbitrary!
        } finally {
            tx.rollback();
        }
    }
}