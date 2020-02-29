package com.ico.ltd.filtering.domain;

import com.ico.ltd.filtering.config.PersistenceConfig;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class Cascade {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    void detachAndMerge() {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        Long ITEM_ID;
        try {
            tx.begin();
            {
                User user = new User("johndoe");
                em.persist(user);

                Item item = new Item("Some Item", user);
                em.persist(item);
                ITEM_ID = item.getId();

                Bid firstBid = new Bid(new BigDecimal("99.00"), item);
                item.getBids().add(firstBid);
                em.persist(firstBid);

                Bid secondBid = new Bid(new BigDecimal("100.00"), item);
                item.getBids().add(secondBid);
                em.persist(secondBid);

                em.flush();
            }
            em.clear();

            Item item = em.find(Item.class, ITEM_ID);
            assertEquals(item.getBids().size(), 2); // Initializes bids
            em.detach(item);

            em.clear();

            item.setName("New Name");

            Bid bid = new Bid(new BigDecimal("101.00"), item);
            item.getBids().add(bid);

            /*
               Hibernate merges the detached <code>item</code>: First, it checks if the
               persistence context already contains an <code>Item</code> with the given
               identifier value. In this case, there isn't any, so the <code>Item</code>
               is loaded from the database. Hibernate is smart enough to know that
               it will also need the <code>bids</code> during merging, so it fetches them
               right away in the same SQL query. Hibernate then copies the detached <code>item</code>
               values onto the loaded instance, which it returns to you in persistent state.
               The same procedure is applied to every <code>Bid</code>, and Hibernate
               will detect that one of the <code>bids</code> is new.

               Hibernate always loads entity associations eagerly with a JOIN when merging,
               if CascadeType.MERGE is enabled for the association.
             */
            Item mergedItem = em.merge(item);
            // select i.*, b.*
            //  from ITEM i
            //    left outer join BID b on i.ID = b.ITEM_ID
            //  where i.ID = ?

            /*
               Hibernate made the new <code>Bid</code> persistent during merging, it
               now has an identifier value assigned.
             */
            for (Bid b : mergedItem.getBids()) {
                assertNotNull(b.getId());
            }

            /*
               When you flush the persistence context, Hibernate detects that the
               <code>name</code> of the <code>Item</code> changed during merging.
               The new <code>Bid</code> will also be stored.
             */
            em.flush();
            // update ITEM set NAME = ? where ID = ?
            // insert into BID values (?, ?, ?, ...)

            em.clear();

            item = em.find(Item.class, ITEM_ID);
            assertEquals(item.getName(), "New Name");
            assertEquals(item.getBids().size(), 3);

            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    @Test
    void cascadeRefresh() throws Exception {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            Long USER_ID;
            Long CREDIT_CARD_ID = null;

            {

                User user = new User("johndoe");
                user.getBillingDetails().add(
                        new CreditCard("John Doe", "1234567890", "11", "2020")
                );
                user.getBillingDetails().add(
                        new BankAccount("John Doe", "45678", "Some Bank", "1234")
                );
                em.persist(user);
                em.flush();

                USER_ID = user.getId();
                for (BillingDetails bd : user.getBillingDetails()) {
                    if (bd instanceof CreditCard)
                        CREDIT_CARD_ID = bd.getId();
                }
                assertNotNull(CREDIT_CARD_ID);
            }

            tx.commit();
            em.close();
            // Locks from INSERTs must be released, commit and start a new unit of work

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

             /*
               An instance of <code>User</code> is loaded from the database.
             */
            User user = em.find(User.class, USER_ID);

            /*
               Its lazy <code>billingDetails</code> collection is initialized when
               you iterate through the elements or when you call <code>size()</code>.
             */
            assertEquals(user.getBillingDetails().size(), 2);
            for (BillingDetails bd : user.getBillingDetails()) {
                assertEquals(bd.getOwner(), "John Doe");
            }

            // Someone modifies the billing information in the database!
            final Long SOME_USER_ID = USER_ID;
            final Long SOME_CREDIT_CARD_ID = CREDIT_CARD_ID;

            // In a separate transaction, so no locks are held in the database on the
            // updated/deleted rows and we can SELECT them again in the original transaction
            Executors.newSingleThreadExecutor().submit(() -> {
                EntityManager em1 = emf.createEntityManager();
                EntityTransaction tx1 = em1.getTransaction();

                try {
                    tx1.begin();

                    em1.unwrap(Session.class).doWork((con) -> {
                        PreparedStatement ps;

                                /* Delete the credit card, this will cause the refresh to
                                   fail with EntityNotFoundException!
                                ps = con.prepareStatement(
                                    "delete from CREDITCARD where ID = ?"
                                );
                                ps.setLong(1, SOME_CREDIT_CARD_ID);
                                ps.executeUpdate();
                                ps = con.prepareStatement(
                                    "delete from BILLINGDETAILS where ID = ?"
                                );
                                ps.setLong(1, SOME_CREDIT_CARD_ID);
                                ps.executeUpdate();
                                */

                        // Update the bank account
                        ps = con.prepareStatement(
                                "update BILLINGDETAILS set OWNER = ? where USER_ID = ?"
                        );
                        ps.setString(1, "Doe John");
                        ps.setLong(2, SOME_USER_ID);
                        ps.executeUpdate();
                    });

                    tx1.commit();
                    em1.close();
                } catch (Exception exc) {
                    // should not fail
                    tx1.rollback();
                }
            }).get();

            /*
               When you <code>refresh()</code> the managed <code>User</code> instance,
               Hibernate cascades the operation to the managed <code>BillingDetails</code>
               and refreshes each with a SQL <code>SELECT</code>. If one of these instances
               is no longer in the database, Hibernate throws an <code>EntityNotFoundException</code>.
               Then, Hibernate refreshes the <code>User</code> instance and eagerly
               loads the whole <code>billingDetails</code> collection to discover any
               new <code>BillingDetails</code>.
             */
            em.refresh(user);
            // select * from CREDITCARD join BILLINGDETAILS where ID = ?
            // select * from BANKACCOUNT join BILLINGDETAILS where ID = ?
            // select * from USERS
            //  left outer join BILLINGDETAILS
            //  left outer join CREDITCARD
            //  left outer JOIN BANKACCOUNT
            // where ID = ?

            for (BillingDetails bd : user.getBillingDetails()) {
                assertEquals(bd.getOwner(), "Doe John");
            }

            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }

    @Test
    void cascadeReplicate() {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Long ITEM_ID;

        try {
            {
                tx.begin();

                User user = new User("johndoe");
                em.persist(user);

                Item item = new Item("Some Item", user);
                em.persist(item);
                ITEM_ID = item.getId();

                tx.commit();
                em.close();
            }

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();
            Item item = em.find(Item.class, ITEM_ID);

            // Initialize the lazy Item#seller
            assertNotNull(item.getSeller().getUsername());

            tx.commit();
            em.close();

            EntityManager otherDatabase = emf.createEntityManager();
            tx = otherDatabase.getTransaction();
            tx.begin();

            otherDatabase.unwrap(Session.class).replicate(item, ReplicationMode.OVERWRITE);
            // select ID from ITEM where ID = ?
            // select ID from USERS where ID = ?

            tx.commit();
            // update ITEM set NAME = ?, SELLER_ID = ?, ... where ID = ?
            // update USERS set USERNAME = ?, ... where ID = ?
            otherDatabase.close();

        } finally {
            tx.rollback();
        }

    }
}