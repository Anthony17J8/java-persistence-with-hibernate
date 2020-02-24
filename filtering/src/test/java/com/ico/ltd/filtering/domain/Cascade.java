package com.ico.ltd.filtering.domain;

import com.ico.ltd.filtering.config.PersistenceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;

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
}